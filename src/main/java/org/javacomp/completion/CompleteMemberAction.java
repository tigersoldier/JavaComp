package org.javacomp.completion;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.SolvedEntityType;
import org.javacomp.model.SolvedReferenceType;
import org.javacomp.model.SolvedType;
import org.javacomp.parser.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.TypeSolver;

/** An action to get completion candidates for member selection. */
class CompleteMemberAction implements CompletionAction {
  private final JLogger logger = JLogger.createForEnclosingClass();
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
  public List<CompletionCandidate> getCompletionCandidates(PositionContext positionContext) {
    Optional<SolvedType> solvedType =
        expressionSolver.solve(
            memberExpression,
            positionContext.getModule(),
            positionContext.getScopeAtPosition(),
            positionContext.getPosition());
    logger.fine("Solved member expression: %s", solvedType);
    if (!solvedType.isPresent()) {
      return ImmutableList.of();
    }

    if (solvedType.get() instanceof SolvedReferenceType) {
      Collection<Entity> classMembers =
          new ClassMemberCompletor(typeSolver, expressionSolver)
              .getClassMembers(
                  EntityWithContext.from(solvedType.get()).build(), positionContext.getModule())
              .values();
      return createCompletionCandidates(classMembers);
    }

    if (solvedType.get() instanceof SolvedEntityType)
      return createCompletionCandidates(
          ((SolvedEntityType) solvedType.get())
              .getEntity()
              .getChildScope()
              .getMemberEntities()
              .values());

    // TODO: handle array type
    logger.warning("Unsupported member completion of type %s", solvedType.get());
    return ImmutableList.of();
  }

  private static List<CompletionCandidate> createCompletionCandidates(Collection<Entity> entities) {
    return entities
        .stream()
        .map((entity) -> new EntityCompletionCandidate(entity))
        .collect(ImmutableList.toImmutableList());
  }
}
