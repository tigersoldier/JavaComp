package org.javacomp.protocol.textdocument;

import java.net.URI;
import org.javacomp.protocol.RequestParams;

/**
 * Parameters for "textDocument/didOpen" notification.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didopentextdocument-notification
 */
public class DidOpenTextDocumentParams implements RequestParams {
  public TextDocumentItem textDocument;

  /** An item to transfer a text document from the client to the server. */
  public static class TextDocumentItem {

    /** The text document's URI. */
    public URI uri;

    /** The text document's language identifier. */
    public String languageId;

    /**
     * The version number of this document (it will strictly increase after each change, including
     * undo/redo).
     */
    public int version;

    /** The content of the opened text document. */
    public String text;
  }
}
