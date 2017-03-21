package org.javacomp.server;

import org.javacomp.server.protocol.NullParams;
import org.javacomp.server.protocol.RequestParams;

/**
 * Logic for handling incoming request for a particular request method.
 *
 * @param <PARAM> the type of the request param name. If the request doesn't have any parameter, the
 *     type must be {@link NullParams}.
 */
public abstract class RequestHandler<PARAM extends RequestParams> {
  private final String method;
  private final Class<PARAM> paramsType;

  /**
   * @param method the request method name this handler can process
   * @param paramsType the type of the request parameters this handler expects
   */
  protected RequestHandler(String method, Class<PARAM> paramsType) {
    this.method = method;
    this.paramsType = paramsType;
  }

  /** Gets the request method name this handler can handle. */
  public String getMethod() {
    return this.method;
  }

  /** Gets the expected request parameter type. */
  public Class<PARAM> getParamsType() {
    return this.paramsType;
  }

  /**
   * Actual logic of handling a request. To be implemented by sub-classes.
   *
   * @param request the request to be handled
   * @return the result of the request. If non-null, must be able to be converted to JSON by Gson
   * @throws RequestException if any error happens with a known ErrorCode
   * @throws Exceptoin if any error happens without a specific ErrorCode
   */
  public abstract Object handleRequest(Request<PARAM> request) throws RequestException, Exception;

  @Override
  public String toString() {
    return String.format("RequestHandler (%s): %s", paramsType.getSimpleName(), method);
  }
}
