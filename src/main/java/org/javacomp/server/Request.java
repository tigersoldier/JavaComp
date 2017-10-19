package org.javacomp.server;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.javacomp.protocol.NullParams;
import org.javacomp.protocol.RequestParams;

/**
 * A request sent by the client.
 *
 * <p>The request handles the base protocol of Microsoft Language Server Protocol. See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#base-protocol
 *
 * @param <T> the type of the request parameters. If the request doesn't have any parameter, type
 *     must be {@link NullParams}.
 */
@AutoValue
public abstract class Request<T extends RequestParams> {

  /** Header of the request. Keys are lowercased. */
  public abstract ImmutableMap<String, String> getHeader();

  /** The name of the request method. */
  public abstract String getMethod();

  /** The request ID. If the ID is null, it's a notification. */
  @Nullable
  public abstract String getId();

  /** The parameters specific to the requdst method. */
  @Nullable
  public abstract T getParams();

  public static <T extends RequestParams> Request<T> create(
      Map<String, String> header, String method, String messageId, @Nullable T params) {
    return new AutoValue_Request<T>(ImmutableMap.copyOf(header), method, messageId, params);
  }
}
