package org.javacomp.protocol.textdocument;

import javax.annotation.Nullable;
import org.javacomp.protocol.MarkupContent;
import org.javacomp.protocol.Range;

/** The result of a hover request. */
public class Hover {
  /** The hover's content. */
  public MarkupContent contents;

  /**
   * An optional range is a range inside a text document that is used to visualize a hover, e.g. by
   * changing the background color.
   */
  @Nullable public Range range;
}
