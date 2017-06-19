package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Range;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a variable. */
public class VariableEntity extends Entity {
  public static final Set<Entity.Kind> ALLOWED_KINDS =
      EnumSet.of(Entity.Kind.VARIABLE, Entity.Kind.FIELD);

  private final TypeReference type;
  private final EntityScope parentScope;

  public VariableEntity(
      String simpleName,
      Entity.Kind kind,
      List<String> qualifiers,
      boolean isStatic,
      TypeReference type,
      EntityScope parentScope,
      Range<Integer> variableNameRange) {
    super(simpleName, kind, qualifiers, isStatic, variableNameRange);
    checkArgument(ALLOWED_KINDS.contains(kind), "Kind %s is not allowed for variables.", kind);
    this.type = type;
    this.parentScope = parentScope;
  }

  public TypeReference getType() {
    return type;
  }

  @Override
  public EntityScope getChildScope() {
    return parentScope;
  }

  public EntityScope getParentScope() {
    return parentScope;
  }

  @Override
  public String toString() {
    return "VariableEntity<" + getType().getSimpleName() + ' ' + getSimpleName() + ">";
  }
}
