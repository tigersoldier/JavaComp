package org.javacomp.file;

import java.nio.file.Path;

/** Utilities for dealing with paths. */
public class PathUtils {
  private PathUtils() {}

  public static boolean shouldIgnoreFile(Path filePath) {
    String filename = filePath.getFileName().toString();

    // File names starting with a dot are hidden files in *nix systems.
    if (filename.startsWith(".")) {
      return true;
    }

    // File names ending with ~ are common backup file names.
    if (filename.endsWith("~")) {
      return true;
    }

    // File names ending with .bak are common backup file names.
    if (filename.endsWith(".bak")) {
      return true;
    }

    return false;
  }
}
