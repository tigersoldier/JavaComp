package org.javacomp.typesolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.NullEntity;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedReferenceType;
import org.javacomp.model.SolvedType;
import org.javacomp.model.SolvedTypeParameters;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.parser.TypeReferenceScanner;

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
      ExpressionTree expression, Module module, EntityScope baseScope, int position) {
    List<EntityWithContext> definitions =
        solveDefinitions(expression, module, baseScope, position, ALL_ENTITY_KINDS);
    return Optional.ofNullable(solveEntityType(definitions, module))
        .map(entityWityContext -> entityWityContext.toSolvedType());
  }

  /**
   * Solve all entities that defines the given expression.
   *
   * <p>For methods, all overloads are returned. The best matched method is the first element.
   *
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  public List<EntityWithContext> solveDefinitions(
      ExpressionTree expression,
      Module module,
      EntityScope baseScope,
      int position,
      Set<Entity.Kind> allowedKinds) {
    List<EntityWithContext> entities =
        new ExpressionDefinitionScanner(module, baseScope, position, allowedKinds)
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
        .filter(entityWithContext -> allowedKinds.contains(entityWithContext.getEntity().getKind()))
        .collect(ImmutableList.toImmutableList());
  }

  @Nullable
  private EntityWithContext solveEntityType(List<EntityWithContext> foundEntities, Module module) {
    if (foundEntities.isEmpty()) {
      return null;
    }

    EntityWithContext entityWithContext = foundEntities.get(0);
    SolvedTypeParameters solvedTypeParameters = entityWithContext.getSolvedTypeParameters();
    Entity entity = entityWithContext.getEntity();
    if (entity instanceof MethodEntity) {
      MethodEntity methodEntity = (MethodEntity) entity;
      return typeSolver
          .solve(methodEntity.getReturnType(), solvedTypeParameters, methodEntity, module)
          .map(solvedType -> EntityWithContext.from(solvedType).setInstanceContext(true).build())
          .orElse(null);
    }
    if (entity instanceof VariableEntity) {
      VariableEntity variableEntity = (VariableEntity) entity;
      return typeSolver
          .solve(
              variableEntity.getType(),
              solvedTypeParameters,
              variableEntity.getParentScope(),
              module)
          .map(solvedType -> EntityWithContext.from(solvedType).setInstanceContext(true).build())
          .orElse(null);
    }
    return entityWithContext;
  }

  private class ExpressionDefinitionScanner extends TreeScanner<List<EntityWithContext>, Void> {
    private final EntityScope baseScope;
    private final Module module;
    private final ImmutableSet<Entity.Kind> allowedEntityKinds;
    private final int position;
    private SolvedTypeParameters contextTypeParameters;

    private List<Optional<SolvedType>> methodArgs;

    private ExpressionDefinitionScanner(
        Module module, EntityScope baseScope, int position, Set<Entity.Kind> allowedEntityKinds) {
      this.module = module;
      this.baseScope = baseScope;
      this.allowedEntityKinds = ImmutableSet.copyOf(allowedEntityKinds);
      this.methodArgs = null;
      this.position = position;
      this.contextTypeParameters = typeSolver.solveTypeParametersInScope(baseScope, module);
    }

    @Override
    public List<EntityWithContext> visitMethodInvocation(MethodInvocationTree node, Void unused) {
      methodArgs = new ArrayList<>(node.getArguments().size());
      for (ExpressionTree arg : node.getArguments()) {
        methodArgs.add(solve(arg, module, baseScope, ((JCTree) arg).getStartPosition()));
      }
      List<EntityWithContext> methods = scan(node.getMethodSelect(), null);
      methodArgs = null;
      return applyTypeArguments(methods, node.getTypeArguments());
    }

    @Override
    public List<EntityWithContext> visitNewClass(NewClassTree node, Void unused) {
      List<EntityWithContext> baseClassEntities;
      if (node.getEnclosingExpression() != null) {
        // <EnclosingExpression>.new <identifier>(...).
        EntityWithContext enclosingClass =
            solveEntityType(scan(node.getEnclosingExpression(), null), module);
        if (enclosingClass == null || !(enclosingClass.getEntity() instanceof ClassEntity)) {
          return ImmutableList.of();
        }
        baseClassEntities =
            new ExpressionDefinitionScanner(
                    module,
                    ((ClassEntity) enclosingClass.getEntity()).getChildScope(),
                    -1 /* position is useless for solving classes. */,
                    allowedEntityKinds)
                .scan(node.getIdentifier(), null);
      } else {
        baseClassEntities = scan(node.getIdentifier(), null);
      }
      return applyTypeArguments(
          baseClassEntities
              .stream()
              .map(baseClass -> baseClass.toBuilder().setInstanceContext(true).build())
              .collect(Collectors.toList()),
          node.getTypeArguments());
    }

    @Override
    public List<EntityWithContext> visitMemberSelect(MemberSelectTree node, Void unused) {
      List<Optional<SolvedType>> savedMethodArgs = methodArgs;
      methodArgs = null;
      List<EntityWithContext> expressionEntities = scan(node.getExpression(), null);
      EntityWithContext expressionType = solveEntityType(expressionEntities, module);
      methodArgs = savedMethodArgs;
      if (expressionType == null) {
        return ImmutableList.of();
      }

      String identifier = node.getIdentifier().toString();

      if (isMethodInvocation()) {
        return ImmutableList.copyOf(
            memberSolver.findMethodMembers(identifier, methodArgs, expressionType, module));
      } else {
        return toList(memberSolver.findNonMethodMember(identifier, expressionType, module));
      }
    }

    @Override
    public List<EntityWithContext> visitArrayAccess(ArrayAccessTree node, Void unused) {
      EntityWithContext expType = solveEntityType(scan(node.getExpression(), null), module);
      if (expType == null || expType.getArrayLevel() == 0) {
        return ImmutableList.of();
      }

      return ImmutableList.of(expType.toBuilder().decrementArrayLevel().build());
    }

    @Override
    public List<EntityWithContext> visitIdentifier(IdentifierTree node, Void unused) {
      String identifier = node.getName().toString();

      if (IDENT_THIS.equals(identifier)) {
        ClassEntity enclosingClass = findEnclosingClass(baseScope);
        return toList(
            enclosingClass,
            true /* isInstanceContext */,
            typeSolver.solveTypeParameters(
                enclosingClass.getTypeParameters(),
                ImmutableList.<TypeArgument>of(),
                contextTypeParameters,
                enclosingClass,
                module));
      }

      if (IDENT_SUPER.equals(identifier)) {
        ClassEntity enclosingClass = findEnclosingClass(baseScope);
        if (enclosingClass != null && enclosingClass.getSuperClass().isPresent()) {
          return toList(
              typeSolver
                  .solve(
                      enclosingClass.getSuperClass().get(),
                      contextTypeParameters,
                      enclosingClass.getParentScope().get(),
                      module)
                  .filter(solvedType -> solvedType instanceof SolvedReferenceType)
                  .map(
                      solvedType -> {
                        SolvedReferenceType superClass = (SolvedReferenceType) solvedType;
                        return EntityWithContext.builder()
                            .setEntity(superClass.getEntity())
                            .setSolvedTypeParameters(superClass.getTypeParameters())
                            .setArrayLevel(0)
                            .setInstanceContext(true)
                            .build();
                      }));
        }
      }

      if (contextTypeParameters.getTypeParameter(identifier).isPresent()) {
        return ImmutableList.of(
            EntityWithContext.from(contextTypeParameters.getTypeParameter(identifier).get())
                .build());
      }

      List<EntityWithContext> entities =
          typeSolver.findEntitiesFromScope(
              node.getName().toString(), baseScope, module, position, getAllowedEntityKinds());

      if (!entities.isEmpty()) {
        if (isMethodInvocation()) {
          entities = overloadSolver.prioritizeMatchedMethod(entities, methodArgs, module);
        }
        return entities;
      }

      if (isMethodInvocation()) {
        // Method cannot be direct memeber of root package.
        return ImmutableList.of();
      }

      return toList(
          typeSolver.findClassOrPackageInModule(
              ImmutableList.of(node.getName().toString()), module),
          false /* isInstanceContext */,
          contextTypeParameters);
    }

    @Override
    public List<EntityWithContext> visitLiteral(LiteralTree node, Void unused) {
      Object value = node.getValue();
      EntityWithContext.Builder builder = EntityWithContext.simpleBuilder();

      if (value == null) {
        return ImmutableList.of(builder.setEntity(NullEntity.INSTANCE).build());
      }

      if (value instanceof String) {
        return toList(
            typeSolver.findClassInModule(TypeSolver.JAVA_LANG_STRING_QUALIFIERS, module),
            true /* isInstanceContext */,
            SolvedTypeParameters.EMPTY);
      }

      Optional<PrimitiveEntity> primitiveEntity = PrimitiveEntity.get(value.getClass());
      if (primitiveEntity.isPresent()) {
        return ImmutableList.of(builder.setEntity(primitiveEntity.get()).build());
      }

      logger.warning("Unknown literal type: %s", value);
      return ImmutableList.of();
    }

    @Override
    public List<EntityWithContext> visitLambdaExpression(LambdaExpressionTree node, Void unused) {
      // TODO: implement this.
      return ImmutableList.of();
    }

    @Override
    public List<EntityWithContext> visitTypeCast(TypeCastTree node, Void unused) {
      TypeReference typeReference = new TypeReferenceScanner().getTypeReference(node.getType());
      Optional<SolvedType> solvedType =
          typeSolver.solve(typeReference, contextTypeParameters, baseScope, module);
      return toList(solvedType.map(t -> EntityWithContext.from(t).build()));
    }

    private List<EntityWithContext> applyTypeArguments(
        List<EntityWithContext> entities, List<? extends Tree> typeArguments) {
      // TODO: implement this;
      return entities;
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

    private List<EntityWithContext> toList(Optional<EntityWithContext> optionalEntityWithContext) {
      if (optionalEntityWithContext.isPresent()) {
        return ImmutableList.of(optionalEntityWithContext.get());
      }
      return ImmutableList.of();
    }

    private List<EntityWithContext> toList(
        Optional<Entity> optionalEntity,
        boolean isInstanceContext,
        SolvedTypeParameters solvedTypeParameters) {
      return toList(optionalEntity.orElse(null), isInstanceContext, solvedTypeParameters);
    }

    private List<EntityWithContext> toList(
        @Nullable Entity entity,
        boolean isInstanceContext,
        SolvedTypeParameters solvedTypeParameters) {
      if (entity == null) {
        return ImmutableList.of();
      }
      return ImmutableList.of(
          EntityWithContext.builder()
              .setArrayLevel(0)
              .setEntity(entity)
              .setInstanceContext(isInstanceContext)
              .setSolvedTypeParameters(solvedTypeParameters)
              .build());
    }
  }
}
