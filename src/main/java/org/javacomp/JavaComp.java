package org.javacomp;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
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
import org.javacomp.proto.SymbolProto.Symbol;

public class JavaComp {
  private final String filename;

  public JavaComp(String filename) {
    this.filename = filename;
  }

  public void parse() {
    String input;
    try {
      input = new String(Files.readAllBytes(Paths.get(filename)), UTF_8);
      Context context = new Context();
      JavacFileManager fileManager = new JavacFileManager(context, true /* register */, UTF_8);
      JavacParser parser =
          ParserFactory.instance(context)
              .newParser(
                  input, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
      JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
      new AstScanner().scan(compilationUnit, SymbolIndexScope.GLOBAL_SCOPE);
      printScope(SymbolIndexScope.GLOBAL_SCOPE, 0);
    } catch (IOException e) {
      System.exit(1);
    }
  }

  private static String formatSymbol(Symbol symbol) {
    StringBuilder sb = new StringBuilder();
    if (symbol.getAccessLevel() != Symbol.AccessLevel.ACCESS_LEVEL_UNKNOWN) {
      sb.append(symbol.getAccessLevel());
      sb.append(' ');
    }
    sb.append(symbol.getType().name());
    sb.append(' ');
    boolean first = true;
    for (String qualifier : symbol.getQualifierList()) {
      if (first) {
        first = false;
      } else {
        sb.append('.');
      }
      sb.append(qualifier);
    }
    if (!first) {
      sb.append('.');
    }
    sb.append(symbol.getSimpleName());
    return sb.toString();
  }

  private static void printScope(SymbolIndexScope scope, int index) {
    Joiner joiner = Joiner.on(".");
    for (int i = 0; i < index; i++) {
      System.out.print(' ');
    }
    System.out.println(formatSymbol(scope.getSymbol()));
    for (SymbolIndexScope child : scope.getAllScopes()) {
      printScope(child, index + 2);
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Need at least one param.");
      System.exit(1);
    }
    new JavaComp(args[0]).parse();
  }
}
