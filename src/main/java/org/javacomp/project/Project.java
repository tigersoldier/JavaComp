package org.javacomp.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.source.tree.LineMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.completion.Completor;
import org.javacomp.file.FileChangeListener;
import org.javacomp.file.FileManager;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.FileContentFixer;
import org.javacomp.parser.FileContentFixer.FixedContent;
import org.javacomp.parser.ParserContext;
import org.javacomp.reference.DefinitionSolver;
import org.javacomp.reference.MethodSignatures;
import org.javacomp.reference.SignatureSolver;
import org.javacomp.storage.IndexStore;

/** Handles all files in a project. */
public class Project {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final String JAVA_EXTENSION = ".java";
  private static final String JDK_RESOURCE_PATH = "/resources/jdk/index.json";

  private final Module projectModule;
  private final IndexOptions indexOptions;
  private final Completor completor;
  private final DefinitionSolver definitionSolver;
  private final SignatureSolver signatureSolver;
  private final FileManager fileManager;
  private final URI rootUri;
  private final ParserContext parserContext;
  private final FileContentFixer fileContentFixer;
  private Path lastCompletedFile = null;

  private boolean initialized;

  public Project(FileManager fileManager, URI rootUri, IndexOptions indexOptions) {
    projectModule = new Module();
    completor = new Completor();
    parserContext = new ParserContext();
    fileContentFixer = new FileContentFixer(parserContext);
    this.indexOptions = indexOptions;
    this.fileManager = fileManager;
    this.rootUri = rootUri;
    this.definitionSolver = new DefinitionSolver();
    this.signatureSolver = new SignatureSolver();
  }

  public synchronized void initialize() {
    if (initialized) {
      logger.warning("Project has already been initalized.");
      return;
    }
    initialized = true;

    fileManager.setFileChangeListener(new ProjectFileChangeListener());

    walkDirectory(Paths.get(rootUri));
  }

  public synchronized void loadJdkModule() {
    logger.fine("Loading JDK module");
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(this.getClass().getResourceAsStream(JDK_RESOURCE_PATH), UTF_8))) {
      logger.fine("JDK module loaded");
      projectModule.addDependingModule(new IndexStore().readModule(reader));
    } catch (Throwable t) {
      logger.warning(t, "Unable to load JDK module");
    }
  }

  private void walkDirectory(Path rootDir) {
    Deque<Path> queue = new LinkedList<>();
    queue.add(rootDir);
    while (!queue.isEmpty()) {
      Path baseDir = queue.remove();
      try (Stream<Path> entryStream = Files.list(baseDir)) {
        entryStream.forEach(
            entryPath -> {
              if (fileManager.shouldIgnorePath(entryPath)) {
                logger.info("Ignoring path %s", entryPath);
                return;
              }
              if (Files.isDirectory(entryPath)) {
                queue.add(entryPath);
              } else if (isJavaFile(entryPath) && !fileManager.shouldIgnorePath(entryPath)) {
                addOrUpdateFile(entryPath);
              }
            });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private synchronized void addOrUpdateFile(Path filePath) {
    try {
      Optional<CharSequence> optionalContent = fileManager.getFileContent(filePath);
      if (!optionalContent.isPresent()) {
        return;
      }
      CharSequence content = optionalContent.get();
      LineMap adjustedLineMap = null;

      if (lastCompletedFile != null && lastCompletedFile.equals(filePath)) {
        FixedContent fixedContent = fileContentFixer.fixFileContent(content);
        content = fixedContent.getContent();
        adjustedLineMap = fixedContent.getAdjustedLineMap();
      }
      FileScope fileScope =
          new AstScanner(indexOptions)
              .startScan(
                  parserContext.parse(filePath.toString(), content), filePath.toString(), content);
      fileScope.setAdjustedLineMap(adjustedLineMap);
      projectModule.addOrReplaceFileScope(fileScope);
    } catch (Throwable e) {
      logger.warning(e, "Failed to process file %s", filePath);
    }
  }

  private void removeFile(Path filePath) {
    projectModule.removeFile(filePath);
  }

  /**
   * @param filePath the path of the file beging completed
   * @param line 0-based line number
   * @param column 0-based character offset of the line
   */
  public synchronized List<CompletionCandidate> getCompletionCandidates(
      Path filePath, int line, int column) {
    if (!filePath.equals(lastCompletedFile)) {
      lastCompletedFile = filePath;
      addOrUpdateFile(filePath);
    }
    return completor.getCompletionCandidates(projectModule, filePath, line, column);
  }

  /**
   * @param filePath the path of the file beging completed
   * @param line 0-based line number
   * @param column 0-based character offset of the line
   */
  public synchronized List<? extends Entity> findDefinitions(Path filePath, int line, int column) {
    return definitionSolver.getDefinitionEntities(projectModule, filePath, line, column);
  }

  public synchronized MethodSignatures findMethodSignatures(Path filePath, int line, int column) {
    return signatureSolver.getMethodSignatures(projectModule, filePath, line, column);
  }

  public Module getModule() {
    return projectModule;
  }

  private static boolean isJavaFile(Path filePath) {
    // We don't check if file is regular file here because the file may be new in editor and not
    // saved to the file system.
    return filePath.toString().endsWith(JAVA_EXTENSION) && !Files.isDirectory(filePath);
  }

  private class ProjectFileChangeListener implements FileChangeListener {
    @Override
    public void onFileChange(Path filePath, WatchEvent.Kind<?> changeKind) {
      logger.fine("onFileChange(%s): %s", changeKind, filePath);
      if (changeKind == StandardWatchEventKinds.ENTRY_CREATE
          || changeKind == StandardWatchEventKinds.ENTRY_MODIFY) {
        if (isJavaFile(filePath)) {
          addOrUpdateFile(filePath);
        }
      } else if (changeKind == StandardWatchEventKinds.ENTRY_DELETE) {
        // Do not check if the file is a java source file here. Deleted file is not a regular file.
        // The module handles nonexistence file correctly.
        removeFile(filePath);
      }
    }
  }
}
