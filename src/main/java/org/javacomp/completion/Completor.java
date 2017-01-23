package org.javacomp.completion;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.List;
import org.javacomp.model.FileIndex;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityIndex;

/** Entry point of completion logic. */
public class Completor {

  private static final String CONSTRUCTOR_NAME = "<init>";

  /**
   * @param globalIndex the global index of the project
   * @param inputFileIndex the parsed file index of the input file
   * @param completionUnit the parsed completion unit of the input file
   * @param filename the filename of the input file
   * @param input the partial content of the input file, ended at the completion point
   */
  public List<CompletionCandidate> getCompletionCandidates(
      GlobalIndex globalIndex,
      FileIndex inputFileIndex,
      JCCompilationUnit compilationUnit,
      String filename,
      String input) {
    EntityIndex completionPointIndex = inputFileIndex.getEntityIndexAt(input.length() - 1);
    CompletionAction action = new CompletionAst().getCompletionAction(compilationUnit);
    Multimap<String, Entity> entities = action.getVisibleEntities(globalIndex, completionPointIndex);
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
