package org.javacomp.server.protocol;

/**
 * A parameter literal used in requests to pass a text document and a position inside that document.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textdocumentpositionparams
 */
public class TextDocumentPositionParams implements RequestParams {
  /** The text document. */
  public TextDocumentIdentifier textDocument;

  /** The position inside the text document. */
  public Position position;
}
