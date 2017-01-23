package org.javacomp.model;

import java.util.List;

/** Represents a name referencing a entity. */
public class ReferenceEntity extends Entity {
  private Entity reference; // The entity it references to.

  public ReferenceEntity(String simpleName, List<String> qualifiers) {
    super(simpleName, Entity.Kind.REFERENCE, qualifiers);
    this.reference = null;
  }

  /**
   * @return If reference is resolved, return the child index of the referenced entity. Otherwise
   *     return a {@Link LeafIndex}.
   */
  @Override
  public EntityIndex getChildIndex() {
    if (reference == null) {
      return LeafIndex.INSTANCE;
    } else {
      return reference.getChildIndex();
    }
  }
}
