package org.javacomp.model;

import com.google.auto.value.AutoValue;

/** A solved type that is referencing to a package. */
@AutoValue
public abstract class SolvedPackageType extends SolvedEntityType {
  @Override
  public abstract PackageEntity getEntity();

  public static SolvedPackageType create(PackageEntity packageEntity) {
    return new AutoValue_SolvedPackageType(packageEntity);
  }
}
