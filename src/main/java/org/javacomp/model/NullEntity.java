package org.javacomp.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

/** Represents null. */
public class NullEntity extends Entity {
  public static final NullEntity INSTANCE = new NullEntity();

  private NullEntity() {
    super(
        "null",
        Entity.Kind.NULL,
        ImmutableList.<String>of() /* qualifiers */,
        true /* isStatic */,
        Range.closedOpen(0, 0));
  }

  @Override
  public EmptyScope getChildScope() {
    return EmptyScope.INSTANCE;
  }

  @Override
  public String toString() {
    return "NullEntity";
  }
}
