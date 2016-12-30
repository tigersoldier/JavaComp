package org.javacomp.model;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Represents an index node with a name.
 *
 * <p>A symbol can be a leaf node (e.g variables) or a scope with more symbols within it (e.g.
 * class, method).
 */
public abstract class Symbol {

  public enum Kind {
    CLASS,
    INTERFACE,
    ANNOTATION,
    ENUM,
    METHOD,
    VARIABLE,
    // Each part of a pacakage qualifier
    // e.g org.javacomp has 2 qualifiers: org and javacomp
    QUALIFIER,
    // A psuedo symbol kind. Represents the a reference to a symbol by name. May be resolved to its referencing symbol.
    REFERENCE,
    ;
  }

  private final String simpleName;
  private final List<String> qualifiers;
  private final Kind kind;

  protected Symbol(String simpleName, Kind kind, List<String> qualifiers) {
    this.simpleName = simpleName;
    this.kind = kind;
    this.qualifiers = ImmutableList.copyOf(qualifiers);
  }

  /**
   * Gets the name of a symbol without qualifiers. For example the simple name of foo.bar.ClassName
   * is ClassName.
   */
  public String getSimpleName() {
    return simpleName;
  }

  /** Gets the qualifiers of the name of the symbol. */
  public List<String> getQualifiers() {
    return qualifiers;
  }

  public Kind getKind() {
    return kind;
  }

  /**
   * @return a {@link SymbolIndex} that can be used to find symbols visible to the scope of the
   *     symbol.
   */
  public abstract SymbolIndex getChildIndex();
}
