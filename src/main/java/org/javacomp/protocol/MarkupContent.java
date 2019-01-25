package org.javacomp.protocol;

import org.javacomp.server.GsonEnum;

/**
 * Represents a string value which content is interpreted base on its kind flag.
 *
 * <p>If the {@code kind} is {@link MarkupKind#MARKDOWN} then the value can contain fenced code
 * blocks like in GitHub issues. See
 * https://help.github.com/articles/creating-and-highlighting-code-blocks/#syntax-highlighting
 *
 * <p><b>Please Note</b> that clients might sanitize the return markdown. A client could decide to
 * remove HTML from the markdown to avoid script execution.
 */
public class MarkupContent {
  /** The type of the Markup */
  public MarkupKind kind;

  /** The content itself */
  public String value;

  // For GSON
  MarkupContent() {}

  private MarkupContent(MarkupKind kind, String value) {
    this.kind = kind;
    this.value = value;
  }

  public static MarkupContent plainText(String value) {
    return new MarkupContent(MarkupKind.PLAINTEXT, value);
  }

  public static MarkupContent markdown(String value) {
    return new MarkupContent(MarkupKind.MARKDOWN, value);
  }

  /**
   * Describes the content type that a client supports in various result literals like {@code
   * Hover}, {@code ParameterInfo} or {@code CompletionItem}.
   */
  @GsonEnum(GsonEnum.SerializeType.LOWERCASE_NAME)
  public enum MarkupKind {
    /** Plain text is supported as a content format */
    PLAINTEXT,
    /** Markdown is supported as a content format */
    MARKDOWN,
  }
}
