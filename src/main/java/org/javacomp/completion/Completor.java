package org.javacomp.completion;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.ModuleScope;
import org.javacomp.parser.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.MemberSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Entry point of completion logic. */
public class Completor {

  private static final String CONSTRUCTOR_NAME = "<init>";

  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;

  public Completor() {
    this.typeSolver = new TypeSolver();
    OverloadSolver overloadSolver = new OverloadSolver(typeSolver);
    this.expressionSolver =
        new ExpressionSolver(
            typeSolver, overloadSolver, new MemberSolver(typeSolver, overloadSolver));
  }

  /**
   * @param moduleScope the global scope of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public List<CompletionCandidate> getCompletionCandidates(
      ModuleScope moduleScope, Path filePath, int line, int column) {
    if (column > 0) {
      // PositionContext gets the tree path whose leaf node includes the position
      // (position < node's endPosition). However, for completions, we want the leaf node either
      // includes the position, or just before the position (position == node's endPosition).
      // Decresing column by 1 will decrease position by 1, which makes
      // adjustedPosition == node's endPosition - 1 if the node is just before the actual position.
      column--;
    }
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(moduleScope, filePath, line, column);

    if (!positionContext.isPresent()) {
      return ImmutableList.of();
    }

    TreePath treePath = positionContext.get().getTreePath();
    CompletionAction action;
    if (treePath.getLeaf() instanceof MemberSelectTree) {
      action = new CompleteMemberAction(treePath, typeSolver, expressionSolver);
    } else {
      action = new CompleteEntityAction(typeSolver, expressionSolver);
    }

    // TODO: filter and sort candidates by query.
    return action
        .getCompletionCandidates(positionContext.get())
        .stream()
        .filter(candidate -> !CONSTRUCTOR_NAME.equals(candidate.getName()))
        .collect(ImmutableList.toImmutableList());
  }
}
