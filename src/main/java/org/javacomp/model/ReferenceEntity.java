package org.javacomp.model;

import com.google.common.collect.Range;
import java.util.List;

/** Represents a name referencing a entity. */
public class ReferenceEntity extends Entity {
  private Entity reference; // The entity it references to.

  public ReferenceEntity(String simpleName, List<String> qualifiers) {
    super(simpleName, Entity.Kind.REFERENCE, qualifiers, Range.closedOpen(0, 0));
    this.reference = null;
  }

  /**
   * @return If reference is resolved, return the child scope of the referenced entity. Otherwise
   *     return a {@Link EmptyScope}.
   */
  @Override
  public EntityScope getChildScope() {
    if (reference == null) {
      return EmptyScope.INSTANCE;
    } else {
      return reference.getChildScope();
    }
  }
}
