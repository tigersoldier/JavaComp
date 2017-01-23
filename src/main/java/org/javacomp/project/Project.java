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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.javacomp.completion.CompletionCandidate;
import org.javacomp.completion.Completor;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;

/** Handles all files in a project. */
public class Project {
  private final GlobalScope globalScope;
  private final Context javacContext;
  private final JavacFileManager fileManager;
  private final AstScanner astScanner;
  private final Completor completor;

  public Project() {
    globalScope = new GlobalScope();
    javacContext = new Context();
    fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    astScanner = new AstScanner();
    completor = new Completor();
  }

  public void addFile(String filename) {
    try {
      String input = new String(Files.readAllBytes(Paths.get(filename)), UTF_8);

      // Set source file of the log before parsing. If not set, IllegalArgumentException will be
      // thrown if the parser enconters errors.
      FileScope fileScope = astScanner.startScan(parseFile(filename, input), filename);
      globalScope.addOrReplaceFileScope(fileScope);
    } catch (IOException e) {
      System.exit(1);
    }
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

  private JCCompilationUnit parseFile(String filename, String content) {
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
}
