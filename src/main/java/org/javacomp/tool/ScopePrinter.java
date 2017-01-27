package org.javacomp.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.project.Project;

public class ScopePrinter {
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private final String filename;
  private final Project project;

  public ScopePrinter(String filename) {
    this.filename = filename;
    this.project = new Project();
  }

  public void parse() {
    project.addFile(filename);
    printScope(project.getGlobalScope(), 0 /* scope */);
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
      for (MethodEntity.Overload overload : methodEntity.getOverloads()) {
        sb.append('\n');
        sb.append(generateIndent(indent + 4));
        sb.append('(');
        boolean firstParameter = true;
        for (MethodEntity.Parameter parameter : overload.getParameters()) {
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
        TypeReference returnType = overload.getReturnType();
        sb.append(formatTypeReference(returnType));
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
