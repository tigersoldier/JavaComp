package org.javacomp.model;

import java.util.List;

/** Represents a variable. */
public class VariableEntity extends Entity {

  private final TypeReference type;

  public VariableEntity(String simpleName, List<String> qualifiers, TypeReference type) {
    super(simpleName, Entity.Kind.VARIABLE, qualifiers);
    this.type = type;
  }

  public TypeReference getType() {
    return type;
  }

  @Override
  public EmptyScope getChildScope() {
    return EmptyScope.INSTANCE;
  }
}
