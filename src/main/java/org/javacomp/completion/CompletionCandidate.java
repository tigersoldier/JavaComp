package org.javacomp.completion;

public interface CompletionCandidate {
  public enum Kind {
    UNKNOWN,
    CLASS,
    INTERFACE,
    ENUM,
    METHOD,
    VARIABLE,
    FIELD,
    PACKAGE,
    KEYWORD,
  }

  public abstract String getName();

  public abstract Kind getKind();
}
