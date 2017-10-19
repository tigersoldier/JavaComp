package org.javacomp.server.handler;

import java.net.URI;
import java.util.Optional;
import org.javacomp.file.FileManager;
import org.javacomp.logging.JLogger;
import org.javacomp.protocol.DidChangeTextDocumentParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/didChange" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didchangetextdocument-notification
 */
public class DidChangeTextDocumentHandler extends NotificationHandler<DidChangeTextDocumentParams> {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final Server server;

  public DidChangeTextDocumentHandler(Server server) {
    super("textDocument/didChange", DidChangeTextDocumentParams.class);
    this.server = server;
  }

  @Override
  protected void handleNotification(Request<DidChangeTextDocumentParams> request) throws Exception {
    FileManager fileManager = server.getFileManager();
    URI fileUri = request.getParams().textDocument.uri;
    logger.fine("Changing document: %s", fileUri);
    for (DidChangeTextDocumentParams.TextDocumentContentChangeEvent change :
        request.getParams().contentChanges) {
      logger.fine("Applying change: %s", change);
      if (change.range != null) {
        fileManager.applyEditToSnapshot(
            fileUri, change.range, Optional.ofNullable(change.rangeLength), change.text);
      } else {
        fileManager.setSnaphotContent(fileUri, change.text);
      }
    }
  }
}
