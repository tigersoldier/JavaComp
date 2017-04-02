package org.javacomp.server.protocol;

import org.javacomp.server.Request;
import org.javacomp.server.RequestException;

/**
 * Base class for all notification handlers.
 *
 * <p>Subclasses should implement {@link #handleNotification} instead of {@link #handleRequest}.
 */
public abstract class NotificationHandler<PARAM extends RequestParams>
    extends RequestHandler<PARAM> {
  protected NotificationHandler(String method, Class<PARAM> paramType) {
    super(method, paramType);
  }

  @Override
  public final Void handleRequest(Request<PARAM> request) throws RequestException, Exception {
    handleNotification(request);
    return null;
  }

  protected abstract void handleNotification(Request<PARAM> request)
      throws RequestException, Exception;

  @Override
  public final boolean isNotification() {
    return true;
  }

  @Override
  public String toString() {
    return String.format(
        "NotificationHandler (%s): %s", getParamsType().getSimpleName(), getMethod());
  }
}
