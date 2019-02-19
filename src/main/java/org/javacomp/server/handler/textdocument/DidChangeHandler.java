package org.javacomp.server.handler.textdocument;

import com.google.common.flogger.FluentLogger;
import java.net.URI;
import java.util.Optional;
import org.javacomp.file.FileManager;
import org.javacomp.protocol.textdocument.DidChangeTextDocumentParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.NotificationHandler;

/**
 * Handles "textDocument/didChange" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#didchangetextdocument-notification
 */
public class DidChangeHandler extends NotificationHandler<DidChangeTextDocumentParams> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Server server;

  public DidChangeHandler(Server server) {
    super("textDocument/didChange", DidChangeTextDocumentParams.class);
    this.server = server;
  }

  @Override
  protected void handleNotification(Request<DidChangeTextDocumentParams> request) throws Exception {
    FileManager fileManager = server.getFileManager();
    URI fileUri = request.getParams().textDocument.uri;
    logger.atFine().log("Changing document: %s", fileUri);
    for (DidChangeTextDocumentParams.TextDocumentContentChangeEvent change :
        request.getParams().contentChanges) {
      logger.atFine().log("Applying change: %s", change);
      if (change.range != null) {
        fileManager.applyEditToSnapshot(
            fileUri, change.range, Optional.ofNullable(change.rangeLength), change.text);
      } else {
        fileManager.setSnaphotContent(fileUri, change.text);
      }
    }
  }
}
