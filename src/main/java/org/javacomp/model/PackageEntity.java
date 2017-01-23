package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a package. */
public class PackageEntity extends Entity {
  private final PackageScope packageScope;

  public PackageEntity(
      String simpleName, List<String> qualifiers, PackageScope packageScope) {
    super(simpleName, Entity.Kind.QUALIFIER, qualifiers);
    this.packageScope = packageScope;
  }

  @Override
  public PackageScope getChildScope() {
    return packageScope;
  }
}
