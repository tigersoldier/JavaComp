package org.javacomp.completion;

import org.javacomp.model.Entity;

/** A {@link CompletionCandidate} backed by {@link Entity}. */
class EntityCompletionCandidate extends CompletionCandidate {
  private final Entity entity;

  EntityCompletionCandidate(Entity entity) {
    this.entity = entity;
  }

  @Override
  public String getName() {
    return entity.getSimpleName();
  }

  @Override
  public Kind getKind() {
    return toCandidateKind(entity.getKind());
  }

  public static Kind toCandidateKind(Entity.Kind entityKind) {
    switch (entityKind) {
      case CLASS:
        return CompletionCandidate.Kind.CLASS;
      case ANNOTATION:
      case INTERFACE:
        return CompletionCandidate.Kind.INTERFACE;
      case ENUM:
        return CompletionCandidate.Kind.ENUM;
      case METHOD:
        return CompletionCandidate.Kind.METHOD;
      case VARIABLE:
      case PRIMITIVE:
        return CompletionCandidate.Kind.VARIABLE;
      case FIELD:
        return CompletionCandidate.Kind.FIELD;
      case QUALIFIER:
        return CompletionCandidate.Kind.PACKAGE;
      case REFERENCE:
      default:
        return CompletionCandidate.Kind.UNKNOWN;
    }
  }
}
