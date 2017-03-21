package org.javacomp.server;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The response message.
 *
 * <p>See the Language Server Protocol spec:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#response-message
 */
public class Response {
  private final String jsonrpc;
  private final String id;
  @Nullable private final Object result;
  @Nullable private final ResponseError error;

  private Response(String id, @Nullable Object result, @Nullable ResponseError error) {
    this.jsonrpc = "2.0";
    this.id = id;
    this.result = result;
    this.error = error;
  }

  /**
   * Creates a success response.
   *
   * @param id the ID of the corresponding requst.
   * @param result the result of the request. If non-null, must be able to be converted to JSON by
   *     Gson.
   */
  public static Response createResponse(String id, @Nullable Object result) {
    return new Response(id, result, null /* error */);
  }

  /** Create a response for errors. */
  public static Response createError(
      String id, ErrorCode errorCode, String msgfmt, Object... args) {
    return new Response(
        id, null /* result */, new ResponseError(errorCode, String.format(msgfmt, args)));
  }

  /** Create a response for errors. */
  public static Response createError(String id, ResponseError error) {
    return new Response(id, null /* result */, error);
  }

  public String getJsonRpc() {
    return jsonrpc;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public Object getResult() {
    return result;
  }

  @Nullable
  public ResponseError getError() {
    return error;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("result", result)
        .add("error", error)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Response)) {
      return false;
    }

    Response other = (Response) o;
    return Objects.equals(jsonrpc, other.jsonrpc)
        && Objects.equals(id, other.id)
        && Objects.equals(result, other.result)
        && Objects.equals(error, other.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jsonrpc, id, result, error);
  }

  public static class ResponseError {
    private final int code;
    private final String message;

    public ResponseError(ErrorCode errorCode, String message) {
      this.code = errorCode.getCode();
      this.message = message;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("code", code).add("message", message).toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ResponseError)) {
        return false;
      }

      ResponseError other = (ResponseError) o;
      return code == other.code && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
      return Objects.hash(code, message);
    }
  }
}
