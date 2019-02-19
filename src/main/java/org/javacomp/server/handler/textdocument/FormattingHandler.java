package org.javacomp.server.handler.textdocument;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.java.Formatter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.javacomp.logging.JLogger;
import org.javacomp.protocol.Position;
import org.javacomp.protocol.Range;
import org.javacomp.protocol.TextEdit;
import org.javacomp.protocol.textdocument.DocumentFormattingParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.RequestHandler;

/**
 * Handles "textDocument/formatting" request.
 *
 * <p>See https://microsoft.github.io/language-server-protocol/specification#textDocument_formatting
 */
public class FormattingHandler extends RequestHandler<DocumentFormattingParams> {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');

  private final Server server;

  public FormattingHandler(Server server) {
    super("textDocument/formatting", DocumentFormattingParams.class);
    this.server = server;
  }

  @Override
  public ImmutableList<TextEdit> handleRequest(Request<DocumentFormattingParams> request)
      throws Exception {
    Optional<CharSequence> content =
        server.getFileManager().getFileContent(Paths.get(request.getParams().textDocument.uri));
    if (!content.isPresent()) {
      logger.warning("Cannot find file content for %s", request.getParams().textDocument.uri);
      return ImmutableList.of();
    }
    if (content.get().length() == 0) {
      return ImmutableList.of();
    }
    String newContent = new Formatter().formatSourceAndFixImports(content.get().toString());
    List<String> lines = LINE_SPLITTER.splitToList(content.get());
    int endLine = lines.size() - 1;
    int endColumn = lines.get(endLine).length();
    Range range = new Range(new Position(0, 0), new Position(endLine, endColumn));
    return ImmutableList.of(new TextEdit(range, newContent));
  }
}
