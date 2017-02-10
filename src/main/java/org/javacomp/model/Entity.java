package org.javacomp.model;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A Java entity declared in the source code.
 *
 * <p>A entity can be a leaf node (e.g variables) or a scope with more entities within it (e.g.
 * class, method).
 */
public abstract class Entity {

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
    // A psuedo entity kind. Represents the a reference to a entity by name. May be resolved to its referencing entity.
    REFERENCE,
    // A premitive type.
    PRIMITIVE,
    ;
  }

  private final String simpleName;
  private final List<String> qualifiers;
  private final Kind kind;

  protected Entity(String simpleName, Kind kind, List<String> qualifiers) {
    this.simpleName = simpleName;
    this.kind = kind;
    this.qualifiers = ImmutableList.copyOf(qualifiers);
  }

  /**
   * Gets the name of a entity without qualifiers. For example the simple name of foo.bar.ClassName
   * is ClassName.
   */
  public String getSimpleName() {
    return simpleName;
  }

  /** Gets the qualifiers of the name of the entity. */
  public List<String> getQualifiers() {
    return qualifiers;
  }

  public Kind getKind() {
    return kind;
  }

  /**
   * @return a {@link EntityScope} that can be used to find entities visible to the scope of the
   *     entity.
   */
  public abstract EntityScope getChildScope();
}
