package org.javacomp.tool;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.javacomp.file.FileChangeListener;
import org.javacomp.file.FileManager;
import org.javacomp.file.TextRange;
import org.javacomp.options.IndexOptions;
import org.javacomp.project.Project;
import org.javacomp.storage.IndexStore;

/**
 * Creates index files for specified source code.
 *
 * <p>Usage: Indexer <root path> <output file> <ignored paths...>
 */
public class Indexer {

  private final Path rootPath;
  private final FileManager fileManager;
  private final Project project;

  public Indexer(String rootPath, List<String> ignoredPaths) {
    this.rootPath = Paths.get(rootPath);
    this.fileManager = new SimpleFileManager();
    this.project =
        new Project(
            fileManager,
            this.rootPath.toUri(),
            IndexOptions.PUBLIC_READONLY_BUILDER.setIgnorePaths(ignoredPaths).build());
  }

  public void run(String indexFile) {
    project.initialize();
    new IndexStore().writeModuleScopeToFile(project.getModuleScope(), Paths.get(indexFile));
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: Indexer <root path> <output file> <ignored paths...>");
      return;
    }
    List<String> ignorePaths = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      ignorePaths.add(args[i]);
    }

    new Indexer(args[0], ignorePaths).run(args[1]);
  }

  public static class SimpleFileManager implements FileManager {

    @Override
    public void openFileForSnapshot(URI fileUri, String content) throws IOException {
      // No-op;
    }

    @Override
    public void applyEditToSnapshot(
        URI fileUri, TextRange editRange, Optional<Integer> rangeLength, String newText) {
      // No-op
    }

    @Override
    public void setSnaphotContent(URI fileUri, String newText) {
      // No-op
    }

    @Override
    public void closeFileForSnapshot(URI fileUri) {
      // No-op
    }

    @Override
    public void watchSubDirectories(Path rootDirectory) {
      // No-op
    }

    @Override
    public void setFileChangeListener(FileChangeListener listener) {
      // No-op
    }

    @Override
    public Optional<CharSequence> getFileContent(Path filePath) {
      try {
        return Optional.of(new String(Files.readAllBytes(filePath), UTF_8));
      } catch (IOException e) {
        // fall through.
      }
      return Optional.ofNullable(null);
    }

    @Override
    public void shutdown() {
      // No-op
    }
  }
}
