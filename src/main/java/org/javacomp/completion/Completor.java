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
   * @param inputFileScope the parsed file scope of the input file
   * @param completionUnit the parsed completion unit of the input file
   * @param input the partial content of the input file, ended at the completion point
   */
  public List<CompletionCandidate> getCompletionCandidates(
      GlobalScope globalScope, Path filePath, int line, int column) {
    Optional<FileScope> inputFileScope = globalScope.getFileScope(filePath.toString());
    if (!inputFileScope.isPresent()) {
      return ImmutableList.of();
    }

    JCCompilationUnit compilationUnit = inputFileScope.get().getCompilationUnit();
    LineMap lineMap = compilationUnit.getLineMap();
    int position = (int) lineMap.getPosition(line, column);
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
