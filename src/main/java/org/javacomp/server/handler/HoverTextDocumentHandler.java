package org.javacomp.server.handler;

import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.project.Project;
import org.javacomp.protocol.Hover;
import org.javacomp.protocol.TextDocumentPositionParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/hover" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textDocument_hover
 */
public class HoverTextDocumentHandler extends RequestHandler<TextDocumentPositionParams> {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final Server server;

  public HoverTextDocumentHandler(Server server) {
    super("textDocument/hover", TextDocumentPositionParams.class);
    this.server = server;
  }

  @Override
  public Hover handleRequest(Request<TextDocumentPositionParams> request) throws Exception {
    TextDocumentPositionParams params = request.getParams();
    Project project = server.getProject();
    List<? extends Entity> definitions =
        project.findDefinitions(
            Paths.get(params.textDocument.uri),
            params.position.getLine(),
            params.position.getCharacter());

    Hover hover = new Hover();

    if (definitions.isEmpty()) {
      hover.contents = ImmutableList.of();
      return hover;
    }

    Entity entity = definitions.get(0);
    Optional<String> languageValue = formatEntity(entity);
    if (!languageValue.isPresent()) {
      hover.contents = ImmutableList.of();
      return hover;
    }

    hover.contents = ImmutableList.of(createLanguageString(languageValue.get()));

    return hover;
  }

  private Hover.LanguageString createLanguageString(String value) {
    Hover.LanguageString languageString = new Hover.LanguageString();
    languageString.value = value;
    languageString.language = "java";
    return languageString;
  }

  private Optional<String> formatEntity(Entity entity) {
    switch (entity.getKind()) {
      case METHOD:
        return Optional.of(formatMethod((MethodEntity) entity));
      case VARIABLE:
      case FIELD:
        return Optional.of(formatVariable((VariableEntity) entity));
      case CLASS:
      case INTERFACE:
      case ANNOTATION:
      case ENUM:
        return Optional.of(formatClass((ClassEntity) entity));
      default:
        return Optional.empty();
    }
  }

  private String formatMethod(MethodEntity method) {
    StringBuilder sb = new StringBuilder();
    if (!method.getTypeParameters().isEmpty()) {
      sb.append("<");
      sb.append(
          method
              .getTypeParameters()
              .stream()
              .map(t -> t.toDisplayString())
              .collect(Collectors.joining(", ")));
      sb.append("> ");
    }
    if (method.getReturnType() != TypeReference.EMPTY_TYPE) {
      sb.append(method.getReturnType().toDisplayString());
      sb.append(" ");
    }

    appendSimpleQualifiers(sb, method.getQualifiers());
    // Constructor name is always <init>, which should not be shown.
    if (!method.isConstructor()) {
      sb.append(".");
      sb.append(method.getSimpleName());
    }
    sb.append("(");
    boolean firstParameter = true;
    for (VariableEntity parameter : method.getParameters()) {
      if (firstParameter) {
        firstParameter = false;
      } else {
        sb.append(", ");
      }
      sb.append(formatVariable(parameter));
    }
    sb.append(")");
    return sb.toString();
  }

  private String formatVariable(VariableEntity variable) {
    StringBuilder sb = new StringBuilder();

    if (variable.getType() != TypeReference.EMPTY_TYPE) {
      sb.append(variable.getType().toDisplayString());
      sb.append(" ");
    }

    if (appendSimpleQualifiers(sb, variable.getQualifiers())) {
      sb.append(".");
    }
    sb.append(variable.getSimpleName());
    return sb.toString();
  }

  /**
   * Append simple qualifiers to a string builder.
   *
   * <p>If any element of {@code qualifiers} starts with a uppercase letter, the first element with
   * uppercase letter and all elements follwing it will be appended to {@code sb}, separated by
   * dots. Otherwise, only the last element of {@code qualifiers} is appended, if it's not empty.
   *
   * @return {@code true} if there is at least one element appended to {@code sb}, e.g. {@code
   *     qualifiers} is not empty
   */
  private boolean appendSimpleQualifiers(StringBuilder sb, List<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return false;
    }

    boolean shouldAppend = false;
    for (String qualifier : qualifiers) {
      if (!shouldAppend && qualifier.length() > 0 && Character.isUpperCase(qualifier.charAt(0))) {
        shouldAppend = true;
        sb.append(qualifier);
        continue;
      }
      if (shouldAppend) {
        sb.append(".");
        sb.append(qualifier);
      }
    }

    if (!shouldAppend) {
      // We got lower case class name :( At least append the last qualifier.
      sb.append(qualifiers.get(qualifiers.size() - 1));
    }

    return true;
  }

  private String formatClass(ClassEntity classEntity) {
    StringBuilder sb = new StringBuilder();
    switch (classEntity.getKind()) {
      case CLASS:
        sb.append("class");
        break;
      case ANNOTATION:
        sb.append("@interface");
        break;
      case INTERFACE:
        sb.append("interface");
        break;
      case ENUM:
        sb.append("enum");
        break;
      default:
        logger.warning("Unknown entity kind for class: %s", classEntity.getKind());
        sb.append(classEntity.getKind().name().toLowerCase());
        break;
    }
    sb.append(" ");
    sb.append(classEntity.getQualifiedName());
    if (!classEntity.getTypeParameters().isEmpty()) {
      sb.append("<");
      sb.append(
          classEntity
              .getTypeParameters()
              .stream()
              .map(t -> t.toDisplayString())
              .collect(Collectors.joining(", ")));
      sb.append(">");
    }

    if (classEntity.getSuperClass().isPresent()) {
      sb.append(" extends ");
      sb.append(classEntity.getSuperClass().get().toDisplayString());
    }

    if (!classEntity.getInterfaces().isEmpty()) {
      sb.append(" implements ");
      boolean isFirstIface = true;
      for (TypeReference iface : classEntity.getInterfaces()) {
        if (isFirstIface) {
          isFirstIface = false;
        } else {
          sb.append(", ");
        }

        sb.append(iface.toDisplayString());
      }
    }

    return sb.toString();
  }
}
