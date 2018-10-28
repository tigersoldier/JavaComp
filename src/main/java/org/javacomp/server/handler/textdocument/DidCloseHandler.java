package org.javacomp.server.handler.textdocument;

import java.net.URI;
import org.javacomp.protocol.textdocument.DidCloseTextDocumentParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.NotificationHandler;

/**
 * Handles "textDocument/didClose" notification.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didclosetextdocument-notification
 */
public class DidCloseHandler extends NotificationHandler<DidCloseTextDocumentParams> {
  private final Server server;

  public DidCloseHandler(Server server) {
    super("textDocument/didClose", DidCloseTextDocumentParams.class);
    this.server = server;
  }

  @Override
  protected void handleNotification(Request<DidCloseTextDocumentParams> request) throws Exception {
    URI fileUri = request.getParams().textDocument.uri;
    server.getFileManager().closeFileForSnapshot(fileUri);
  }
}
