package org.javacomp.server.protocol;

import static org.javacomp.server.GsonEnum.SerializeType.LOWERCASE_NAME;

import java.net.URI;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.javacomp.options.JavaCompOptions;
import org.javacomp.server.GsonEnum;

/**
 * Parameters for "initialize" method.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#initialize-request
 */
public class InitializeParams implements RequestParams {
  public int processId;
  public URI rootUri;
  public ClientCapabilities capabilities;
  public String trace;
  @Nullable public InitializationOptions initializationOptions;
  // rootPath is deprecated and not supported.

  /** JavaComp specific options. */
  public static class InitializationOptions implements JavaCompOptions {
    /** Path of the log file. If not set, logs are not written to any file. */
    @Nullable public String logPath;

    /** The minimum log level. Logs with the level and above will be logged. */
    @Nullable public LogLevel logLevel;

    @Override
    @Nullable
    public String getLogPath() {
      return logPath;
    }

    @Override
    @Nullable
    public Level getLogLevel() {
      if (logLevel == null) {
        return null;
      }
      switch (logLevel) {
        case SEVERE:
          return Level.SEVERE;
        case WARNING:
          return Level.WARNING;
        case INFO:
          return Level.INFO;
        case FINE:
          return Level.FINE;
        case FINER:
          return Level.FINER;
        case FINEST:
          return Level.FINEST;
        default:
          return null;
      }
    }
  }

  /** Java log levels. */
  @GsonEnum(LOWERCASE_NAME)
  public static enum LogLevel {
    SEVERE,
    WARNING,
    INFO,
    FINE,
    FINER,
    FINEST,
  }
}
