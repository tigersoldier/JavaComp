package org.javacomp.tool;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.FileContentFixer;
import org.javacomp.parser.ParserContext;

public class ScopePrinter {
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private static final Joiner LINE_JOINER = Joiner.on("\n");
  private final ParserContext parserContext;
  private final AstScanner astScanner;
  private final FileContentFixer fileContentFixer;

  public ScopePrinter() {
    this.parserContext = new ParserContext();
    this.astScanner = new AstScanner(IndexOptions.FULL_INDEX_BUILDER.build());
    this.fileContentFixer = new FileContentFixer(this.parserContext);
  }

  public void parse(String filename, boolean fixFileContent) {
    CharSequence content;
    try {
      content = LINE_JOINER.join(Files.readAllLines(Paths.get(filename)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (fixFileContent) {
      content = fileContentFixer.fixFileContent(content).getContent();
      System.out.println("Fixed content:");
      System.out.println(content);
    }
    FileScope fileScope =
        astScanner.startScan(parserContext.parse(filename, content), filename, content);
    printScope(fileScope, 0 /* scope */);
  }

  private static String formatEntity(Entity entity, int indent) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    sb.append(generateIndent(indent));
    sb.append(entity.getSimpleName());
    sb.append(": ");
    sb.append(entity.getKind());
    if (entity instanceof ClassEntity) {
      ClassEntity classEntity = (ClassEntity) entity;
      Optional<TypeReference> superClass = classEntity.getSuperClass();
      if (superClass.isPresent()) {
        sb.append(" extends ");
        sb.append(formatTypeReference(superClass.get()));
      }
      boolean hasInterface = false;
      for (TypeReference iface : classEntity.getInterfaces()) {
        if (!hasInterface) {
          hasInterface = true;
          sb.append(" implements ");
        }
        System.out.print(formatTypeReference(iface));
      }
    } else if (entity instanceof MethodEntity) {
      MethodEntity methodEntity = (MethodEntity) entity;
      sb.append('(');
      boolean firstParameter = true;
      for (VariableEntity parameter : methodEntity.getParameters()) {
        if (!firstParameter) {
          sb.append(", ");
        } else {
          firstParameter = false;
        }
        TypeReference parameterType = parameter.getType();
        sb.append(formatTypeReference(parameterType));
        sb.append(' ');
        sb.append(parameter.getSimpleName());
      }
      sb.append(")->");
      TypeReference returnType = methodEntity.getReturnType();
      sb.append(formatTypeReference(returnType));
    } else if (entity instanceof VariableEntity) {
      VariableEntity variableEntity = (VariableEntity) entity;
      sb.append(": ");
      sb.append(formatTypeReference(variableEntity.getType()));
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

  private static void printScope(EntityScope scope, int indent) {
    Joiner joiner = Joiner.on(".");
    for (Entity entity : scope.getMemberEntities().values()) {
      System.out.println(formatEntity(entity, indent));
      // VariableEntity's child scope is the same as its parent scope, which is being visited.
      if (!(entity instanceof VariableEntity)) {
        printScope(entity.getScope(), indent + 2);
      }
    }
  }

  private static String formatTypeReference(TypeReference typeReference) {
    return typeReference.toDisplayString();
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      printHelp();
    }
    boolean fixFileContent = false;
    String filename;
    if ("-f".equals(args[0])) {
      if (args.length < 2) {
        printHelp();
      }
      fixFileContent = true;
      filename = args[1];
    } else {
      filename = args[0];
    }
    new ScopePrinter().parse(filename, fixFileContent);
  }

  private static void printHelp() {
    System.out.println("Usage: ScopePrinter [-f] <file path>");
    System.exit(1);
  }
}
