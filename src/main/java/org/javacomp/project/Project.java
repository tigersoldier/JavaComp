package org.javacomp.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.completion.Completor;
import org.javacomp.file.FileChangeListener;
import org.javacomp.file.FileManager;
import org.javacomp.logging.JLogger;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;

/** Handles all files in a project. */
public class Project {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final String JAVA_EXTENSION = ".java";

  private final GlobalScope globalScope;
  private final Context javacContext;
  private final JavacFileManager javacFileManager;
  private final AstScanner astScanner;
  private final Completor completor;
  private final FileManager fileManager;
  private final URI rootUri;

  private boolean initialized;

  public Project(FileManager fileManager, URI rootUri) {
    globalScope = new GlobalScope();
    javacContext = new Context();
    javacFileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    astScanner = new AstScanner();
    completor = new Completor();
    this.fileManager = fileManager;
    this.rootUri = rootUri;
  }

  public synchronized void initialize() {
    if (initialized) {
      logger.warning("Project has already been initalized.");
      return;
    }
    initialized = true;

    fileManager.setFileChangeListener(new ProjectFileChangeListener());
    try {
      Files.walk(Paths.get(rootUri))
          .forEach(
              filePath -> {
                if (isJavaFile(filePath)) {
                  addOrUpdateFile(filePath);
                }
              });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addOrUpdateFile(Path filePath) {
    Optional<CharSequence> content = fileManager.getFileContent(filePath);
    if (content.isPresent()) {
      FileScope fileScope =
          astScanner.startScan(parseFile(filePath.toString(), content.get()), filePath.toString());
      globalScope.addOrReplaceFileScope(fileScope);
    }
  }

  private void removeFile(Path filePath) {
    globalScope.removeFile(filePath);
  }

  /**
   * @param input UTF-8 encoded input
   * @param line 1-based line number
   * @param column 1-based byte offset of the line
   */
  public List<CompletionCandidate> getCompletionCandidates(
      String filename, byte[] inputBytes, int line, int column) {
    StringBuilder inputBuilder = new StringBuilder();
    int startOffset = 0;
    int endOffset = 0;
    for (int currentLine = 0;
        currentLine < line && startOffset < inputBytes.length;
        currentLine++) {
      while (endOffset < inputBytes.length && inputBytes[endOffset] != '\n') {
        endOffset++;
      }
      inputBuilder.append(
          new String(Arrays.copyOfRange(inputBytes, startOffset, endOffset), UTF_8));
      startOffset = endOffset;
    }
    if (inputBytes.length - startOffset < column) {
      // Malformed request
      return ImmutableList.of();
    }
    String targetLine =
        new String(Arrays.copyOfRange(inputBytes, startOffset, startOffset + column), UTF_8);
    inputBuilder.append(targetLine);

    JCCompilationUnit completionUnit = parseFile(filename, inputBuilder.toString());
    FileScope inputFileScope = astScanner.startScan(completionUnit, filename);
    return completor.getCompletionCandidates(
        globalScope, inputFileScope, completionUnit, filename, inputBuilder.toString());
  }

  private JCCompilationUnit parseFile(String filename, CharSequence content) {
    SourceFileObject sourceFileObject = new SourceFileObject(filename);
    Log javacLog = Log.instance(javacContext);
    javacLog.useSource(sourceFileObject);

    // Create a parser and start parsing.
    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                content, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
    return parser.parseCompilationUnit();
  }

  public GlobalScope getGlobalScope() {
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
