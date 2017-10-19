package org.javacomp.server;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.javacomp.protocol.NullParams;
import org.javacomp.protocol.RequestParams;
import org.javacomp.server.handler.NotificationHandler;
import org.javacomp.server.handler.RequestHandler;
import org.javacomp.server.io.ResponseWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class RequestDispatcherTest {
  @Rule public MockitoRule mrule = MockitoJUnit.rule();

  private static final String JSON_RPC = "2.0";

  // Empty header is OK - the test request handlers don't use it here.
  private static final Map<String, String> HEADER = ImmutableMap.of();

  private final Handler1 handler1 = new Handler1();
  private final Handler2 handler2 = new Handler2();
  private final NullParamsHandler nullParamsHandler = new NullParamsHandler();
  private final CountingNotificationHandler notificationHandler = new CountingNotificationHandler();
  private final TestResponseWriter responseWriter = new TestResponseWriter();

  @Mock private RequestParser requestParser;
  @Captor private ArgumentCaptor<Response> responseCaptor;

  private RequestDispatcher dispatcher;

  @Before
  public void setUpDefaultDispatcher() {
    dispatcher = createDispatcher(handler1, handler2, nullParamsHandler, notificationHandler);
  }

  @Test
  public void testDispatchRequests() {
    // Handle request1.
    JsonObject rawParams1 = new JsonObject();
    String params1Value = "foo";
    rawParams1.add("strvalue", new JsonPrimitive(params1Value));
    dispatchRequest(handler1.getMethod(), "id1", rawParams1);
    Response expectedResponse1 = Response.createResponse("id1", params1Value);
    assertResponseWritten(expectedResponse1);

    // Handle request2.
    JsonObject rawParams2 = new JsonObject();
    int params2Value = 42;
    rawParams2.add("intvalue", new JsonPrimitive(params2Value));
    dispatchRequest(handler2.getMethod(), "id2", rawParams2);
    Response expectedResponse2 = Response.createResponse("id2", params2Value);
    assertResponseWritten(expectedResponse2);
  }

  @Test
  public void testDispatchNotification_doesNotWriteResponse() {
    dispatchRequest(notificationHandler.getMethod(), null /* id */, null);
    assertThat(responseWriter.pollResponse()).isNull();
    assertThat(notificationHandler.numReceived).isEqualTo(1);
  }

  @Test
  public void testNullParamsAndResult() {
    dispatchRequest(nullParamsHandler.getMethod(), "id3", null /* params */);
    Response response = responseWriter.pollResponse();
    assertThat(response.getId()).isEqualTo("id3");
    assertThat(response.getResult()).isNull();
  }

  @Test
  public void testDispatchRequest_missingMethod_returnsInvalidRequestError() {
    JsonObject params = new JsonObject();
    dispatchRequest(null /* method */, "id", params);
    assertErrorResponseWritten(ErrorCode.INVALID_REQUEST, "id", "method");
  }

  @Test
  public void testDispatchRequest_missingParamsForNonNullHandler_returnsInvalidRequestError() {
    dispatchRequest(handler1.getMethod(), "id", null /* params */);
    assertErrorResponseWritten(ErrorCode.INVALID_REQUEST, "id", "params");
  }

  @Test
  public void testDispatchRequest_methodNotExist_returnsMethodNotFoundError() {
    JsonObject params = new JsonObject();
    dispatchRequest("invalidmethod", "id", params);
    assertErrorResponseWritten(ErrorCode.METHOD_NOT_FOUND, "id", "invalidmethod");
  }

  @Test
  public void testHandlerThrowsException_returnsInternalError() {
    ErrorThrowingHandler handler = new ErrorThrowingHandler(new Exception("internal error"));
    dispatcher = createDispatcher(handler);
    dispatchRequest(handler.getMethod(), "id", null /* params */);
    assertErrorResponseWritten(ErrorCode.INTERNAL_ERROR, "id", "internal error");
  }

  @Test
  public void testHandlerThrowsRequestException_returnsErrorWithErrorCode() {
    ErrorThrowingHandler handler =
        new ErrorThrowingHandler(
            new RequestException(ErrorCode.SERVER_NOT_INITIALIZED, "custom error"));
    dispatcher = createDispatcher(handler);
    dispatchRequest(handler.getMethod(), "id", null /* params */);
    assertErrorResponseWritten(ErrorCode.SERVER_NOT_INITIALIZED, "id", "custom error");
  }

  private RequestDispatcher createDispatcher(RequestHandler<?>... handlers) {
    Gson gson = new Gson();
    RequestDispatcher.Builder builder =
        new RequestDispatcher.Builder()
            .setGson(gson)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setRequestParser(requestParser)
            .setResponseWriter(responseWriter);
    for (RequestHandler handler : handlers) {
      builder.registerHandler(handler);
    }
    return builder.build();
  }

  private boolean dispatchRequest(String method, @Nullable String id, @Nullable JsonObject params) {
    RawRequest rawRequest =
        RawRequest.create(HEADER, new RawRequest.Content(method, id, JSON_RPC, params));
    try {
      when(requestParser.parse()).thenReturn(rawRequest);
    } catch (Exception e) {
      // will not happen
    }
    return dispatcher.dispatchRequest();
  }

  private void assertResponseWritten(Response expected) {
    assertThat(responseWriter.pollResponse()).isEqualTo(expected);
  }

  private void assertErrorResponseWritten(ErrorCode errorCode, String id, String message) {
    Response response = responseWriter.pollResponse();
    assertThat(response.getId()).isEqualTo(id);
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

  private static class CountingNotificationHandler extends NotificationHandler<NullParams> {
    private int numReceived = 0;

    private CountingNotificationHandler() {
      super("notifications", NullParams.class);
    }

    @Override
    protected void handleNotification(Request<NullParams> request) throws Exception {
      numReceived++;
    }
  }

  private static class TestResponseWriter extends ResponseWriter {
    private final LinkedBlockingQueue<Response> responseQueue;

    private TestResponseWriter() {
      super(new Gson(), System.out);
      responseQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void writeResponse(Response response) {
      try {
        responseQueue.put(response);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private Response pollResponse() {
      try {
        return responseQueue.poll(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
