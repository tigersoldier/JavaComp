package org.javacomp.typesolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.VariableEntity;

/** Logic for solving the result type of an expression. */
public class ExpressionSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final Set<Entity.Kind> ALL_ENTITY_KINDS = EnumSet.allOf(Entity.Kind.class);
  private static final Set<Entity.Kind> ALLOWED_KINDS_METHOD = ImmutableSet.of(Entity.Kind.METHOD);
  private static final Set<Entity.Kind> ALLOWED_KINDS_NON_METHOD =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(ClassEntity.ALLOWED_KINDS)
          .add(Entity.Kind.VARIABLE)
          .add(Entity.Kind.FIELD)
          .add(Entity.Kind.QUALIFIER)
          .build();
  private static final String IDENT_THIS = "this";
  private static final String IDENT_SUPER = "super";
  private static final String IDENT_LENGTH = "length";

  private final TypeSolver typeSolver;
  private final OverloadSolver overloadSolver;
  private final MemberSolver memberSolver;

  public ExpressionSolver(
      TypeSolver typeSolver, OverloadSolver overloadSolver, MemberSolver memberSolver) {
    this.typeSolver = typeSolver;
    this.overloadSolver = overloadSolver;
    this.memberSolver = memberSolver;
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  public Optional<SolvedType> solve(
      ExpressionTree expression, GlobalScope globalScope, EntityScope baseScope, int position) {
    List<Entity> definitions =
        solveDefinitions(expression, globalScope, baseScope, position, ALL_ENTITY_KINDS);
    return Optional.ofNullable(solveEntityType(definitions, globalScope));
  }

  /**
   * Solve all entities that defines the given expression.
   *
   * <p>For methods, all overloads are returned. The best matched method is the first element.
   *
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  public List<Entity> solveDefinitions(
      ExpressionTree expression,
      GlobalScope globalScope,
      EntityScope baseScope,
      int position,
      Set<Entity.Kind> allowedKinds) {
    List<Entity> entities =
        new ExpressionDefinitionScanner(globalScope, baseScope, position, allowedKinds)
            .scan(expression, null /* unused */);
    if (entities == null) {
      logger.warning(
          new Throwable(),
          "Unsupported expression: (%s) %s",
          expression.getClass().getSimpleName(),
          expression);
      return ImmutableList.of();
    }

    logger.fine("Found definitions for %s: %s", expression, entities);
    return entities
        .stream()
        .filter(entity -> allowedKinds.contains(entity.getKind()))
        .collect(ImmutableList.toImmutableList());
  }

  @Nullable
  private SolvedType solveEntityType(List<Entity> foundEntities, GlobalScope globalScope) {
    if (foundEntities.isEmpty()) {
      return null;
    }

    Entity entity = foundEntities.get(0);
    if (entity instanceof MethodEntity) {
      MethodEntity methodEntity = (MethodEntity) entity;
      return typeSolver.solve(methodEntity.getReturnType(), globalScope, methodEntity).orElse(null);
    }
    if (entity instanceof VariableEntity) {
      VariableEntity variableEntity = (VariableEntity) entity;
      return typeSolver
          .solve(variableEntity.getType(), globalScope, variableEntity.getParentScope())
          .orElse(null);
    }
    if (entity instanceof ClassEntity) {
      return SolvedType.builder().setEntity(entity).setPrimitive(false).setArray(false).build();
    }
    if (entity instanceof PackageEntity) {
      return SolvedType.builder().setEntity(entity).setPrimitive(false).setArray(false).build();
    }
    if (entity instanceof PrimitiveEntity) {
      return SolvedType.builder().setEntity(entity).setPrimitive(true).setArray(false).build();
    }
    return null;
  }

  private class ExpressionDefinitionScanner extends TreeScanner<List<Entity>, Void> {
    private final EntityScope baseScope;
    private final GlobalScope globalScope;
    private final ImmutableSet<Entity.Kind> allowedEntityKinds;
    private final int position;

    private List<Optional<SolvedType>> methodArgs;

    private ExpressionDefinitionScanner(
        GlobalScope globalScope,
        EntityScope baseScope,
        int position,
        Set<Entity.Kind> allowedEntityKinds) {
      this.globalScope = globalScope;
      this.baseScope = baseScope;
      this.allowedEntityKinds = ImmutableSet.copyOf(allowedEntityKinds);
      this.methodArgs = null;
      this.position = position;
    }

    @Override
    public List<Entity> visitMethodInvocation(MethodInvocationTree node, Void unused) {
      methodArgs = new ArrayList<>(node.getArguments().size());
      for (ExpressionTree arg : node.getArguments()) {
        methodArgs.add(solve(arg, globalScope, baseScope, ((JCTree) arg).getStartPosition()));
      }
      List<Entity> methods = scan(node.getMethodSelect(), null);
      methodArgs = null;
      return methods;
    }

    @Override
    public List<Entity> visitNewClass(NewClassTree node, Void unused) {
      if (node.getEnclosingExpression() != null) {
        // <EnclosingExpression>.new <identifier>(...).
        SolvedType enclosingClassType =
            solveEntityType(scan(node.getEnclosingExpression(), null), globalScope);
        if (enclosingClassType == null
            || !(enclosingClassType.getEntity() instanceof ClassEntity)) {
          return ImmutableList.of();
        }
        return new ExpressionDefinitionScanner(
                globalScope,
                enclosingClassType.getEntity().getChildScope(),
                -1 /* position is useless for solving classes. */,
                allowedEntityKinds)
            .scan(node.getIdentifier(), null);
      } else {
        return scan(node.getIdentifier(), null);
      }
    }

    @Override
    public List<Entity> visitMemberSelect(MemberSelectTree node, Void unused) {
      List<Optional<SolvedType>> savedMethodArgs = methodArgs;
      methodArgs = null;
      SolvedType expressionType = solveEntityType(scan(node.getExpression(), null), globalScope);
      methodArgs = savedMethodArgs;
      if (expressionType == null) {
        return ImmutableList.of();
      }

      String identifier = node.getIdentifier().toString();

      if (isMethodInvocation()) {
        return ImmutableList.copyOf(
            memberSolver.findMethodMembers(identifier, methodArgs, expressionType, globalScope));
      } else {
        return toList(memberSolver.findNonMethodMember(identifier, expressionType, globalScope));
      }
    }

    @Override
    public List<Entity> visitArrayAccess(ArrayAccessTree node, Void unused) {
      SolvedType expType = solveEntityType(scan(node.getExpression(), null), globalScope);
      if (expType == null || !expType.isArray()) {
        return ImmutableList.of();
      }

      return ImmutableList.of(expType.getEntity());
    }

    @Override
    public List<Entity> visitIdentifier(IdentifierTree node, Void unused) {
      String identifier = node.getName().toString();

      if (IDENT_THIS.equals(identifier)) {
        return toList(findEnclosingClass(baseScope));
      }

      if (IDENT_SUPER.equals(identifier)) {
        ClassEntity enclosingClass = findEnclosingClass(baseScope);
        if (enclosingClass != null && enclosingClass.getSuperClass().isPresent()) {
          return toList(
              typeSolver
                  .solve(
                      enclosingClass.getSuperClass().get(),
                      globalScope,
                      enclosingClass.getParentScope().get())
                  .map(solvedType -> solvedType.getEntity()));
        }
      }

      List<Entity> entities =
          typeSolver.findEntitiesInScope(
              node.getName().toString(), globalScope, baseScope, position, getAllowedEntityKinds());

      if (!entities.isEmpty()) {
        if (isMethodInvocation()) {
          return overloadSolver.prioritizeMatchedMethod(entities, methodArgs, globalScope);
        }
        return entities;
      }

      if (isMethodInvocation()) {
        // Method cannot be direct memeber of root package.
        return ImmutableList.of();
      }

      return toList(
          typeSolver.findDirectMember(
              node.getName().toString(), globalScope, ALLOWED_KINDS_NON_METHOD));
    }

    private Set<Entity.Kind> getAllowedEntityKinds() {
      return methodArgs == null ? allowedEntityKinds : ALLOWED_KINDS_METHOD;
    }

    @Nullable
    private ClassEntity findEnclosingClass(EntityScope baseScope) {
      for (; baseScope != null; baseScope = baseScope.getParentScope().orElse(null)) {
        if (baseScope instanceof ClassEntity) {
          return (ClassEntity) baseScope;
        }
      }
      return null;
    }

    private boolean isMethodInvocation() {
      return methodArgs != null;
    }

    private List<Entity> toList(Optional<Entity> optionalEntity) {
      return toList(optionalEntity.orElse(null));
    }

    private List<Entity> toList(@Nullable Entity entity) {
      if (entity == null) {
        return ImmutableList.of();
      }
      return ImmutableList.of(entity);
    }
  }
}
