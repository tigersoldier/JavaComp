package org.javacomp.server.handler.textdocument;

import org.javacomp.protocol.textdocument.DidOpenTextDocumentParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.NotificationHandler;

/**
 * Handles "textDocument/didOpen" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didopentextdocument-notification
 */
public class DidOpenHandler extends NotificationHandler<DidOpenTextDocumentParams> {
  private final Server server;

  public DidOpenHandler(Server server) {
    super("textDocument/didOpen", DidOpenTextDocumentParams.class);
    this.server = server;
  }

  @Override
  protected void handleNotification(Request<DidOpenTextDocumentParams> request) throws Exception {
    DidOpenTextDocumentParams.TextDocumentItem textDocument = request.getParams().textDocument;
    server.getFileManager().openFileForSnapshot(textDocument.uri, textDocument.text);
  }
}
