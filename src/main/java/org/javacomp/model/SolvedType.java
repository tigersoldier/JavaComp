package org.javacomp.model;

import com.google.auto.value.AutoValue;

/** The actual type solved for {@link TypeReference}. */
@AutoValue
public abstract class SolvedType {

  public abstract Entity getEntity();

  public abstract boolean isPrimitive();

  public abstract boolean isArray();

  public static Builder builder() {
    return new AutoValue_SolvedType.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setEntity(Entity entity);

    public abstract Builder setPrimitive(boolean isPrimitive);

    public abstract Builder setArray(boolean isArray);

    public abstract SolvedType build();
  }
}
