package org.javacomp.protocol;

/**
 * Parameters for "textDocument/didClose" notification.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didclosetextdocument-notification
 */
public class DidCloseTextDocumentParams implements RequestParams {
  /** The document that was closed. */
  public TextDocumentIdentifier textDocument;
}
