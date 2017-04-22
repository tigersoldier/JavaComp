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
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.ParserContext;

public class ScopePrinter {
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private static final Joiner LINE_JOINER = Joiner.on("\n");
  private final String filename;
  private final ParserContext parserContext;
  private final AstScanner astScanner;

  public ScopePrinter(String filename) {
    this.filename = filename;
    this.parserContext = new ParserContext();
    this.astScanner = new AstScanner();
  }

  public void parse() {
    String content;
    try {
      content = LINE_JOINER.join(Files.readAllLines(Paths.get(filename)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FileScope fileScope = astScanner.startScan(parserContext.parse(filename, content), filename);
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
      for (MethodEntity.Parameter parameter : methodEntity.getParameters()) {
        if (!firstParameter) {
          sb.append(", ");
        } else {
          firstParameter = false;
        }
        TypeReference parameterType = parameter.getType();
        sb.append(formatTypeReference(parameterType));
        sb.append(' ');
        sb.append(parameter.getName());
      }
      sb.append(")->");
      TypeReference returnType = methodEntity.getReturnType();
      sb.append(formatTypeReference(returnType));
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
      printScope(entity.getChildScope(), indent + 2);
    }
  }

  private static String formatTypeReference(TypeReference typeReference) {
    return QUALIFIER_JOINER.join(typeReference.getFullName());
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Need at least one param.");
      System.exit(1);
    }
    new ScopePrinter(args[0]).parse();
  }
}
