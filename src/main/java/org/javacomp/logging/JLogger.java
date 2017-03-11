package org.javacomp.logging;

import java.util.logging.Logger;

/**
 * Wrapper around Java logger.
 *
 * <p>This wrapper provide convenient methods for creating loggers and logging with formatting
 * strings.
 */
public class JLogger {
  private final Logger javaLogger;

  private JLogger(String enclosingClassName) {
    javaLogger = Logger.getLogger(enclosingClassName);
  }

  /** Creates a {@link JLogger} and sets its tag to the name of the class that calls it. */
  public static JLogger createForEnclosingClass() {
    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    // The top of the stack trace is this method. The one belows it is the caller class.
    String enclosingClassName = stackTrace[1].getClassName();
    return new JLogger(enclosingClassName);
  }

  /** Logs a message at severe level. */
  public void severe(String msg) {
    javaLogger.severe(msg);
  }

  /**
   * Logs a message at severe level with formatting parameters.
   *
   * @param msgfmt the message format string that can be accepted by {@link String#format}
   * @param args arguments to be filled into {@code msgfmt}
   */
  public void severe(String msgfmt, Object... args) {
    javaLogger.severe(String.format(msgfmt, args));
  }

  /** Logs a message at warning level. */
  public void warning(String msg) {
    javaLogger.warning(msg);
  }

  /**
   * Logs a message at warning level with formatting parameters.
   *
   * @param msgfmt the message format string that can be accepted by {@link String#format}
   * @param args arguments to be filled into {@code msgfmt}
   */
  public void warning(String msgfmt, Object... args) {
    javaLogger.warning(String.format(msgfmt, args));
  }

  /** Logs a message at info level. */
  public void info(String msg) {
    javaLogger.info(msg);
  }

  /**
   * Logs a message at info level with formatting parameters.
   *
   * @param msgfmt the message format string that can be accepted by {@link String#format}
   * @param args arguments to be filled into {@code msgfmt}
   */
  public void info(String msgfmt, Object... args) {
    javaLogger.info(String.format(msgfmt, args));
  }
}
