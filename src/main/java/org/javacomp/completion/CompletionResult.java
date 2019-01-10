package org.javacomp.completion;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/** Result of completion request. */
@AutoValue
public abstract class CompletionResult {

  public abstract ImmutableList<CompletionCandidate> candidates();

  public abstract int prefixLine();

  public abstract int prefixStartColumn();

  public abstract int prefixEndColumn();

  public static Builder builder() {
    return new AutoValue_CompletionResult.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder candidates(ImmutableList<CompletionCandidate> candidates);

    public abstract Builder prefixLine(int line);

    public abstract Builder prefixStartColumn(int startColumn);

    public abstract Builder prefixEndColumn(int endColumn);

    public abstract CompletionResult build();
  }
}
