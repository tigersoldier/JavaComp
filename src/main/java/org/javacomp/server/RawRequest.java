package org.javacomp.server;

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Objects;

/**
 * A parsed request. Params are parsed as raw JSON object, not their own types.
 *
 * <p>A request consists of header and content. Header of a request are key-value pairs. All keys
 * are lowercased in the parsed request. Contents are of the same format, where the actual params
 * are represented as a JSON Object.
 *
 * <p>This class is mainly for deserialization. The fully-typed requst that everyone should use is
 * {@link Request}.
 */
@AutoValue
public abstract class RawRequest {
  /** Gets the header of the request. All keys are lowercased. */
  public abstract Map<String, String> getHeader();

  /** Gets the content of the request. */
  public abstract Content getContent();

  public static RawRequest create(Map<String, String> header, Content content) {
    return new AutoValue_RawRequest(header, content);
  }

  /**
   * Content of the request.
   *
   * <p>id, method and params are guaranteed to be non-null by {@link RequestParser}.
   */
  public static class Content {
    private String method;
    private JsonElement id;
    private String jsonrpc;
    private JsonElement params;

    public Content() {}

    public Content(String method, JsonElement id, String jsonrpc, JsonElement params) {
      this.method = method;
      this.id = id;
      this.jsonrpc = jsonrpc;
      this.params = params;
    }

    public String getMethod() {
      return method;
    }

    public JsonElement getId() {
      return id;
    }

    public String getJsonRpc() {
      return jsonrpc;
    }

    public JsonElement getParams() {
      return params;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Content)) {
        return false;
      }
      Content other = (Content) o;
      return Objects.equals(this.id, other.id)
          && Objects.equals(this.method, other.method)
          && Objects.equals(this.jsonrpc, other.jsonrpc)
          && Objects.equals(this.params, other.params);
    }

    @Override
    public int hashCode() {
      return Objects.hash(method, id, jsonrpc, params);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("method", method)
          .add("id", id)
          .add("jsonrpc", jsonrpc)
          .add("params", params)
          .toString();
    }
  }
}
