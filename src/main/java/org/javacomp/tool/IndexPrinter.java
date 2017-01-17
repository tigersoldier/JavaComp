package org.javacomp.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import org.javacomp.model.ClassSymbol;
import org.javacomp.model.MethodSymbol;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.javacomp.model.TypeReference;
import org.javacomp.model.util.QualifiedNames;
import org.javacomp.project.Project;

public class IndexPrinter {
  private final String filename;
  private final Project project;

  public IndexPrinter(String filename) {
    this.filename = filename;
    this.project = new Project();
  }

  public void parse() {
    project.addFile(filename);
    printIndex(project.getGlobalIndex(), 0 /* index */);
  }

  private static String formatSymbol(Symbol symbol, int indent) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    sb.append(generateIndent(indent));
    sb.append(symbol.getSimpleName());
    sb.append(": ");
    sb.append(symbol.getKind());
    if (symbol instanceof ClassSymbol) {
      ClassSymbol classSymbol = (ClassSymbol) symbol;
      Optional<TypeReference> superClass = classSymbol.getSuperClass();
      if (superClass.isPresent()) {
        sb.append(" extends ");
        sb.append(
            QualifiedNames.formatQualifiedName(
                superClass.get().getQualifiers(), superClass.get().getSimpleName()));
      }
      boolean hasInterface = false;
      for (TypeReference iface : classSymbol.getInterfaces()) {
        if (!hasInterface) {
          hasInterface = true;
          sb.append(" implements ");
        }
        sb.append(QualifiedNames.formatQualifiedName(iface.getQualifiers(), iface.getSimpleName()));
      }
    } else if (symbol instanceof MethodSymbol) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      for (MethodSymbol.Overload overload : methodSymbol.getOverloads()) {
        sb.append('\n');
        sb.append(generateIndent(indent + 4));
        sb.append('(');
        boolean firstParameter = true;
        for (MethodSymbol.Parameter parameter : overload.getParameters()) {
          if (!firstParameter) {
            sb.append(", ");
          } else {
            firstParameter = false;
          }
          TypeReference parameterType = parameter.getType();
          sb.append(
              QualifiedNames.formatQualifiedName(
                  parameterType.getQualifiers(), parameterType.getSimpleName()));
          sb.append(' ');
          sb.append(parameter.getName());
        }
        sb.append(")->");
        TypeReference returnType = overload.getReturnType();
        sb.append(
            QualifiedNames.formatQualifiedName(
                returnType.getQualifiers(), returnType.getSimpleName()));
      }
    }
    return sb.toString();
  }

  private static StringBuilder generateIndent(int indent) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indent; i++) {
      sb.append(' ');
    }
    return sb;
  }

  private static void printIndex(SymbolIndex index, int indent) {
    Joiner joiner = Joiner.on(".");
    for (Symbol symbol : index.getMemberSymbols().values()) {
      System.out.println(formatSymbol(symbol, indent));
      printIndex(symbol.getChildIndex(), indent + 2);
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Need at least one param.");
      System.exit(1);
    }
    new IndexPrinter(args[0]).parse();
  }
}
