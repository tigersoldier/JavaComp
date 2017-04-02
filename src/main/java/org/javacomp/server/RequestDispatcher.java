package org.javacomp.server;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.javacomp.logging.JLogger;
import org.javacomp.server.io.ResponseWriter;
import org.javacomp.server.io.StreamClosedException;
import org.javacomp.server.protocol.NullParams;
import org.javacomp.server.protocol.RequestHandler;
import org.javacomp.server.protocol.RequestParams;

/**
 * Logic for dispatching requests to registered {@link RequestHandler} instances based on method
 * name.
 */
public class RequestDispatcher {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final Gson gson;
  private final ImmutableMap<String, RequestHandler> handlerRegistry;
  private final RequestParser requestParser;
  private final ResponseWriter responseWriter;

  private RequestDispatcher(Builder builder, ImmutableMap<String, RequestHandler> handlerRegistry) {
    this.gson = checkNotNull(builder.gson, "gson");
    this.requestParser = checkNotNull(builder.requestParser, "requestParser");
    this.responseWriter = checkNotNull(builder.responseWriter, "responseWriter");
    this.handlerRegistry = checkNotNull(handlerRegistry, "handlerRegistry");
  }

  /**
   * Reads a requset, dispatches it to the registered handler and writes response to client.
   *
   * @return whether the dispatcher can dispatch more requests. If it's false, the server should be
   *     shutdown and exit
   */
  public boolean dispatchRequest() {
    RawRequest rawRequest;
    try {
      rawRequest = requestParser.parse();
    } catch (RequestException e) {
      logger.severe(e, "Malformed request received and unable to recover. Shutting down.");
      return false;
    } catch (StreamClosedException e) {
      logger.severe(e, "Input stream closed, shutting down server.");
      return false;
    }

    Object result = null;
    Response.ResponseError error = null;
    try {
      result = dispatchRequestInternal(rawRequest);
    } catch (RequestException e) {
      logger.severe(e, "Failed to process request.");
      error = new Response.ResponseError(e.getErrorCode(), e.getMessage());
    } catch (Throwable e) {
      logger.severe(e, "Failed to process request.");
      error = new Response.ResponseError(ErrorCode.INTERNAL_ERROR, e.getMessage());
    }

    String requestId = rawRequest.getContent().getId();
    if (Strings.isNullOrEmpty(requestId)) {
      // No ID provided. The request is a notification and the client doesn't expect any response.
      return true;
    }

    Response response;
    if (error != null) {
      response = Response.createError(requestId, error);
    } else {
      response = Response.createResponse(requestId, result);
    }
    try {
      responseWriter.writeResponse(response);
    } catch (Throwable e) {
      logger.severe(e, "Failed to write response, shutting down server.");
      return false;
    }

    return true;
  }

  /**
   * Dispatches a {@link RawRequest} to the {@link RequestHandler} registered for the method of the
   * request.
   *
   * @return the result of processing the request
   */
  private Object dispatchRequestInternal(RawRequest rawRequest) throws RequestException, Exception {
    RawRequest.Content requestContent = rawRequest.getContent();

    if (Strings.isNullOrEmpty(requestContent.getMethod())) {
      throw new RequestException(ErrorCode.INVALID_REQUEST, "Missing request method.");
    }

    if (!handlerRegistry.containsKey(requestContent.getMethod())) {
      throw new RequestException(
          ErrorCode.METHOD_NOT_FOUND, "Cannot find method %s", requestContent.getMethod());
    }

    RequestHandler handler = handlerRegistry.get(requestContent.getMethod());

    Request typedRequest = convertRawToRequest(rawRequest, handler);
    @SuppressWarnings("unchecked")
    Object result = handler.handleRequest(typedRequest);
    return result;
  }

  @SuppressWarnings("unchecked")
  private Request convertRawToRequest(RawRequest rawRequest, RequestHandler handler)
      throws RequestException {
    RequestParams requestParams;
    RawRequest.Content requestContent = rawRequest.getContent();
    Class<?> parameterType = handler.getParamsType();
    if (parameterType == NullParams.class) {
      requestParams = null;
    } else {
      if (requestContent.getParams() == null) {
        throw new RequestException(ErrorCode.INVALID_REQUEST, "Missing request params.");
      }
      // We don't know the type here, but it's OK since GSON has the type.
      requestParams = (RequestParams) gson.fromJson(requestContent.getParams(), parameterType);
    }

    return Request.create(
        rawRequest.getHeader(), requestContent.getMethod(), requestContent.getId(), requestParams);
  }

  /** Builder for {@link RequestDispatcher}. */
  public static class Builder {
    private Gson gson;
    private ImmutableMap.Builder<String, RequestHandler> registryBuilder;
    private RequestParser requestParser;
    private ResponseWriter responseWriter;

    public Builder() {
      this.registryBuilder = new ImmutableMap.Builder<>();
    }

    public Builder setGson(Gson gson) {
      this.gson = gson;
      return this;
    }

    public Builder setRequestParser(RequestParser requestParser) {
      this.requestParser = requestParser;
      return this;
    }

    public Builder setResponseWriter(ResponseWriter responseWriter) {
      this.responseWriter = responseWriter;
      return this;
    }

    /** Registers a {@link RequestHandler} to the {@link RequestDispatcher} to be built. */
    public Builder registerHandler(RequestHandler handler) {
      registryBuilder.put(handler.getMethod(), handler);
      return this;
    }

    public RequestDispatcher build() {
      return new RequestDispatcher(this, registryBuilder.build());
    }
  }
}
