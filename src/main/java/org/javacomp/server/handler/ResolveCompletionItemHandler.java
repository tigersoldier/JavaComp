package org.javacomp.server.handler;

import org.javacomp.protocol.CompletionItem;
import org.javacomp.protocol.CompletionItem.ResolveData;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "completionItem/resolve" method.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#completion-item-resolve-request
 */
public class ResolveCompletionItemHandler extends RequestHandler<CompletionItem> {
  private final Server server;

  public ResolveCompletionItemHandler(Server server) {
    super("completionItem/resolve", CompletionItem.class);
    this.server = server;
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
        default:
          throw new UnsupportedOperationException("Unsupported resolve action: " + data.action);
      }
    }

    completionItem.data = null;
    return completionItem;
  }
}
