package org.javacomp.logging;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Wrapper around Java logger.
 *
 * <p>This wrapper provide convenient methods for creating loggers and logging with formatting
 * strings.
 */
public class JLogger {
  private static volatile boolean hasFileHandler = false;
  private final Logger javaLogger;

  public static synchronized void setLogFile(String filePath) {
    Logger rootLogger = Logger.getLogger("");

    if (hasFileHandler) {
      rootLogger.warning("Log file has already been set.");
      return;
    }
    hasFileHandler = true;

    try {
      FileHandler fileHandler = new FileHandler(filePath);
      fileHandler.setFormatter(new SimpleFormatter());
      rootLogger.addHandler(fileHandler);
    } catch (Exception e) {
    }
  }

  public static synchronized void setLogLevel(Level level) {
    Logger rootLogger = Logger.getLogger("");
    rootLogger.setLevel(level);
    for (Handler handler : rootLogger.getHandlers()) {
      handler.setLevel(level);
    }
  }

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

  /**
   * Logs a message at severe level with formatting parameters and associated Throwable information.
   *
   * @param thrown Throwable associated with the log message
   * @param msgfmt the message format string that can be accepted by {@link String#format}
   * @param args arguments to be filled into {@code msgfmt}
   */
  public void severe(Throwable thrown, String msgfmt, Object... args) {
    javaLogger.log(Level.SEVERE, String.format(msgfmt, args), thrown);
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

  /**
   * Logs a message at warning level with formatting parameters and associated Throwable
   * information.
   *
   * @param thrown Throwable associated with the log message
   * @param msgfmt the message format string that can be accepted by {@link String#format}
   * @param args arguments to be filled into {@code msgfmt}
   */
  public void warning(Throwable thrown, String msgfmt, Object... args) {
    javaLogger.log(Level.WARNING, String.format(msgfmt, args), thrown);
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

  /** Logs a message at fine level. */
  public void fine(String msg) {
    javaLogger.fine(msg);
  }

  /**
   * Logs a message at fine level with formatting parameters.
   *
   * @param msgfmt the message format string that can be accepted by {@link String#format}
   * @param args arguments to be filled into {@code msgfmt}
   */
  public void fine(String msgfmt, Object... args) {
    javaLogger.fine(String.format(msgfmt, args));
  }
}
