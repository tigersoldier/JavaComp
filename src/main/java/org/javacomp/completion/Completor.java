package org.javacomp.completion;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Entry point of completion logic. */
public class Completor {

  private static final String CONSTRUCTOR_NAME = "<init>";

  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;
  private final CompletionAst completionAst;

  public Completor() {
    this.typeSolver = new TypeSolver();
    this.expressionSolver = new ExpressionSolver(typeSolver, new OverloadSolver(typeSolver));
    this.completionAst = new CompletionAst(typeSolver, expressionSolver);
  }

  /**
   * @param globalScope the global scope of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public List<CompletionCandidate> getCompletionCandidates(
      GlobalScope globalScope, Path filePath, int line, int column) {
    Optional<FileScope> inputFileScope = globalScope.getFileScope(filePath.toString());
    if (!inputFileScope.isPresent()) {
      return ImmutableList.of();
    }

    JCCompilationUnit compilationUnit = inputFileScope.get().getCompilationUnit();
    LineMap lineMap = inputFileScope.get().getLineMap();
    // LineMap accepts 1-based line and column numbers.
    int position = (int) lineMap.getPosition(line + 1, column + 1);
    EntityScope completionPointScope = inputFileScope.get().getEntityScopeAt(position - 1);
    CompletionAction action = completionAst.getCompletionAction(compilationUnit, position);
    // TODO: filter and sort candidates by query.
    return action
        .getCompletionCandidates(globalScope, completionPointScope)
        .stream()
        .filter(candidate -> !CONSTRUCTOR_NAME.equals(candidate.getName()))
        .collect(ImmutableList.toImmutableList());
  }
}
