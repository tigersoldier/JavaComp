package org.javacomp.completion;

import com.google.common.collect.ImmutableList;
import org.javacomp.parser.PositionContext;

/** Action to perform the requested completion. */
interface CompletionAction {
  public ImmutableList<CompletionCandidate> getCompletionCandidates(
      PositionContext positionContext, String prefix);
}
