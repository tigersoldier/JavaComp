package org.javacomp.server.protocol;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.file.FileTextLocation;
import org.javacomp.project.Project;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.protocol.CompletionList.CompletionItemKind;

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
    List<FileTextLocation> definitions =
        project.findDefinitions(
            Paths.get(params.textDocument.uri),
            params.position.getLine(),
            params.position.getCharacter());

    return definitions
        .stream()
        .map(
            loc -> {
              Location location = new Location();
              location.uri = loc.getFilePath().toUri();
              location.range = Range.createFromTextRange(loc.getRange());
              return location;
            })
        .collect(Collectors.toList());
  }

  private static CompletionItemKind getCompletionItemKind(CompletionCandidate.Kind candidateKind) {
    switch (candidateKind) {
      case CLASS:
        return CompletionItemKind.CLASS;
      case INTERFACE:
        return CompletionItemKind.INTERFACE;
      case ENUM:
        return CompletionItemKind.ENUM;
      case METHOD:
        return CompletionItemKind.METHOD;
      case VARIABLE:
        return CompletionItemKind.VARIABLE;
      case FIELD:
        return CompletionItemKind.FIELD;
      case PACKAGE:
        return CompletionItemKind.MODULE;
      case KEYWORD:
        return CompletionItemKind.KEYWORD;
      default:
        return CompletionItemKind.UNKNOWN;
    }
  }
}
