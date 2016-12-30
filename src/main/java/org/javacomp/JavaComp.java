package org.javacomp;


import com.google.common.base.Joiner;
import java.io.IOException;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.javacomp.project.Project;

public class JavaComp {
  private final String filename;
  private final Project project;

  public JavaComp(String filename) {
    this.filename = filename;
    this.project = new Project();
  }

  public void parse() {
    project.addFile(filename);
    printIndex(project.getGlobalIndex(), 0 /* index */);
  }

  private static String formatSymbol(Symbol symbol) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String qualifier : symbol.getQualifiers()) {
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

  private static void printIndex(SymbolIndex index, int indent) {
    Joiner joiner = Joiner.on(".");
    for (int i = 0; i < indent; i++) {
      System.out.print(' ');
    }
    for (Symbol symbol : index.getAllSymbols().values()) {
      System.out.println(formatSymbol(symbol));
      printIndex(symbol.getChildIndex(), indent + 2);
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