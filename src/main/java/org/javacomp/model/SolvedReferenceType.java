package org.javacomp.model;

import com.google.auto.value.AutoValue;

/** A solved type that is a simple reference type. */
@AutoValue
public abstract class SolvedReferenceType extends SolvedEntityType {
  @Override
  public abstract ClassEntity getEntity();

  public abstract SolvedTypeParameters getTypeParameters();

  public static SolvedReferenceType create(
      ClassEntity classEntity, SolvedTypeParameters typeParameters) {
    return new AutoValue_SolvedReferenceType(classEntity, typeParameters);
  }
}
