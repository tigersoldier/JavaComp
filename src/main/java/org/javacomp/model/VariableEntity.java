package org.javacomp.model;

import java.util.List;

/** Represents a variable. */
public class VariableEntity extends Entity {

  public VariableEntity(String simpleName, List<String> qualifiers) {
    super(simpleName, Entity.Kind.VARIABLE, qualifiers);
  }

  @Override
  public EmptyScope getChildScope() {
    return EmptyScope.INSTANCE;
  }
}
