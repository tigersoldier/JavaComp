package org.javacomp.server.handler;

import org.javacomp.protocol.NullParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "exit" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#exit-notification
 */
public class ExitHandler extends NotificationHandler<NullParams> {
  private final Server server;

  public ExitHandler(Server server) {
    super("exit", NullParams.class);
    this.server = server;
  }

  @Override
  protected void handleNotification(Request<NullParams> request) {
    server.exit();
  }
}
