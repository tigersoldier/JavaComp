package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a variable. */
public class VariableEntity extends Entity {
  private static final Set<Entity.Kind> ALLOWED_KINDS =
      EnumSet.of(Entity.Kind.VARIABLE, Entity.Kind.FIELD);

  private final TypeReference type;

  public VariableEntity(
      String simpleName, Entity.Kind kind, List<String> qualifiers, TypeReference type) {
    super(simpleName, kind, qualifiers);
    checkArgument(ALLOWED_KINDS.contains(kind), "Kind %s is not allowed for variables.", kind);
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
