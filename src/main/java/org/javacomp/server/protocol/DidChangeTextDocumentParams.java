package org.javacomp.server.protocol;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Parameters for "textDocument/didChange" notification.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didchangetextdocument-notification
 */
public class DidChangeTextDocumentParams implements RequestParams {
  /**
   * The document that did change. The version number points to the version after all provided
   * content changes have been applied.
   */
  public VersionedTextDocumentIdentifier textDocument;

  /** The actual content changes. */
  public List<TextDocumentContentChangeEvent> contentChanges;

  /**
   * An event describing a change to a text document. If range and rangeLength are omitted the new
   * text is considered to be the full content of the document.
   */
  public static class TextDocumentContentChangeEvent {
    /** The range of the document that changed. */
    @Nullable public Range range;

    /** The length of the range that got replaced. */
    @Nullable public Integer rangeLength;

    /** The new text of the range/document. */
    public String text;
  }
}
