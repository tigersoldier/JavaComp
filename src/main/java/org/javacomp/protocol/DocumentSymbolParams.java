package org.javacomp.protocol;

/**
 * A parameter literal used in requests to pass a text document.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textdocumentpositionparams
 */
public class DocumentSymbolParams implements RequestParams {
  /** The text document. */
  public TextDocumentIdentifier textDocument;
}
