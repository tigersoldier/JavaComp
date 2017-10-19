package org.javacomp.protocol;

import java.util.List;
import javax.annotation.Nullable;

/** The result of a hover request. */
public class Hover {
  /** The hover's content. */
  public List<MarkedString> contents;

  /**
   * An optional range is a range inside a text document that is used to visualize a hover, e.g. by
   * changing the background color.
   */
  @Nullable public Range range;

  /**
   * Marker interface for rendering human readable text. It is either a markdown string or a
   * code-block that provides a language and a code snippet.
   *
   * <p>Note that markdown strings will be sanitized - that means html will be escaped.
   */
  public interface MarkedString {}

  /**
   * A code-block that provides a language and a code snippet.
   *
   * <p>The pair of a language and a value is an equivalent to markdown: ```${language} ${value} ```
   */
  public static class LanguageString implements MarkedString {
    /**
     * The language identifier is sematically equal to the optional language identifier in fenced
     * code blocks in GitHub issues. See
     * https://help.github.com/articles/creating-and-highlighting-code-blocks/#syntax-highlighting
     */
    public String language;

    public String value;
  }
}
