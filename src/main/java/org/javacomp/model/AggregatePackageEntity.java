package org.javacomp.model;

import java.util.List;

/** A {@link PackageEntity} that associates to {@link AggregatePackageScope}. */
public class AggregatePackageEntity extends PackageEntity {
  public AggregatePackageEntity(String simpleName, List<String> qualifiers) {
    super(simpleName, qualifiers, new AggregatePackageScope());
  }

  @Override
  public AggregatePackageScope getChildScope() {
    return (AggregatePackageScope) super.getChildScope();
  }
}
