package org.javacomp.completion;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CompletionCandidate {
  public enum Kind {
    UNKNOWN,
    CLASS,
    INTERFACE,
    ENUM,
    METHOD,
    VARIABLE,
    FIELD,
    PACKAGE,
  }

  public abstract String getName();

  public abstract Kind getKind();

  public static CompletionCandidate.Builder builder() {
    return new AutoValue_CompletionCandidate.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String name);

    public abstract Builder setKind(Kind kind);

    public abstract CompletionCandidate build();
  }
}
