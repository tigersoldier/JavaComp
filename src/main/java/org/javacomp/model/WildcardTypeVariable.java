package org.javacomp.model;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/** A {@link TypeVariable} starting with question mark (?). */
@AutoValue
public abstract class WildcardTypeVariable implements TypeVariable {

  public static WildcardTypeVariable create(Optional<Bound> bound) {
    return new AutoValue_WildcardTypeVariable(bound);
  }

  public abstract Optional<Bound> getBound();

  @Override
  public String toDisplayString() {
    if (getBound().isPresent()) {
      return "? " + getBound().get().toDisplayString();
    }
    return "?";
  }

  @Override
  public String toString() {
    return "WildcardTypeVariable<" + getBound() + ">";
  }

  @AutoValue
  public abstract static class Bound {
    public enum Kind {
      SUPER,
      EXTENDS,
    }

    public abstract Kind getKind();

    public abstract TypeReference getTypeReference();

    public String toDisplayString() {
      return (getKind() == Kind.SUPER ? "super" : "extends")
          + " "
          + getTypeReference().toDisplayString();
    }

    @Override
    public String toString() {
      return "Bound<" + getKind() + " " + getTypeReference() + ">";
    }

    public static Bound create(Kind kind, TypeReference typeReference) {
      return new AutoValue_WildcardTypeVariable_Bound(kind, typeReference);
    }
  }
}
