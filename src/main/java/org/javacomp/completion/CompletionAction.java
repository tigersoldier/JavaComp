package org.javacomp.completion;

import java.util.List;
import org.javacomp.parser.PositionContext;

/** Action to perform the requested completion. */
interface CompletionAction {
  public List<CompletionCandidate> getCompletionCandidates(
      PositionContext positionContext, String prefix);
}
