package org.javacomp.server.protocol;

import static org.javacomp.server.GsonEnum.SerializeType.LOWERCASE_NAME;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.List;
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

    /**
     * Pattern of paths that JavaComp should ignore.
     *
     * <p>The patterns are valid Java path glob patterns defined by the documentation of {@link
     * java.nio.file.FileSystem#getPathMatcher} without {@code "glob:"} prefix.
     *
     * <p>When determing whether a path of a file or directory should be ignored, the path is
     * converted into 2 froms: the filename and pseudo absolute path. A pseudo absolute path is the
     * path without the prefix of project root path. For example, if a path is /root/path/foo/bar
     * and the project root path is /root/path, its pseudo absolute path is /foo/bar. If a path is
     * not under project root, its pseudo absolute path is itself.
     *
     * <p>Both filename of the path and its pseudo absolute path are checked by all ignore path
     * patterns. If either form matches any of the patterns, the path is ignored.
     */
    @Nullable public List<String> ignorePaths;

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

    public List<String> getIgnorePaths() {
      if (ignorePaths == null) {
        return ImmutableList.of();
      }
      return ImmutableList.copyOf(ignorePaths);
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
