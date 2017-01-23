package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a package. */
public class PackageEntity extends Entity {
  private final PackageIndex packageIndex;

  public PackageEntity(
      String simpleName, List<String> qualifiers, PackageIndex packageIndex) {
    super(simpleName, Entity.Kind.QUALIFIER, qualifiers);
    this.packageIndex = packageIndex;
  }

  @Override
  public PackageIndex getChildIndex() {
    return packageIndex;
  }
}
