package org.javacomp.server.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.javacomp.server.Response;

/**
 * A writer that writes {@link Response} to an {@link OutputStream}.
 *
 * <p>The format is defined in the Base Protocol section of Language Server Protocol:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#base-protocol
 *
 * <p>When the writer is closed, the underlying {@link OutputStream} is closed as well.
 */
public class ResponseWriter implements Closeable {
  private static final String HEADER_CONTENT_LENGTH = "Content-Length";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String CONTENT_TYPE = "application/javacomp-jsonrpc; charset=utf8";
  private static final String HEADER_DELIMITER = ": ";
  private static final String HEADER_LINE_TERMINATOR = "\r\n";
  private final Gson gson;
  private final BufferedWriter writer;

  public ResponseWriter(Gson gson, OutputStream outputStream) {
    this.gson = gson;
    this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8));
  }

  public void writeResponse(Response response) throws IOException {
    String content = gson.toJson(response);
    int contentLength = content.getBytes(UTF_8).length;
    writeHeader(HEADER_CONTENT_LENGTH, "" + contentLength);
    writeHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE);
    writer.write(HEADER_LINE_TERMINATOR);
    writer.write(content);
    writer.flush();
  }

  private void writeHeader(String key, String value) throws IOException {
    writer.write(key);
    writer.write(HEADER_DELIMITER);
    writer.write(value);
    writer.write(HEADER_LINE_TERMINATOR);
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}
