package org.javacomp.options;

import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;

/** User provided options. */
public interface JavaCompOptions {
  /** Path of the log file. If not set, logs are not written to any file. */
  @Nullable
  public String getLogPath();

  /** The minimum log level. Logs with the level and above will be logged. */
  @Nullable
  public Level getLogLevel();

  public List<String> getIgnorePaths();

  public List<String> getTypeIndexFiles();
}
