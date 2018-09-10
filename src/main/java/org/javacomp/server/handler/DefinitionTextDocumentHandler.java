package org.javacomp.server.handler;

import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.project.Project;
import org.javacomp.protocol.Location;
import org.javacomp.protocol.TextDocumentPositionParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/definition" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textDocument_definition
 */
public class DefinitionTextDocumentHandler extends RequestHandler<TextDocumentPositionParams> {
  private final Server server;

  public DefinitionTextDocumentHandler(Server server) {
    super("textDocument/definition", TextDocumentPositionParams.class);
    this.server = server;
  }

  @Override
  public List<Location> handleRequest(Request<TextDocumentPositionParams> request)
      throws Exception {
    TextDocumentPositionParams params = request.getParams();
    Project project = server.getProject();
    List<? extends Entity> definitions =
        project.findDefinitions(
            Paths.get(params.textDocument.uri),
            params.position.getLine(),
            params.position.getCharacter());

    return definitions
        .stream()
        .map(
            entity -> {
              com.google.common.collect.Range<Integer> range = entity.getSymbolRange();
              EntityScope scope = entity.getScope();
              while (!(scope instanceof FileScope) && scope.getParentScope().isPresent()) {
                scope = scope.getParentScope().get();
              }

              if (!(scope instanceof FileScope)) {
                throw new RuntimeException("Cannot reach file scope for " + entity);
              }

              FileScope fileScope = (FileScope) scope;
              if (fileScope.getFileType() != FileScope.FileType.SOURCE_CODE) {
                // If the file scope is not created from a source code (e.g. it's created from
                // a type index JSON file or class file), there is no souce code that defines the
                // symbol.
                return null;
              }
              if (!fileScope.getLineMap().isPresent()) {
                return null;
              }

              return MessageUtils.buildLocationForFile(fileScope, range);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableList.toImmutableList());
  }
}
