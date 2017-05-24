package org.javacomp.project;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.sun.source.tree.LineMap;
import java.io.IOException;
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
import java.util.Set;
import java.util.stream.Stream;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.completion.Completor;
import org.javacomp.file.FileChangeListener;
import org.javacomp.file.FileManager;
import org.javacomp.file.FileTextLocation;
import org.javacomp.file.PathUtils;
import org.javacomp.file.TextPosition;
import org.javacomp.file.TextRange;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.ModuleScope;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.FileContentFixer;
import org.javacomp.parser.FileContentFixer.FixedContent;
import org.javacomp.parser.ParserContext;
import org.javacomp.reference.DefinitionSolver;
import org.javacomp.reference.MethodSignatures;
import org.javacomp.reference.SignatureSolver;

/** Handles all files in a project. */
public class Project {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final String JAVA_EXTENSION = ".java";

  private final ModuleScope globalScope;
  private final AstScanner astScanner;
  private final Completor completor;
  private final DefinitionSolver definitionSolver;
  private final SignatureSolver signatureSolver;
  private final FileManager fileManager;
  private final URI rootUri;
  private final ParserContext parserContext;
  private final FileContentFixer fileContentFixer;
  private final Set<Path> ignorePaths;

  private Path lastCompletedFile = null;

  private boolean initialized;

  public Project(FileManager fileManager, URI rootUri, IndexOptions indexOptions) {
    globalScope = new ModuleScope();
    astScanner = new AstScanner(indexOptions);
    completor = new Completor();
    parserContext = new ParserContext();
    fileContentFixer = new FileContentFixer(parserContext);
    this.fileManager = fileManager;
    this.rootUri = rootUri;
    this.definitionSolver = new DefinitionSolver();
    this.signatureSolver = new SignatureSolver();
    this.ignorePaths =
        indexOptions
            .ignorePaths()
            .stream()
            .map(p -> Paths.get(p))
            .collect(ImmutableSet.toImmutableSet());
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

  private void walkDirectory(Path rootDir) {
    Deque<Path> queue = new LinkedList<>();
    queue.add(rootDir);
    while (!queue.isEmpty()) {
      Path baseDir = queue.remove();
      try (Stream<Path> entryStream = Files.list(baseDir)) {
        entryStream.forEach(
            entryPath -> {
              Path relativePath = rootDir.relativize(entryPath);
              if (ignorePaths.contains(relativePath)) {
                logger.info("Ignoring path %s", entryPath);
                return;
              }
              if (Files.isDirectory(entryPath)) {
                queue.add(entryPath);
              } else if (isJavaFile(entryPath) && !PathUtils.shouldIgnoreFile(entryPath)) {
                addOrUpdateFile(entryPath);
              }
            });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void addOrUpdateFile(Path filePath) {
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
          astScanner.startScan(
              parserContext.parse(filePath.toString(), content), filePath.toString(), content);
      fileScope.setAdjustedLineMap(adjustedLineMap);
      globalScope.addOrReplaceFileScope(fileScope);
    } catch (Throwable e) {
      logger.warning(e, "Failed to process file %s", filePath);
    }
  }

  private void removeFile(Path filePath) {
    globalScope.removeFile(filePath);
  }

  /**
   * @param filePath the path of the file beging completed
   * @param line 0-based line number
   * @param column 0-based character offset of the line
   */
  public List<CompletionCandidate> getCompletionCandidates(Path filePath, int line, int column) {
    if (!filePath.equals(lastCompletedFile)) {
      lastCompletedFile = filePath;
      addOrUpdateFile(filePath);
    }
    return completor.getCompletionCandidates(globalScope, filePath, line, column);
  }

  /**
   * @param filePath the path of the file beging completed
   * @param line 0-based line number
   * @param column 0-based character offset of the line
   */
  public List<FileTextLocation> findDefinitions(Path filePath, int line, int column) {
    List<? extends Entity> entities =
        definitionSolver.getDefinitionEntities(globalScope, filePath, line, column);
    return entities
        .stream()
        .map(
            entity -> {
              Range<Integer> range = entity.getSymbolRange();
              EntityScope scope = entity.getChildScope();
              while (!(scope instanceof FileScope) && scope.getParentScope().isPresent()) {
                scope = scope.getParentScope().get();
              }

              if (!(scope instanceof FileScope)) {
                throw new RuntimeException("Cannot reach file scope for " + entity);
              }

              FileScope fileScope = (FileScope) scope;
              LineMap lineMap = fileScope.getLineMap();
              TextPosition start =
                  TextPosition.create(
                      (int) lineMap.getLineNumber(range.lowerEndpoint()) - 1,
                      (int) lineMap.getColumnNumber(range.lowerEndpoint()) - 1);
              TextPosition end =
                  TextPosition.create(
                      (int) lineMap.getLineNumber(range.upperEndpoint()) - 1,
                      (int) lineMap.getColumnNumber(range.upperEndpoint()) - 1);
              TextRange textRange = TextRange.create(start, end);
              return FileTextLocation.create(Paths.get(fileScope.getFilename()), textRange);
            })
        .collect(ImmutableList.toImmutableList());
  }

  public MethodSignatures findMethodSignatures(Path filePath, int line, int column) {
    return signatureSolver.getMethodSignatures(globalScope, filePath, line, column);
  }

  public ModuleScope getModuleScope() {
    return globalScope;
  }

  private static boolean isJavaFile(Path filePath) {
    return filePath.toString().endsWith(JAVA_EXTENSION) && Files.isRegularFile(filePath);
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
        // The global scope handles nonexistence file correctly.
        removeFile(filePath);
      }
    }
  }
}
