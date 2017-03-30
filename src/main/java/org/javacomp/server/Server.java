package org.javacomp.server;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.javacomp.logging.JLogger;
import org.javacomp.server.io.RequestReader;
import org.javacomp.server.io.ResponseWriter;
import org.javacomp.server.io.StreamClosedException;

/** Entry point of the JavaComp server. */
public class Server {
  private static final int REQUEST_BUFFER_SIZE = 4096;
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final AtomicBoolean shutdown;
  private final RequestParser requestParser;
  private final RequestReader requestReader;
  private final ResponseWriter requestWriter;
  private final RequestDispatcher requestDispatcher;
  private final Gson gson = new Gson();

  public Server(InputStream inputStream, OutputStream outputStream) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.shutdown = new AtomicBoolean(false);
    this.requestReader = new RequestReader(inputStream, REQUEST_BUFFER_SIZE);
    this.requestParser = new RequestParser(gson);
    this.requestWriter = new ResponseWriter(gson, outputStream);
    this.requestDispatcher = new RequestDispatcher.Builder(gson).build();
  }

  public void run() {
    while (!shutdown.get()) {
      RawRequest rawRequest;
      Response response;
      try {
        rawRequest = requestParser.parse(requestReader);
        response = requestDispatcher.dispatchRequest(rawRequest);
      } catch (RequestException e) {
        response = Response.createError(null /* id */, e.getErrorCode(), e.getMessage());
      } catch (StreamClosedException e) {
        logger.severe(e, "Input stream closed, shutting down server.");
        shutdown();
        return;
      }
      try {
        requestWriter.writeResponse(response);
      } catch (Exception e) {
        logger.severe(e, "Failed to write response, shutting down.");
        shutdown();
      }
    }
  }

  public void shutdown() {
    shutdown.set(true);
  }

  public static final void main(String[] args) {
    new Server(System.in, System.out).run();
  }
}
