package org.javacomp.model;

import com.google.common.collect.Range;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.util.QualifiedNames;

/** Represents a package. */
public class PackageEntity extends Entity {
  private final PackageScope packageScope;

  public PackageEntity(String simpleName, List<String> qualifiers, PackageScope packageScope) {
    super(
        simpleName,
        Entity.Kind.QUALIFIER,
        qualifiers,
        true /* isStatic */,
        Optional.empty() /* javadoc */,
        Range.closedOpen(0, 0));
    this.packageScope = packageScope;
  }

  @Override
  public PackageScope getScope() {
    return packageScope;
  }

  @Override
  public String toString() {
    return "PackageEntity<"
        + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
        + ">";
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.empty();
  }
}
