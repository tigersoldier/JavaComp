package org.javacomp;


import com.google.common.base.Joiner;
import java.io.IOException;
import org.javacomp.model.SymbolIndexScope;
import org.javacomp.project.Project;
import org.javacomp.proto.SymbolProto.Symbol;

public class JavaComp {
  private final String filename;
  private final Project project;

  public JavaComp(String filename) {
    this.filename = filename;
    this.project = new Project();
  }

  public void parse() {
    project.addFile(filename);
    printScope(project.getGlobalScope(), 0 /* index */);
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
