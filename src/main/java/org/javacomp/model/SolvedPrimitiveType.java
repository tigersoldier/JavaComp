package org.javacomp.model;

import com.google.auto.value.AutoValue;

/** A solved type that is primitive. */
@AutoValue
public abstract class SolvedPrimitiveType extends SolvedEntityType {
  @Override
  public abstract PrimitiveEntity getEntity();

  public static SolvedPrimitiveType create(PrimitiveEntity primitiveEntity) {
    return new AutoValue_SolvedPrimitiveType(primitiveEntity);
  }
}
