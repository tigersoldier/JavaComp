package org.javacomp.model;

import com.google.auto.value.AutoValue;

/** A solved type that is null. */
@AutoValue
public abstract class SolvedNullType implements SolvedType {
  public abstract NullEntity getNullEntity();

  public static SolvedNullType create(NullEntity nullEntity) {
    return new AutoValue_SolvedNullType(nullEntity);
  }
}
