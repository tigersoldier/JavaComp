package org.javacomp.typesolver;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreeScanner;
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
    return Optional.fromNullable(
        new ExpressionTypeScanner(globalScope, baseScope).scan(expression, null /* unused */));
  }

  private class ExpressionTypeScanner extends TreeScanner<SolvedType, Void> {
    private final EntityScope baseScope;
    private final GlobalScope globalScope;
    private boolean isMethodInvocation;

    private ExpressionTypeScanner(GlobalScope globalScope, EntityScope baseScope) {
      this.globalScope = globalScope;
      this.baseScope = baseScope;
      this.isMethodInvocation = false;
    }

    @Override
    public SolvedType visitMemberSelect(MemberSelectTree node, Void unused) {
      boolean savedIsMethodInvocation = isMethodInvocation;
      isMethodInvocation = false;
      SolvedType expressionType = scan(node.getExpression(), null);
      if (expressionType == null) {
        return null;
      }
      isMethodInvocation = savedIsMethodInvocation;

      String identifier = node.getIdentifier().toString();
      if (IDENT_THIS.equals(identifier)) {
        return expressionType;
      }

      Entity memberEntity =
          typeSolver.findEntityMember(
              node.getIdentifier().toString(),
              expressionType.getEntity(),
              globalScope,
              getAllowedEntityKinds());

      return solveEntityType(memberEntity);
    }

    @Override
    public SolvedType visitIdentifier(IdentifierTree node, Void unused) {
      String identifier = node.getName().toString();

      if (IDENT_THIS.equals(identifier)) {
        return solveEntityType(findEnclosingClass(baseScope));
      }

      if (IDENT_SUPER.equals(identifier)) {
        ClassEntity enclosingClass = findEnclosingClass(baseScope);
        if (enclosingClass != null && enclosingClass.getSuperClass().isPresent()) {
          return typeSolver
              .solve(
                  enclosingClass.getSuperClass().get(),
                  globalScope,
                  enclosingClass.getParentScope().get())
              .orNull();
        }
      }

      Entity entity =
          typeSolver.findEntityInScope(
              node.getName().toString(), globalScope, baseScope, getAllowedEntityKinds());

      if (entity != null) {
        return solveEntityType(entity);
      }

      return solveEntityType(
          typeSolver.findDirectMember(
              node.getName().toString(), globalScope, getAllowedEntityKinds()));
    }

    private Set<Entity.Kind> getAllowedEntityKinds() {
      return isMethodInvocation ? ALLOWED_KINDS_METHOD : ALLOWED_KINDS_NON_METHOD;
    }

    @Nullable
    private SolvedType solveEntityType(@Nullable Entity entity) {
      if (entity == null) {
        return null;
      }
      if (entity instanceof VariableEntity) {
        return typeSolver
            .solve(((VariableEntity) entity).getType(), globalScope, baseScope)
            .orNull();
      }
      if (entity instanceof MethodEntity) {
        // TODO: support overloading resolution
        return typeSolver
            .solve(
                ((MethodEntity) entity).getOverloads().get(0).getReturnType(),
                globalScope,
                baseScope)
            .orNull();
      }
      if (entity instanceof ClassEntity) {
        return SolvedType.builder().setEntity(entity).build();
      }
      if (entity instanceof PackageEntity) {
        return SolvedType.builder().setEntity(entity).build();
      }
      return null;
    }

    @Nullable
    private ClassEntity findEnclosingClass(EntityScope baseScope) {
      for (; baseScope != null; baseScope = baseScope.getParentScope().orNull()) {
        if (baseScope instanceof ClassEntity) {
          return (ClassEntity) baseScope;
        }
      }
      return null;
    }
  }
}
