package org.javacomp.completion;

import java.util.List;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;

/** Action to perform the requested completion. */
interface CompletionAction {
  public List<CompletionCandidate> getCompletionCandidates(
      GlobalScope globalScope, EntityScope completionPointScope);
}
