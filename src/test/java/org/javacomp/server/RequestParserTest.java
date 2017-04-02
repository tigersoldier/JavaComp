package org.javacomp.server;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.StringBufferInputStream;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.javacomp.server.io.RequestReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RequestParserTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  private final Gson gson = GsonUtils.getGson();

  @Test
  public void testParseRequests() throws Exception {
    RequestParser parser =
        createParser(
            "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"cmd1\", \"params\": {\"foo\": 1}}",
            "{\"jsonrpc\": \"2.0\", \"id\": 2, \"method\": \"cmd2\", "
                + "\"params\": {\"bar\": \"baz\"}}");

    JsonObject params1 = new JsonObject();
    params1.addProperty("foo", 1);
    RawRequest.Content content1 =
        new RawRequest.Content("cmd1", "1" /* id */, "2.0" /* jsonrpc */, params1);
    assertThat(parser.parse().getContent()).isEqualTo(content1);

    JsonObject params2 = new JsonObject();
    params2.addProperty("bar", "baz");
    RawRequest.Content content2 =
        new RawRequest.Content("cmd2", "2" /* id */, "2.0" /* jsonrpc */, params2);
    assertThat(parser.parse().getContent()).isEqualTo(content2);
  }

  private RequestParser createParser(String... contents) {
    StringBuilder sb = new StringBuilder();
    for (String content : contents) {
      int length = content.getBytes(UTF_8).length;
      sb.append("Content-Length: ");
      sb.append(length);
      sb.append("\r\n");
      sb.append("Content-Type: utf8\r\n");
      sb.append("\r\n");
      sb.append(content);
    }
    StringBufferInputStream in = new StringBufferInputStream(sb.toString());
    return new RequestParser(gson, new RequestReader(in, 1024 /* capacity */));
  }

  private Matcher<RequestException> errorCodeIs(ErrorCode errorCode) {
    return new CustomMatcher<RequestException>("ParseException with error code " + errorCode) {
      @Override
      public boolean matches(Object object) {
        return (object instanceof RequestException)
            && ((RequestException) object).getErrorCode() == errorCode;
      }
    };
  }
}
