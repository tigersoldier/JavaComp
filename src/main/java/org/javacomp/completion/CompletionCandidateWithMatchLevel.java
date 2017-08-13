package org.javacomp.completion;

import com.google.auto.value.AutoValue;

/** A wrapper of {@link CompletionCandidate} and {@link CompletionPrefixMatcher#MatchLevel}. */
@AutoValue
public abstract class CompletionCandidateWithMatchLevel
    implements Comparable<CompletionCandidateWithMatchLevel> {
  public abstract CompletionCandidate getCompletionCandidate();

  public abstract CompletionPrefixMatcher.MatchLevel getMatchLevel();

  public static CompletionCandidateWithMatchLevel create(
      CompletionCandidate completionCandidate, CompletionPrefixMatcher.MatchLevel matchLevel) {
    return new AutoValue_CompletionCandidateWithMatchLevel(completionCandidate, matchLevel);
  }

  @Override
  public int compareTo(CompletionCandidateWithMatchLevel other) {
    if (this.getMatchLevel().ordinal() != other.getMatchLevel().ordinal()) {
      return other.getMatchLevel().ordinal() - this.getMatchLevel().ordinal();
    }
    return this.getCompletionCandidate()
        .getName()
        .compareTo(other.getCompletionCandidate().getName());
  }
}
