package org.javacomp.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Optional;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.completion.Completor;
import org.javacomp.completion.TextEdits;
import org.javacomp.file.FileChangeListener;
import org.javacomp.file.FileManager;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;
import org.javacomp.options.IndexOptions;
import org.javacomp.protocol.TextEdit;
import org.javacomp.reference.DefinitionSolver;
import org.javacomp.reference.MethodSignatures;
import org.javacomp.reference.ReferenceSolver;
import org.javacomp.reference.SignatureSolver;
import org.javacomp.storage.IndexStore;

/** Handles all files in a project. */
public class Project {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final String JDK_RESOURCE_PATH = "/resources/jdk/index.json";
  private static final String JAVA_EXTENSION = ".java";

  private final FileManager fileManager;
  private final Completor completor;
  private final DefinitionSolver definitionSolver;
  private final SignatureSolver signatureSolver;
  private final ModuleManager moduleManager;
  private Path lastCompletedFile = null;

  private boolean initialized;

  public Project(FileManager fileManager, URI rootUri, IndexOptions indexOptions) {
    this(new FileSystemModuleManager(fileManager, Paths.get(rootUri), indexOptions), fileManager);
  }

  public Project(ModuleManager moduleManager, FileManager fileManager) {
    completor = new Completor(fileManager);
    this.fileManager = fileManager;
    this.definitionSolver = new DefinitionSolver();
    this.signatureSolver = new SignatureSolver();
    this.moduleManager = moduleManager;
  }

  public synchronized void initialize() {
    if (initialized) {
      logger.warning("Project has already been initalized.");
      return;
    }
    initialized = true;

    fileManager.setFileChangeListener(new ProjectFileChangeListener());
    moduleManager.initialize();
  }

  public synchronized void loadJdkModule() {
    logger.info("Loading JDK module");
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(this.getClass().getResourceAsStream(JDK_RESOURCE_PATH), UTF_8))) {
      moduleManager.addDependingModule(new IndexStore().readModule(reader));
      logger.info("JDK module loaded");
    } catch (Throwable t) {
      logger.warning(t, "Unable to load JDK module");
    }
  }

  public synchronized void loadTypeIndexFile(String typeIndexFile) {
    logger.info("Loading type index file %s", typeIndexFile);
    IndexStore indexStore = new IndexStore();
    try {
      Module module =
          indexStore.readModuleFromFile(
              fileManager.getProjectRootPath().resolve(Paths.get(typeIndexFile)));
      moduleManager.addDependingModule(module);
      logger.info("Loaded type index file %s", typeIndexFile);
    } catch (NoSuchFileException nsfe) {
      logger.warning("Unable to load type index file %s: file doesn't exist", typeIndexFile);
    } catch (Throwable t) {
      logger.warning(t, "Unable to load type index file %s", typeIndexFile);
    }
  }

  private synchronized void addOrUpdateFile(Path filePath) {
    // Only fix content for files that are under completion.
    boolean fixContentForParsing = lastCompletedFile != null && lastCompletedFile.equals(filePath);
    moduleManager.addOrUpdateFile(filePath, fixContentForParsing);
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
    return completor.getCompletionCandidates(moduleManager, filePath, line, column);
  }

  /**
   * @param filePath the path of the file beging completed
   * @param line 0-based line number
   * @param column 0-based character offset of the line
   */
  public synchronized List<? extends Entity> findDefinitions(Path filePath, int line, int column) {
    return definitionSolver.getDefinitionEntities(moduleManager, filePath, line, column);
  }

  public synchronized MethodSignatures findMethodSignatures(Path filePath, int line, int column) {
    return signatureSolver.getMethodSignatures(moduleManager, filePath, line, column);
  }

  public synchronized TextEdit textEditForImport(Path filePath, String fullClassName) {
    return new TextEdits().forImportClass(moduleManager, filePath, fullClassName).orElse(null);
  }

  public synchronized Optional<FileItem> getFileItem(Path filePath) {
    return moduleManager.getFileItem(filePath);
  }

  public synchronized Multimap<FileScope, Range<Integer>> findReferencesAtPosition(
      Path filePath, int line, int column) {
    return new ReferenceSolver(fileManager).findReferences(moduleManager, filePath, line, column);
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
        moduleManager.removeFile(filePath);
      }
    }
  }
}
