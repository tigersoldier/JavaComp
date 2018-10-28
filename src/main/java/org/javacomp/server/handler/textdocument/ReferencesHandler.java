package org.javacomp.server.handler.textdocument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.nio.file.Paths;
import java.util.List;
import org.javacomp.logging.JLogger;
import org.javacomp.model.FileScope;
import org.javacomp.protocol.Location;
import org.javacomp.protocol.ReferenceParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.RequestHandler;
import org.javacomp.server.handler.utils.MessageUtils;

/**
 * Handles "textDocument/references" notification.
 *
 * <p>See https://microsoft.github.io/language-server-protocol/specification#textDocument_references
 */
public class ReferencesHandler extends RequestHandler<ReferenceParams> {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final Server server;

  public ReferencesHandler(Server server) {
    super("textDocument/references", ReferenceParams.class);
    this.server = server;
  }

  @Override
  public List<Location> handleRequest(Request<ReferenceParams> request) throws Exception {
    Multimap<FileScope, Range<Integer>> symbols =
        server
            .getProject()
            .findReferencesAtPosition(
                Paths.get(request.getParams().textDocument.uri),
                request.getParams().position.getLine(),
                request.getParams().position.getCharacter());
    return symbols
        .entries()
        .stream()
        .map(
            entry -> {
              FileScope fileScope = entry.getKey();
              Range<Integer> range = entry.getValue();
              return MessageUtils.buildLocationForFile(fileScope, range);
            })
        .collect(ImmutableList.toImmutableList());
  }
}
