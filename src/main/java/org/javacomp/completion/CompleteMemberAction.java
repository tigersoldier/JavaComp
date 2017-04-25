package org.javacomp.completion;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.SolvedType;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.TypeSolver;

/** An action to get completion candidates for member selection. */
class CompleteMemberAction implements CompletionAction {
  private final ExpressionTree memberExpression;
  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;

  CompleteMemberAction(
      TreePath treePath, TypeSolver typeSolver, ExpressionSolver expressionSolver) {
    checkArgument(
        treePath.getLeaf() instanceof MemberSelectTree,
        "Expecting MemberSelectTree, but got %s",
        treePath.getLeaf().getClass().getSimpleName());

    this.memberExpression = ((MemberSelectTree) treePath.getLeaf()).getExpression();
    this.typeSolver = typeSolver;
    this.expressionSolver = expressionSolver;
  }

  @Override
  public Multimap<String, Entity> getVisibleEntities(
      GlobalScope globalScope, EntityScope completionPointScope) {
    Optional<SolvedType> solvedType =
        expressionSolver.solve(memberExpression, globalScope, completionPointScope);
    if (!solvedType.isPresent()) {
      return ImmutableMultimap.of();
    }

    Entity expressionEntity = solvedType.get().getEntity();
    if (expressionEntity instanceof ClassEntity) {
      ClassEntity actualClassEntity = (ClassEntity) expressionEntity;
      ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
      for (ClassEntity classEntity : typeSolver.classHierarchy(actualClassEntity, globalScope)) {
        builder.putAll(classEntity.getMemberEntities());
      }
      return builder.build();
    }

    return solvedType.get().getEntity().getChildScope().getAllEntities();
  }
}
