package org.javacomp.server.handler.textdocument;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.javacomp.protocol.textdocument.CompletionItem;
import org.javacomp.protocol.textdocument.CompletionItem.ResolveAddImportTextEditsParams;
import org.javacomp.protocol.textdocument.CompletionItem.ResolveData;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.RequestHandler;

/**
 * Handles "completionItem/resolve" method.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#completion-item-resolve-request
 */
public class ResolveCompletionItemHandler extends RequestHandler<CompletionItem> {
  private final Server server;
  private final Gson gson;

  public ResolveCompletionItemHandler(Server server, Gson gson) {
    super("completionItem/resolve", CompletionItem.class);
    this.server = server;
    this.gson = gson;
  }

  @Override
  public CompletionItem handleRequest(Request<CompletionItem> request) throws Exception {
    CompletionItem completionItem = request.getParams();
    if (completionItem.data == null) {
      // Nothing to resolve.
      return completionItem;
    }

    for (ResolveData data : completionItem.data) {
      switch (data.action) {
        case ADD_IMPORT_TEXT_EDIT:
          resolveImportClass(completionItem, data.params);
          break;
        default:
          throw new UnsupportedOperationException("Unsupported resolve action: " + data.action);
      }
    }

    completionItem.data = null;
    return completionItem;
  }

  private void resolveImportClass(CompletionItem completionItem, JsonElement jsonParams) {
    if (completionItem.additionalTextEdits == null) {
      completionItem.additionalTextEdits = new ArrayList<>();
    }
    ResolveAddImportTextEditsParams params =
        gson.fromJson(jsonParams, ResolveAddImportTextEditsParams.class);
    completionItem.additionalTextEdits.add(
        server.getProject().textEditForImport(Paths.get(params.uri), params.classFullName));
  }
}
