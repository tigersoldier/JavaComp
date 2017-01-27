package org.javacomp.model;

import java.util.List;

/** Represents a package. */
public class PackageEntity extends Entity {
  private final PackageScope packageScope;

  public PackageEntity(String simpleName, List<String> qualifiers, PackageScope packageScope) {
    super(simpleName, Entity.Kind.QUALIFIER, qualifiers);
    this.packageScope = packageScope;
  }

  @Override
  public PackageScope getChildScope() {
    return packageScope;
  }
}
