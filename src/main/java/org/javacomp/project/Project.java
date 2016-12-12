package org.javacomp.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.javacomp.model.SymbolIndexScope;
import org.javacomp.parser.AstScanner;

/** Handles all files in a project. */
public class Project {
  private final SymbolIndexScope globalScope;
  private final Context javacContext;
  private final JavacFileManager fileManager;
  private final AstScanner astScanner;

  public Project() {
    globalScope = SymbolIndexScope.newGlobalScope();
    javacContext = new Context();
    fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    astScanner = new AstScanner();
  }

  public void addFile(String filename) {
    try {
      String input = new String(Files.readAllBytes(Paths.get(filename)), UTF_8);
      JavacParser parser =
          ParserFactory.instance(javacContext)
              .newParser(
                  input, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
      JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
      astScanner.scan(compilationUnit, globalScope);
    } catch (IOException e) {
      System.exit(1);
    }
  }

  public SymbolIndexScope getGlobalScope() {
    return globalScope;
  }
}
