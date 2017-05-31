package org.javacomp.file;

import com.google.common.collect.ImmutableList;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

/** Utilities for dealing with paths. */
public class PathUtils {
  public static final ImmutableList<PathMatcher> DEFAULT_IGNORE_MATCHERS;
  private static final Path PSUEDO_ROOT_PATH = Paths.get("/");

  static {
    FileSystem fs = FileSystems.getDefault();
    DEFAULT_IGNORE_MATCHERS =
        ImmutableList.of(
            // File names starting with a dot are hidden files in *nix systems.
            fs.getPathMatcher("glob:.*"),
            // File names ending with ~ are common backup file names.
            fs.getPathMatcher("glob:*~"),
            // File names ending with .bak are common backup file names.
            fs.getPathMatcher("glob:*.bak"));
  }

  private PathUtils() {}

  /**
   * Checks whether {@code entryPath} should be ignored according to any of the matchers in {@code
   * ignorePathMatchers}.
   *
   * <p>{@code ignorePathMatchers} are used on the filename part of {@code entryPath}, and the
   * pseudo absolute path created from {@code entryPath} and {@code projectRootPath}.
   *
   * <p>A pseudo absolute is {@code entryPath} itself if it's not under {@code projectRootPath} or
   * {@code entryPath} with the {@code projectRootPath} part removed and the remaining path as an
   * absolute path. For * example, if {@code entryPath} is {@code /root/path/foo/bar}, and {@code
   * projectRootPath} is {@code /root/path}, then the pseudo absolute path is {@code /foo/bar}. This
   * allows clients easily configre matchers relative to the project root path without worrying what
   * the root path is.
   */
  public static boolean shouldIgnorePath(
      Path entryPath, Path projectRootPath, List<PathMatcher> ignorePathMatchers) {
    if (ignorePathMatchers.isEmpty()) {
      return false;
    }

    Path pathName = entryPath.getFileName();
    Path relativePath = projectRootPath.relativize(entryPath);
    Path pseudoAbsolutePath = PSUEDO_ROOT_PATH.resolve(relativePath);

    for (PathMatcher matcher : ignorePathMatchers) {
      if (matcher.matches(pseudoAbsolutePath)) {
        return true;
      }
      if (matcher.matches(pathName)) {
        return true;
      }
    }

    return false;
  }
}
