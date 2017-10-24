package org.javacomp.server.handler;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.project.Project;
import org.javacomp.protocol.ClientCapabilities;
import org.javacomp.protocol.CompletionList;
import org.javacomp.protocol.CompletionList.CompletionItem;
import org.javacomp.protocol.CompletionList.CompletionItemKind;
import org.javacomp.protocol.CompletionList.InsertTextFormat;
import org.javacomp.protocol.TextDocumentPositionParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/completion" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#completion-request
 */
public class CompletionTextDocumentHandler extends RequestHandler<TextDocumentPositionParams> {
  private final Server server;

  public CompletionTextDocumentHandler(Server server) {
    super("textDocument/completion", TextDocumentPositionParams.class);
    this.server = server;
  }

  @Override
  public CompletionList handleRequest(Request<TextDocumentPositionParams> request)
      throws Exception {
    TextDocumentPositionParams params = request.getParams();
    Project project = server.getProject();
    List<CompletionCandidate> candidates =
        project.getCompletionCandidates(
            Paths.get(params.textDocument.uri),
            params.position.getLine(),
            params.position.getCharacter());

    CompletionList completionList = new CompletionList();
    completionList.isIncomplete = false;
    completionList.items = new ArrayList<>();

    for (CompletionCandidate candidate : candidates) {
      CompletionItem item = new CompletionItem();
      item.label = candidate.getName();
      item.kind = getCompletionItemKind(candidate.getKind());
      Optional<String> detail = candidate.getDetail();
      if (detail.isPresent()) {
        item.detail = detail.get();
      }

      // The sort text of a candidate is the candidate label prefixed with its
      // sort category ordinal.
      char sortPrefix = (char) ('a' + candidate.getSortCategory().ordinal());
      item.sortText = sortPrefix + item.label;

      boolean supportsSnippet = clientSupportsSnippet(server.getClientCapabilities());
      item.insertTextFormat =
          supportsSnippet ? InsertTextFormat.SNIPPET : InsertTextFormat.PLAINTEXT;
      Optional<String> insertText = Optional.empty();
      if (supportsSnippet) {
        insertText = candidate.getInsertSnippet();
      }
      if (!insertText.isPresent()) {
        // Either the client doesn't snippet, or the candidate doesn't have a snippet.
        insertText = candidate.getInsertPlainText();
      }
      item.insertText = insertText.orElse(null);
      completionList.items.add(item);
    }
    return completionList;
  }

  private static boolean clientSupportsSnippet(ClientCapabilities clientCapabilities) {
    if (clientCapabilities.textDocument == null
        || clientCapabilities.textDocument.completion == null
        || clientCapabilities.textDocument.completion.completionItem == null) {
      return false;
    }
    return clientCapabilities.textDocument.completion.completionItem.snippetSupport;
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
