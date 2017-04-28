package org.javacomp.completion;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SimpleCompletionCandidate implements CompletionCandidate {
  public static Builder builder() {
    return new AutoValue_SimpleCompletionCandidate.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract Builder setKind(Kind kind);

    public abstract SimpleCompletionCandidate build();
  }
}
