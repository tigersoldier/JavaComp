package org.javacomp.completion;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.List;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;

/** Entry point of completion logic. */
public class Completor {

  private static final String CONSTRUCTOR_NAME = "<init>";

  /**
   * @param globalScope the global scope of the project
   * @param inputFileScope the parsed file scope of the input file
   * @param completionUnit the parsed completion unit of the input file
   * @param filename the filename of the input file
   * @param input the partial content of the input file, ended at the completion point
   */
  public List<CompletionCandidate> getCompletionCandidates(
      GlobalScope globalScope,
      FileScope inputFileScope,
      JCCompilationUnit compilationUnit,
      String filename,
      String input) {
    EntityScope completionPointScope = inputFileScope.getEntityScopeAt(input.length() - 1);
    CompletionAction action = new CompletionAst().getCompletionAction(compilationUnit);
    Multimap<String, Entity> entities =
        action.getVisibleEntities(globalScope, completionPointScope);
    // TODO: filter and sort candidates by query.
    return FluentIterable.from(entities.entries())
        .transform(
            entry -> {
              return CompletionCandidate.builder().setName(entry.getKey()).build();
            })
        .filter(candidate -> !CONSTRUCTOR_NAME.equals(candidate.getName()))
        .toList();
  }
}
