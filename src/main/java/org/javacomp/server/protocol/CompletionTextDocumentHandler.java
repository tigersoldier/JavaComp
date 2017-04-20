package org.javacomp.server.protocol;

import java.util.ArrayList;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.protocol.CompletionList.CompletionItem;
import org.javacomp.server.protocol.CompletionList.CompletionItemKind;
import org.javacomp.server.protocol.CompletionList.InsertTextFormat;

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
    CompletionList completionList = new CompletionList();
    completionList.isIncomplete = false;
    completionList.items = new ArrayList<>();

    CompletionItem item1 = new CompletionItem();
    item1.label = "method item";
    item1.insertTextFormat = InsertTextFormat.PLAINTEXT;
    item1.kind = CompletionItemKind.METHOD;
    completionList.items.add(item1);

    CompletionItem item2 = new CompletionItem();
    item1.label = "class item";
    item1.insertTextFormat = InsertTextFormat.PLAINTEXT;
    item1.kind = CompletionItemKind.CLASS;
    completionList.items.add(item2);

    return completionList;
  }
}
