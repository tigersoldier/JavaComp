package org.javacomp.completion;

import com.google.common.collect.ImmutableList;
import org.javacomp.project.PositionContext;

/** Action to perform the requested completion. */
interface CompletionAction {
  ImmutableList<CompletionCandidate> getCompletionCandidates(
      PositionContext positionContext, String prefix);
}
