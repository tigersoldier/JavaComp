package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a class, interface, enum, or annotation. */
public class ClassSymbol extends Symbol {
  private static final Set<Symbol.Kind> ALLOWED_KINDS =
      EnumSet.of(
          Symbol.Kind.CLASS, Symbol.Kind.INTERFACE, Symbol.Kind.ANNOTATION, Symbol.Kind.ENUM);
  private final ClassIndex classIndex;

  public ClassSymbol(
      String simpleName, Symbol.Kind kind, List<String> qualifiers, ClassIndex classIndex) {
    super(simpleName, kind, qualifiers);
    checkArgument(
        ALLOWED_KINDS.contains(kind),
        "Invalid symbol kind %s, allowed kinds are %s",
        kind,
        ALLOWED_KINDS);
    this.classIndex = classIndex;
  }

  @Override
  public ClassIndex getChildIndex() {
    return classIndex;
  }
}
