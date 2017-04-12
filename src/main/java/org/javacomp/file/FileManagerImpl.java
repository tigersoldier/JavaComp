package org.javacomp.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Manages all files for the same project. */
public class FileManagerImpl implements FileManager {
  /**
   * A map from normalized file name to snapshotted files.
   *
   * <p>All snapshotted files are opened by clients. The the truth of the opened files is the
   * snapshot, not the file content stored on file system.
   */
  private final Map<Path, FileSnapshot> fileSnapshots;

  private final Path projectRoot;

  public FileManagerImpl(URI projectRootUri) {
    projectRoot = Paths.get(projectRootUri);
    fileSnapshots = new HashMap<>();
  }

  @Override
  public void openFileForSnapshot(URI fileUri, String content) throws IOException {
    Path filePath = uriToNormalizedPath(fileUri);
    if (fileSnapshots.containsKey(filePath)) {
      throw new IllegalStateException(String.format("File %s has already been opened.", fileUri));
    }
    fileSnapshots.put(filePath, FileSnapshot.create(filePath.toUri(), content));
  }

  @Override
  public void applyEditToSnapshot(
      URI fileUri, TextRange editRange, Optional<Integer> rangeLength, String newText) {
    Path filePath = uriToNormalizedPath(fileUri);
    if (!fileSnapshots.containsKey(filePath)) {
      throw new IllegalStateException(
          String.format("Cannot apply edit to file %s: file is not opened.", fileUri));
    }

    fileSnapshots.get(filePath).applyEdit(editRange, rangeLength, newText);
  }

  @Override
  public void setSnaphotContent(URI fileUri, String newText) {
    Path filePath = uriToNormalizedPath(fileUri);
    if (!fileSnapshots.containsKey(filePath)) {
      throw new IllegalStateException(
          String.format("Cannot apply edit to file %s: file is not opened.", fileUri));
    }

    fileSnapshots.get(filePath).setContent(newText);
  }

  @Override
  public void closeFileForSnapshot(URI fileUri) {
    Path filePath = uriToNormalizedPath(fileUri);
    if (!fileSnapshots.containsKey(filePath)) {
      throw new IllegalStateException(
          String.format("Cannot close file %s: file is not opened.", fileUri));
    }

    fileSnapshots.remove(filePath);
  }

  @Override
  public void shutdown() {
    fileSnapshots.clear();
  }

  /**
   * Convert a {@link URI} to a normalized {@link Path}. If the path is relative, throws an
   * exception.
   *
   * @throws IllegalArgumentException thrown if the URI is a relative path
   */
  private static Path uriToNormalizedPath(URI fileUri) {
    Path filePath = Paths.get(fileUri);
    if (!filePath.isAbsolute()) {
      throw new IllegalArgumentException("Cannot open a relative URI: " + fileUri);
    }

    filePath = filePath.normalize();
    return filePath;
  }
}
