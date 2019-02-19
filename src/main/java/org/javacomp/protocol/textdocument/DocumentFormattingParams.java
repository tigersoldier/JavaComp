package org.javacomp.protocol.textdocument;

import org.javacomp.protocol.RequestParams;
import org.javacomp.protocol.TextDocumentIdentifier;

/**
 * Parameters for "textDocument/format" request.
 *
 * <p>See https://microsoft.github.io/language-server-protocol/specification#textDocument_formatting
 */
public class DocumentFormattingParams implements RequestParams {
  /** The document to format. */
  public TextDocumentIdentifier textDocument;

  /** The format options. */
  public FormattingOptions options;

  /** Value-object describing what options formatting should use. */
  public class FormattingOptions {
    /** Size of a tab in spaces. */
    public int tabSize;

    /** Prefer spaces over tabs. */
    public boolean insertSpaces;
  }
}
