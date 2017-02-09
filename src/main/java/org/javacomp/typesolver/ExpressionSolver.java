package org.javacomp.typesolver;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ArrayAccessTree;
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
import org.javacomp.model.PrimitiveEntity;
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
  private static final String IDENT_LENGTH = "length";

  private final TypeSolver typeSolver;
  private final OverloadSolver overloadSolver;

  public ExpressionSolver(TypeSolver typeSolver, OverloadSolver overloadSolver) {
    this.typeSolver = typeSolver;
    this.overloadSolver = overloadSolver;
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

      ///////
      // OuterClass.this
      if (IDENT_THIS.equals(identifier)) {
        return expressionType;
      }

      ////////
      //  someArray.length
      if (expressionType.isArray() && IDENT_LENGTH.equals(identifier)) {
        return SolvedType.builder()
            .setEntity(PrimitiveEntity.INT)
            .setPrimitive(true)
            .setArray(false)
            .build();
      }

      ///////
      //  foo.bar(baz)
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
      }

      ////////
      //  foo.bar
      Entity memberEntity =
          typeSolver.findEntityMember(
              node.getIdentifier().toString(),
              expressionType.getEntity(),
              globalScope,
              ALLOWED_KINDS_NON_METHOD);

      return solveNonMethodEntityType(memberEntity);
    }

    @Override
    public SolvedType visitArrayAccess(ArrayAccessTree node, Void unused) {
      SolvedType expType = scan(node.getExpression(), null);
      if (expType == null || !expType.isArray()) {
        return null;
      }

      return expType.toBuilder().setArray(false).build();
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
    private SolvedType solveMethodReturnType(List<Entity> methodEntities) {
      if (methodEntities.isEmpty()) {
        return null;
      }
      for (Entity entity : methodEntities) {
        checkArgument(entity instanceof MethodEntity, "Entity %s is not a method", entity);
      }
      @SuppressWarnings("unchecked")
      List<MethodEntity> methods = (List<MethodEntity>) (List) methodEntities;
      MethodEntity solvedMethod = overloadSolver.solve(methods, methodArgs, globalScope);
      return typeSolver.solve(solvedMethod.getReturnType(), globalScope, baseScope).orElse(null);
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
