package org.javacomp.completion;

import com.google.auto.value.AutoValue;
import java.util.Optional;

@AutoValue
public abstract class SimpleCompletionCandidate implements CompletionCandidate {
  public static Builder builder() {
    return new AutoValue_SimpleCompletionCandidate.Builder();
  }

  @Override
  public Optional<String> getDetail() {
    return Optional.ofNullable(null);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract Builder setKind(Kind kind);

    public abstract SimpleCompletionCandidate build();
  }
}
