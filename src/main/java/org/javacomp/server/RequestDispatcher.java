package org.javacomp.server;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import javax.annotation.Nullable;
import org.javacomp.server.protocol.NullParams;
import org.javacomp.server.protocol.RequestParams;

/**
 * Logic for dispatching requests to registered {@link RequestHandler} instances based on method
 * name.
 */
public class RequestDispatcher {
  private final Gson gson;
  private final ImmutableMap<String, RequestHandler> handlerRegistry;

  private RequestDispatcher(Gson gson, ImmutableMap<String, RequestHandler> handlerRegistry) {
    this.gson = gson;
    this.handlerRegistry = handlerRegistry;
  }

  /**
   * Dispatches a {@link RawRequest} to the {@link RequestHandler} registered for the method of the
   * request.
   */
  public Response dispatchRequest(RawRequest rawRequest) {
    RawRequest.Content requestContent = rawRequest.getContent();
    String requestId = requestContent.getId();
    Response.ResponseError responseError = responseIfContentIsInvalid(rawRequest.getContent());
    if (responseError != null) {
      return Response.createError(requestId, responseError);
    }

    if (!handlerRegistry.containsKey(requestContent.getMethod())) {
      return Response.createError(
          requestId,
          ErrorCode.METHOD_NOT_FOUND,
          "Cannot find method %s",
          requestContent.getMethod());
    }

    RequestHandler handler = handlerRegistry.get(requestContent.getMethod());
    try {
      Request typedRequest = convertRawToRequest(rawRequest, handler);
      @SuppressWarnings("unchecked")
      Object result = handler.handleRequest(typedRequest);
      return Response.createResponse(requestId, result);
    } catch (RequestException e) {
      return Response.createError(requestId, e.getErrorCode(), e.getMessage());
    } catch (Exception e) {
      return Response.createError(requestId, ErrorCode.INTERNAL_ERROR, e.getMessage());
    }
  }

  /** Returns an error response if the required fields are missing in the request content. */
  @Nullable
  private Response.ResponseError responseIfContentIsInvalid(RawRequest.Content content) {
    if (Strings.isNullOrEmpty(content.getId())) {
      return new Response.ResponseError(ErrorCode.INVALID_REQUEST, "Missing request ID.");
    }

    if (Strings.isNullOrEmpty(content.getMethod())) {
      return new Response.ResponseError(ErrorCode.INVALID_REQUEST, "Missing request method.");
    }

    // We don't check the jsonrpc field because it's useless.
    return null;
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
    private final Gson gson;
    private final ImmutableMap.Builder<String, RequestHandler> registryBuilder;

    public Builder(Gson gson) {
      this.gson = gson;
      this.registryBuilder = new ImmutableMap.Builder<>();
    }

    /** Registers a {@link RequestHandler} to the {@link RequestDispatcher} to be built. */
    public Builder registerHandler(RequestHandler handler) {
      registryBuilder.put(handler.getMethod(), handler);
      return this;
    }

    public RequestDispatcher build() {
      return new RequestDispatcher(gson, registryBuilder.build());
    }
  }
}
