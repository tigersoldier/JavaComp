package org.javacomp.completion;

import java.util.Optional;
import org.javacomp.model.Entity;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.VariableEntity;

/** A {@link CompletionCandidate} backed by {@link Entity}. */
class EntityCompletionCandidate implements CompletionCandidate {
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

  @Override
  public Optional<String> getDetail() {
    switch (entity.getKind()) {
      case METHOD:
        {
          StringBuilder sb = new StringBuilder();
          MethodEntity method = (MethodEntity) entity;
          sb.append("(");
          boolean firstParam = true;
          for (VariableEntity param : method.getParameters()) {
            if (firstParam) {
              firstParam = false;
            } else {
              sb.append(", ");
            }
            sb.append(param.getType().toDisplayString());
            sb.append(" ");
            sb.append(param.getSimpleName());
          }
          sb.append("): ");
          sb.append(method.getReturnType().toDisplayString());
          return Optional.of(sb.toString());
        }
      case VARIABLE:
      case FIELD:
        {
          VariableEntity variable = (VariableEntity) entity;
          return Optional.of(variable.getType().toDisplayString());
        }
      default:
        return Optional.empty();
    }
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
