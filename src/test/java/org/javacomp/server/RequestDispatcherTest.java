package org.javacomp.server;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import javax.annotation.Nullable;
import org.javacomp.server.protocol.NullParams;
import org.javacomp.server.protocol.RequestParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RequestDispatcherTest {
  private static final String JSON_RPC = "2.0";

  // Empty header is OK - the test request handlers don't use it here.
  private static final Map<String, String> HEADER = ImmutableMap.of();

  private final Handler1 handler1 = new Handler1();
  private final Handler2 handler2 = new Handler2();
  private final NullParamsHandler nullParamsHandler = new NullParamsHandler();
  private RequestDispatcher dispatcher;

  @Before
  public void setUpDefaultDispatcher() {
    dispatcher = createDispatcher(handler1, handler2, nullParamsHandler);
  }

  @Test
  public void testDispatchRequests() {

    // Handle request1.
    JsonObject rawParams1 = new JsonObject();
    String params1Value = "foo";
    rawParams1.add("strvalue", new JsonPrimitive(params1Value));
    Response response1 = dispatchRequest(handler1.getMethod(), "id1", rawParams1);
    Response expectedResponse1 = Response.createResponse("id1", params1Value);
    assertThat(response1).isEqualTo(expectedResponse1);

    // Handle request2.
    JsonObject rawParams2 = new JsonObject();
    int params2Value = 42;
    rawParams2.add("intvalue", new JsonPrimitive(params2Value));
    Response response2 = dispatchRequest(handler2.getMethod(), "id2", rawParams2);
    Response expectedResponse2 = Response.createResponse("id2", params2Value);
    assertThat(response2).isEqualTo(expectedResponse2);
  }

  @Test
  public void testNullParamsAndResult() {
    Response response = dispatchRequest(nullParamsHandler.getMethod(), "id3", null /* params */);
    assertThat(response.getId()).isEqualTo("id3");
    assertThat(response.getResult()).isNull();
  }

  @Test
  public void testDispatchRequest_missingMethod_returnsInvalidRequestError() {
    JsonObject params = new JsonObject();
    Response response = dispatchRequest(null /* method */, "id", params);
    assertErrorResponse(response, ErrorCode.INVALID_REQUEST, "method");
  }

  @Test
  public void testDispatchRequest_missingId_returnsInvalidRequestError() {
    JsonObject params = new JsonObject();
    Response response = dispatchRequest("cmd1", null /* id */, params);
    assertErrorResponse(response, ErrorCode.INVALID_REQUEST, "ID");
  }

  @Test
  public void testDispatchRequest_missingParamsForNonNullHandler_returnsInvalidRequestError() {
    Response response = dispatchRequest(handler1.getMethod(), "id", null /* params */);
    assertErrorResponse(response, ErrorCode.INVALID_REQUEST, "params");
  }

  @Test
  public void testDispatchRequest_methodNotExist_returnsMethodNotFoundError() {
    JsonObject params = new JsonObject();
    Response response = dispatchRequest("invalidmethod", "id", params);
    assertErrorResponse(response, ErrorCode.METHOD_NOT_FOUND, "invalidmethod");
  }

  @Test
  public void testHandlerThrowsException_returnsInternalError() {
    ErrorThrowingHandler handler = new ErrorThrowingHandler(new Exception("internal error"));
    dispatcher = createDispatcher(handler);
    Response response = dispatchRequest(handler.getMethod(), "id", null /* params */);
    assertErrorResponse(response, ErrorCode.INTERNAL_ERROR, "internal error");
  }

  @Test
  public void testHandlerThrowsRequestException_returnsErrorWithErrorCode() {
    ErrorThrowingHandler handler =
        new ErrorThrowingHandler(
            new RequestException(ErrorCode.SERVER_NOT_INITIALIZED, "custom error"));
    dispatcher = createDispatcher(handler);
    Response response = dispatchRequest(handler.getMethod(), "id", null /* params */);
    assertErrorResponse(response, ErrorCode.SERVER_NOT_INITIALIZED, "custom error");
  }

  private static RequestDispatcher createDispatcher(RequestHandler<?>... handlers) {
    Gson gson = new Gson();
    RequestDispatcher.Builder builder = new RequestDispatcher.Builder(gson);
    for (RequestHandler handler : handlers) {
      builder.registerHandler(handler);
    }
    return builder.build();
  }

  private Response dispatchRequest(String method, String id, @Nullable JsonObject params) {
    RawRequest rawRequest =
        RawRequest.create(HEADER, new RawRequest.Content(method, id, JSON_RPC, params));
    return dispatcher.dispatchRequest(rawRequest);
  }

  private void assertErrorResponse(Response response, ErrorCode errorCode, String message) {
    assertThat(response.getError()).isNotNull();
    assertThat(response.getError().getCode()).isEqualTo(errorCode.getCode());
    assertThat(response.getError().getMessage()).contains(message);
  }

  private static class Param1 implements RequestParams {
    public String strvalue;

    public Param1() {}

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Param1)) {
        return false;
      }
      return strvalue.equals(((Param1) o).strvalue);
    }
  }

  private static class Param2 implements RequestParams {
    public int intvalue;

    public Param2() {}

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Param2)) {
        return false;
      }
      return intvalue == ((Param2) o).intvalue;
    }
  }

  private static class Handler1 extends RequestHandler<Param1> {
    private Handler1() {
      super("cmd1", Param1.class);
    }

    @Override
    public String handleRequest(Request<Param1> request) {
      return request.getParams().strvalue;
    }
  }

  private static class Handler2 extends RequestHandler<Param2> {
    private Handler2() {
      super("cmd2", Param2.class);
    }

    @Override
    public Integer handleRequest(Request<Param2> request) {
      return request.getParams().intvalue;
    }
  }

  private static class NullParamsHandler extends RequestHandler<NullParams> {
    private NullParamsHandler() {
      super("nullparams", NullParams.class);
    }

    @Override
    public Void handleRequest(Request<NullParams> request) {
      return null;
    }
  }

  private static class ErrorThrowingHandler extends RequestHandler<NullParams> {
    private final Exception toThrow;

    private ErrorThrowingHandler(Exception toThrow) {
      super(toThrow.getClass().getSimpleName(), NullParams.class);
      this.toThrow = toThrow;
    }

    @Override
    public Void handleRequest(Request<NullParams> request) throws Exception {
      throw toThrow;
    }
  }
}
