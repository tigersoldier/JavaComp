package org.javacomp.server;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.javacomp.logging.JLogger;
import org.javacomp.server.io.RequestReader;
import org.javacomp.server.io.ResponseWriter;
import org.javacomp.server.protocol.ExitHandler;
import org.javacomp.server.protocol.InitializeHandler;
import org.javacomp.server.protocol.ShutdownHandler;

/** Entry point of the JavaComp server. */
public class JavaComp implements Server {
  private static final int REQUEST_BUFFER_SIZE = 4096;
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final AtomicBoolean isRunning;
  private final RequestParser requestParser;
  private final ResponseWriter responseWriter;
  private final RequestDispatcher requestDispatcher;
  private final Gson gson;

  private volatile boolean initialized;
  private volatile int exitCode = 0;

  public JavaComp(InputStream inputStream, OutputStream outputStream) {
    this.gson = GsonUtils.getGson();
    this.isRunning = new AtomicBoolean(true);
    this.requestParser =
        new RequestParser(this.gson, new RequestReader(inputStream, REQUEST_BUFFER_SIZE));
    this.responseWriter = new ResponseWriter(this.gson, outputStream);
    this.requestDispatcher =
        new RequestDispatcher.Builder()
            .setGson(gson)
            .setRequestParser(requestParser)
            .setResponseWriter(responseWriter)
            .registerHandler(new InitializeHandler(this))
            .registerHandler(new ShutdownHandler(this))
            .registerHandler(new ExitHandler(this))
            .build();
  }

  public int run() {
    synchronized (isRunning) {
      isRunning.set(true);
    }
    while (isRunning.get()) {
      if (!requestDispatcher.dispatchRequest()) {
        exit();
      }
    }
    return exitCode;
  }

  @Override
  public synchronized void initialize(int clientProcessId) {
    initialized = true;
    //TODO: Someday we should implement monitoring client process for all major platforms.
  }

  @Override
  public synchronized void shutdown() {
    initialized = false;
  }

  @Override
  public synchronized void exit() {
    if (!isRunning.get()) {
      return;
    }

    isRunning.set(false);
    if (initialized) {
      logger.warning(new Throwable(), "exit() is called without shutting down the server.");
      exitCode = 1;
    }

    // Close input and stream to stop blocking on incoming requests.
    try {
      requestParser.close();
    } catch (Exception e) {
      logger.warning(e, "Failed to close input stream on exit.");
    }
  }

  public static final void main(String[] args) {
    int exitCode = new JavaComp(System.in, System.out).run();
    System.exit(exitCode);
  }
}
