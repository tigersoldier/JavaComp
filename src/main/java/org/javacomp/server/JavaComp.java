package org.javacomp.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.javacomp.file.FileManager;
import org.javacomp.file.FileManagerImpl;
import org.javacomp.logging.JLogger;
import org.javacomp.options.IndexOptions;
import org.javacomp.options.JavaCompOptions;
import org.javacomp.project.Project;
import org.javacomp.server.io.RequestReader;
import org.javacomp.server.io.ResponseWriter;
import org.javacomp.server.protocol.CompletionTextDocumentHandler;
import org.javacomp.server.protocol.DefinitionTextDocumentHandler;
import org.javacomp.server.protocol.DidChangeTextDocumentHandler;
import org.javacomp.server.protocol.DidCloseTextDocumentHandler;
import org.javacomp.server.protocol.DidOpenTextDocumentHandler;
import org.javacomp.server.protocol.ExitHandler;
import org.javacomp.server.protocol.HoverTextDocumentHandler;
import org.javacomp.server.protocol.InitializeHandler;
import org.javacomp.server.protocol.ShutdownHandler;
import org.javacomp.server.protocol.SignatureHelpTextDocumentHandler;

/** Entry point of the JavaComp server. */
public class JavaComp implements Server {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final int REQUEST_BUFFER_SIZE = 4096;
  private static final int NUM_THREADS = 10;

  private final AtomicBoolean isRunning;
  private final RequestParser requestParser;
  private final ResponseWriter responseWriter;
  private final ExecutorService executor;
  private final RequestDispatcher requestDispatcher;
  private final Gson gson;

  private boolean initialized;
  private int exitCode = 0;
  private FileManager fileManager;
  private Project project;

  public JavaComp(InputStream inputStream, OutputStream outputStream) {
    this.gson = GsonUtils.getGson();
    this.isRunning = new AtomicBoolean(true);
    this.requestParser =
        new RequestParser(this.gson, new RequestReader(inputStream, REQUEST_BUFFER_SIZE));
    this.responseWriter = new ResponseWriter(this.gson, outputStream);
    this.executor = Executors.newFixedThreadPool(NUM_THREADS);
    this.requestDispatcher =
        new RequestDispatcher.Builder()
            .setGson(gson)
            .setRequestParser(requestParser)
            .setResponseWriter(responseWriter)
            // Server manipulation
            .registerHandler(new InitializeHandler(this))
            .registerHandler(new ShutdownHandler(this))
            .registerHandler(new ExitHandler(this))
            // Text document manipulation
            .registerHandler(new DidOpenTextDocumentHandler(this))
            .registerHandler(new DidChangeTextDocumentHandler(this))
            .registerHandler(new DidCloseTextDocumentHandler(this))
            .registerHandler(new CompletionTextDocumentHandler(this))
            .registerHandler(new DefinitionTextDocumentHandler(this))
            .registerHandler(new SignatureHelpTextDocumentHandler(this))
            .registerHandler(new HoverTextDocumentHandler(this))
            .setExecutor(executor)
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
  public synchronized void initialize(
      int clientProcessId, URI projectRootUri, @Nullable JavaCompOptions options) {
    checkState(!initialized, "Cannot initialize the server twice in a row.");
    initialized = true;

    List<String> ignorePaths;
    if (options != null) {
      Level logLevel = options.getLogLevel();
      String logPath = options.getLogPath();
      if (logPath != null) {
        JLogger.setLogFile(logPath);
      }
      if (logLevel != null) {
        JLogger.setLogLevel(logLevel);
      }
      ignorePaths = options.getIgnorePaths();
    } else {
      ignorePaths = ImmutableList.of();
    }
    fileManager = new FileManagerImpl(projectRootUri, ignorePaths, executor);
    project = new Project(fileManager, projectRootUri, IndexOptions.FULL_INDEX_BUILDER.build());

    // Project initialization can take a while. Run it in a separate thread.
    executor.submit(
        () -> {
          synchronized (JavaComp.this) {
            project.initialize();
            project.loadJdkModule();
            if (options != null) {
              for (String indexFilePath : options.getTypeIndexFiles()) {
                project.loadTypeIndexFile(indexFilePath);
              }
            }
          }
        });

    // TODO: Someday we should implement monitoring client process for all major platforms.
  }

  @Override
  public synchronized void shutdown() {
    checkState(initialized, "Shutting down the server without initializing it.");
    initialized = false;
    fileManager.shutdown();
    fileManager = null;
    executor.shutdown();
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

  @Override
  public synchronized FileManager getFileManager() {
    checkState(initialized, "Server not initialized.");
    return checkNotNull(fileManager);
  }

  @Override
  public synchronized Project getProject() {
    checkState(initialized, "Server not initialized.");
    return checkNotNull(project);
  }

  public static final void main(String[] args) {
    if (args.length > 0 && "-v".equals(args[0])) {
      System.out.println(
          Constants.MAJOR_VERSION + "." + Constants.MINOR_VERSION + "." + Constants.PATCH_VERSION);
      System.exit(0);
    }
    int exitCode = new JavaComp(System.in, System.out).run();
    System.exit(exitCode);
  }
}
