package org.javacomp.model;

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;

/**
 * Reference to an {@link Entity} with additional contextual information for helping type solving.
 */
@AutoValue
public abstract class EntityWithContext {
  public abstract Entity getEntity();

  public abstract SolvedTypeParameters getSolvedTypeParameters();

  public abstract int getArrayLevel();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_EntityWithContext.Builder();
  }

  public static Builder simpleBuilder() {
    return builder().setArrayLevel(0).setSolvedTypeParameters(SolvedTypeParameters.EMPTY);
  }

  public static Builder from(SolvedType solvedType) {
    if (solvedType instanceof SolvedArrayType) {
      return from(((SolvedArrayType) solvedType).getBaseType()).incrementArrayLevel();
    }
    if (solvedType instanceof SolvedEntityType) {
      Builder builder =
          builder().setArrayLevel(0).setEntity(((SolvedEntityType) solvedType).getEntity());
      if (solvedType instanceof SolvedReferenceType) {
        builder.setSolvedTypeParameters(((SolvedReferenceType) solvedType).getTypeParameters());
      } else {
        builder.setSolvedTypeParameters(SolvedTypeParameters.EMPTY);
      }
      return builder;
    }

    throw new RuntimeException(
        "Cannot convert unsupported SolvedType to EntityWithContext: " + solvedType);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setEntity(Entity value);

    public abstract Builder setSolvedTypeParameters(SolvedTypeParameters value);

    public abstract Builder setArrayLevel(int value);

    public abstract int getArrayLevel();

    public Builder incrementArrayLevel() {
      return setArrayLevel(getArrayLevel() + 1);
    }

    public Builder decrementArrayLevel() {
      int currentArrayLevel = getArrayLevel();
      checkState(currentArrayLevel > 0, "Cannot decrement array level when it's already zero");
      return setArrayLevel(currentArrayLevel - 1);
    }

    public abstract EntityWithContext build();
  }
}
