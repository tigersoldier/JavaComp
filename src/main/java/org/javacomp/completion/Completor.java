package org.javacomp.completion;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import org.javacomp.file.FileManager;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.FileScope;
import org.javacomp.parser.LineMapUtil;
import org.javacomp.project.ModuleManager;
import org.javacomp.project.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.MemberSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Entry point of completion logic. */
public class Completor {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final FileManager fileManager;
  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;

  private CachedCompletion cachedCompletion = CachedCompletion.NONE;

  public Completor(FileManager fileManager) {
    this.fileManager = fileManager;
    this.typeSolver = new TypeSolver();
    OverloadSolver overloadSolver = new OverloadSolver(typeSolver);
    this.expressionSolver =
        new ExpressionSolver(
            typeSolver, overloadSolver, new MemberSolver(typeSolver, overloadSolver));
  }

  /**
   * @param module the module of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public CompletionResult getCompletionCandidates(
      ModuleManager moduleManager, Path filePath, int line, int column) {
    // PositionContext gets the tree path whose leaf node includes the position
    // (position < node's endPosition). However, for completions, we want the leaf node either
    // includes the position, or just before the position (position == node's endPosition).
    // Decresing column by 1 will decrease position by 1, which makes
    // adjustedPosition == node's endPosition - 1 if the node is just before the actual position.
    int contextColumn = column > 0 ? column - 1 : 0;
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(moduleManager, filePath, line, contextColumn);

    if (!positionContext.isPresent()) {
      return CompletionResult.builder()
          .candidates(ImmutableList.of())
          .prefixLine(line)
          .prefixStartColumn(column)
          .prefixEndColumn(column)
          .build();
    }

    String prefix =
        extractCompletionPrefix(positionContext.get().getFileScope(), filePath, line, column);
    ImmutableList<CompletionCandidate> candidates;
    if (cachedCompletion.isIncrementalCompletion(filePath, line, column, prefix)) {
      candidates = getCompletionCandidatesFromCache(prefix);
    } else {
      candidates = computeCompletionCandidates(positionContext.get(), prefix);
      cachedCompletion =
          CachedCompletion.builder()
              .setFilePath(filePath)
              .setLine(line)
              .setColumn(column)
              .setPrefix(prefix)
              .setCompletionCandidates(candidates)
              .build();
    }
    // TODO: limit the number of the candidates.
    return CompletionResult.builder()
        .candidates(candidates)
        .prefixLine(line)
        .prefixStartColumn(column - prefix.length())
        .prefixEndColumn(column)
        .build();
  }

  private ImmutableList<CompletionCandidate> computeCompletionCandidates(
      PositionContext positionContext, String prefix) {
    TreePath treePath = positionContext.getTreePath();
    CompletionAction action;
    if (treePath.getLeaf() instanceof MemberSelectTree) {
      ExpressionTree parentExpression = ((MemberSelectTree) treePath.getLeaf()).getExpression();
      action =
          CompleteMemberAction.forMemberSelect(
              parentExpression, typeSolver, expressionSolver);
    } else if (treePath.getLeaf() instanceof MemberReferenceTree) {
      ExpressionTree parentExpression =
          ((MemberReferenceTree) treePath.getLeaf()).getQualifierExpression();
      action =
          CompleteMemberAction.forMethodReference(
              parentExpression, typeSolver, expressionSolver);
    } else if (treePath.getLeaf() instanceof LiteralTree) {
      // Do not complete on any literals, especially strings.
      action = NoCandidateAction.INSTANCE;
    } else {
      action = new CompleteSymbolAction(typeSolver, expressionSolver);
    }
    return action.getCompletionCandidates(positionContext, prefix);
  }

  private ImmutableList<CompletionCandidate> getCompletionCandidatesFromCache(String prefix) {
    return new CompletionCandidateListBuilder(prefix)
        .addCandidates(cachedCompletion.getCompletionCandidates())
        .build();
  }

  @VisibleForTesting
  String extractCompletionPrefix(FileScope fileScope, Path filePath, int line, int column) {
    CharSequence fileContent = fileManager.getFileContent(filePath).orElse(null);
    if (fileContent == null) {
      logger.warning("Cannot get file content of %s for completion prefix", filePath);
      return "";
    }
    // Get position of line, column. Note that we cannot use the position from
    // PositionContext because it's the position of the possibly modified
    // content, not the original content.
    JCCompilationUnit compilationUnit = fileScope.getCompilationUnit().get();
    LineMap lineMap = compilationUnit.getLineMap();
    int position = LineMapUtil.getPositionFromZeroBasedLineAndColumn(lineMap, line, column);
    if (position < 0) {
      logger.warning(
          "Position of (%s, %s): %s is negative when getting completion prefix for file %s",
          line, column, position, filePath);
    }
    if (position >= fileContent.length()) {
      logger.warning(
          "Position of (%s, %s): %s is greater than the length of the content %s when "
              + "getting completion prefix for file %s",
          line, column, position, fileContent.length(), filePath);
    }

    int start = position - 1;
    while (start >= 0 && Character.isJavaIdentifierPart(fileContent.charAt(start))) {
      start--;
    }
    return fileContent.subSequence(start + 1, position).toString();
  }

  /** A {@link CompletionAction} that always returns an empty list of candidates. */
  private static class NoCandidateAction implements CompletionAction {
    public static final NoCandidateAction INSTANCE = new NoCandidateAction();

    @Override
    public ImmutableList<CompletionCandidate> getCompletionCandidates(
        PositionContext positionContext, String prefix) {
      return ImmutableList.of();
    }
  }
}
