package org.javacomp.server.protocol;

import java.net.URI;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/didClose" notification.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didclosetextdocument-notification
 */
public class DidCloseTextDocumentHandler extends NotificationHandler<DidCloseTextDocumentParams> {
  private final Server server;

  public DidCloseTextDocumentHandler(Server server) {
    super("textDocument/didClose", DidCloseTextDocumentParams.class);
    this.server = server;
  }

  @Override
  protected void handleNotification(Request<DidCloseTextDocumentParams> request) throws Exception {
    URI fileUri = request.getParams().textDocument.uri;
    server.getFileManager().closeFileForSnapshot(fileUri);
  }
}
