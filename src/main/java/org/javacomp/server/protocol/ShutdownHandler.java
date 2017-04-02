package org.javacomp.server.protocol;

import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "shutdown" method.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#shutdown-request
 */
public class ShutdownHandler extends RequestHandler<NullParams> {
  private final Server server;

  public ShutdownHandler(Server server) {
    super("shutdown", NullParams.class);
    this.server = server;
  }

  @Override
  public Void handleRequest(Request<NullParams> request) {
    server.shutdown();
    return null;
  }
}
