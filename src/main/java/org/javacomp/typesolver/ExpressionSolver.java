package org.javacomp.typesolver;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.VariableEntity;

/** Logic for solving the result type of an expression. */
public class ExpressionSolver {
  private static final Set<Entity.Kind> ALLOWED_KINDS_METHOD = ImmutableSet.of(Entity.Kind.METHOD);
  private static final Set<Entity.Kind> ALLOWED_KINDS_NON_METHOD =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(ClassEntity.ALLOWED_KINDS)
          .add(Entity.Kind.VARIABLE)
          .add(Entity.Kind.QUALIFIER)
          .build();
  private static final String IDENT_THIS = "this";
  private static final String IDENT_SUPER = "super";

  private final TypeSolver typeSolver;

  public ExpressionSolver(TypeSolver typeSolver) {
    this.typeSolver = typeSolver;
  }

  public Optional<SolvedType> solve(
      ExpressionTree expression, GlobalScope globalScope, EntityScope baseScope) {
    return Optional.ofNullable(
        new ExpressionTypeScanner(globalScope, baseScope).scan(expression, null /* unused */));
  }

  private class ExpressionTypeScanner extends TreeScanner<SolvedType, Void> {
    private final EntityScope baseScope;
    private final GlobalScope globalScope;
    private List<Optional<SolvedType>> methodArgs;

    private ExpressionTypeScanner(GlobalScope globalScope, EntityScope baseScope) {
      this.globalScope = globalScope;
      this.baseScope = baseScope;
      this.methodArgs = null;
    }

    @Override
    public SolvedType visitMethodInvocation(MethodInvocationTree node, Void unused) {
      methodArgs = new ArrayList<>(node.getArguments().size());
      for (ExpressionTree arg : node.getArguments()) {
        methodArgs.add(solve(arg, globalScope, baseScope));
      }
      SolvedType solvedType = scan(node.getMethodSelect(), null);
      methodArgs = null;
      return solvedType;
    }

    @Override
    public SolvedType visitMemberSelect(MemberSelectTree node, Void unused) {
      List<Optional<SolvedType>> savedMethodArgs = methodArgs;
      methodArgs = null;
      SolvedType expressionType = scan(node.getExpression(), null);
      if (expressionType == null) {
        return null;
      }
      methodArgs = savedMethodArgs;

      String identifier = node.getIdentifier().toString();
      if (IDENT_THIS.equals(identifier)) {
        return expressionType;
      }

      if (isMethodInvocation()) {
        // Methods must be defined in classes.
        if (expressionType.getEntity() instanceof ClassEntity) {
          return solveMethodReturnType(
              typeSolver.findClassMethods(
                  node.getIdentifier().toString(),
                  (ClassEntity) expressionType.getEntity(),
                  globalScope));
        }
        return null;
      } else {
        Entity memberEntity =
            typeSolver.findEntityMember(
                node.getIdentifier().toString(),
                expressionType.getEntity(),
                globalScope,
                ALLOWED_KINDS_NON_METHOD);

        return solveNonMethodEntityType(memberEntity);
      }
    }

    @Override
    public SolvedType visitIdentifier(IdentifierTree node, Void unused) {
      String identifier = node.getName().toString();

      if (IDENT_THIS.equals(identifier)) {
        return solveNonMethodEntityType(findEnclosingClass(baseScope));
      }

      if (IDENT_SUPER.equals(identifier)) {
        ClassEntity enclosingClass = findEnclosingClass(baseScope);
        if (enclosingClass != null && enclosingClass.getSuperClass().isPresent()) {
          return typeSolver
              .solve(
                  enclosingClass.getSuperClass().get(),
                  globalScope,
                  enclosingClass.getParentScope().get())
              .orElse(null);
        }
      }

      List<Entity> entities =
          typeSolver.findEntitiesInScope(
              node.getName().toString(), globalScope, baseScope, getAllowedEntityKinds());

      if (!entities.isEmpty()) {
        return solveEntityType(entities);
      }

      if (isMethodInvocation()) {
        // Method cannot be direct memeber of root package.
        return null;
      }
      return solveNonMethodEntityType(
          typeSolver.findDirectMember(
              node.getName().toString(), globalScope, ALLOWED_KINDS_NON_METHOD));
    }

    private Set<Entity.Kind> getAllowedEntityKinds() {
      return methodArgs == null ? ALLOWED_KINDS_NON_METHOD : ALLOWED_KINDS_METHOD;
    }

    @Nullable
    private SolvedType solveEntityType(List<Entity> foundEntities) {
      if (isMethodInvocation()) {
        return solveMethodReturnType(foundEntities);
      } else {
        // Non-method entities have no overload
        return solveNonMethodEntityType(foundEntities.get(0));
      }
    }

    @Nullable
    private SolvedType solveMethodReturnType(List<Entity> methodOverloads) {
      // TODO: properly support overloading resolution
      for (Entity entity : methodOverloads) {
        MethodEntity method = (MethodEntity) entity;
        if (method.getParameters().size() == methodArgs.size()) {
          return typeSolver.solve(method.getReturnType(), globalScope, baseScope).orElse(null);
        }
      }
      return null;
    }

    @Nullable
    private SolvedType solveNonMethodEntityType(@Nullable Entity entity) {
      if (entity == null) {
        return null;
      }
      if (entity instanceof VariableEntity) {
        return typeSolver
            .solve(((VariableEntity) entity).getType(), globalScope, baseScope)
            .orElse(null);
      }
      if (entity instanceof ClassEntity) {
        return SolvedType.builder().setEntity(entity).setPrimitive(false).setArray(false).build();
      }
      if (entity instanceof PackageEntity) {
        return SolvedType.builder().setEntity(entity).setPrimitive(false).setArray(false).build();
      }
      return null;
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
  }
}
