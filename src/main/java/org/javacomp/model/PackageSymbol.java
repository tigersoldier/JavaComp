package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a package. */
public class PackageSymbol extends Symbol {
  private final PackageIndex packageIndex;

  public PackageSymbol(
      String simpleName, List<String> qualifiers, PackageIndex packageIndex) {
    super(simpleName, Symbol.Kind.QUALIFIER, qualifiers);
    this.packageIndex = packageIndex;
  }

  @Override
  public PackageIndex getChildIndex() {
    return packageIndex;
  }
}
