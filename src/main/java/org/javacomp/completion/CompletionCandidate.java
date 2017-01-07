package org.javacomp.completion;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CompletionCandidate {
  public abstract String getName();

  public static CompletionCandidate.Builder builder() {
    return new AutoValue_CompletionCandidate.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract CompletionCandidate build();
  }
}
