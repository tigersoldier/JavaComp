package org.javacomp.completion;

import java.util.Optional;

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

  public String getName();

  default public Optional<String> getInsertPlainText() {
    return Optional.empty();
  }

  default public Optional<String> getInsertSnippet() {
    return Optional.empty();
  }

  public Kind getKind();

  public Optional<String> getDetail();
}
