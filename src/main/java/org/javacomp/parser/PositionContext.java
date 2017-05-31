package org.javacomp.parser;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import java.util.Optional;
import org.javacomp.logging.JLogger;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;

/** All information inferred from a given cursor position of a file. */
@AutoValue
public abstract class PositionContext {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  public abstract EntityScope getScopeAtPosition();

  public abstract Module getModule();

  public abstract TreePath getTreePath();

  public abstract int getPosition();

  public abstract EndPosTable getEndPosTable();

  /**
   * Creates a {@link PositionContext} instance based on the given file path and position.
   *
   * @param module the module of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public static Optional<PositionContext> createForPosition(
      Module module, Path filePath, int line, int column) {
    Optional<FileScope> inputFileScope = module.getFileScope(filePath.toString());
    if (!inputFileScope.isPresent()) {
      return Optional.ofNullable(null);
    }

    JCCompilationUnit compilationUnit = inputFileScope.get().getCompilationUnit();
    LineMap lineMap = inputFileScope.get().getLineMap();
    // LineMap accepts 1-based line and column numbers.
    int position = (int) lineMap.getPosition(line + 1, column + 1);
    EntityScope scopeAtPosition = inputFileScope.get().getEntityScopeAt(position - 1);
    PositionAstScanner scanner = new PositionAstScanner(compilationUnit.endPositions, position);
    logger.fine("Starting PositionAstScanner, position: %s", position);
    TreePath treePath = scanner.scan(compilationUnit, null);
    logger.fine("TreePath for position: %s", TreePathFormatter.formatTreePath(treePath));

    return Optional.of(
        new AutoValue_PositionContext(
            scopeAtPosition, module, treePath, position, compilationUnit.endPositions));
  }

  /** A {@link TreePathScanner} that returns the tree path enclosing the given position. */
  private static class PositionAstScanner extends TreePathScanner<TreePath, Void> {
    private final EndPosTable endPosTable;
    private final int position;

    private PositionAstScanner(EndPosTable endPosTable, int position) {
      this.endPosTable = endPosTable;
      this.position = position;
    }

    @Override
    public TreePath scan(Tree tree, Void unused) {
      if (tree == null) {
        return null;
      }

      JCTree jcTree = (JCTree) tree;
      int startPosition = jcTree.getStartPosition();
      int endPosition = jcTree.getEndPosition(endPosTable);
      boolean positionInNodeRange =
          (startPosition < 0 || startPosition <= position)
              && (position < endPosition || endPosition < 0);
      logger.fine(
          "PositionAstScanner: visiting node: %s, start: %s, end: %s.%s",
          tree.accept(new TreePathFormatter.TreeFormattingVisitor(), null),
          jcTree.getStartPosition(),
          jcTree.getEndPosition(endPosTable),
          positionInNodeRange ? " ✔" : "");
      if (!positionInNodeRange) {
        return null;
      }
      TreePath currentPath = new TreePath(getCurrentPath(), tree);

      TreePath ret = super.scan(tree, null);
      if (ret != null) {
        return ret;
      }

      return (tree instanceof ErroneousTree) ? null : currentPath;
    }

    @Override
    public TreePath visitErroneous(ErroneousTree node, Void unused) {
      for (Tree tree : node.getErrorTrees()) {
        TreePath ret = scan(tree, unused);
        if (ret != null) {
          return ret;
        }
      }
      return null;
    }

    @Override
    public TreePath reduce(TreePath r1, TreePath r2) {
      if (r1 != null) {
        return r1;
      }
      return r2;
    }
  }
}
