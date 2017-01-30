package org.javacomp.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Represents a method. */
public class MethodEntity extends Entity {
  private final MethodScope methodScope;
  private final TypeReference returnType;
  private final List<Parameter> parameters;

  public MethodEntity(
      String simpleName,
      List<String> qualifiers,
      MethodScope methodScope,
      TypeReference returnType,
      List<Parameter> parameters) {
    super(simpleName, Entity.Kind.METHOD, qualifiers);
    this.methodScope = methodScope;
    this.returnType = returnType;
    this.parameters = ImmutableList.copyOf(parameters);
  }

  @Override
  public EntityScope getChildScope() {
    return methodScope;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  public TypeReference getReturnType() {
    return returnType;
  }

  @AutoValue
  public abstract static class Parameter {
    public abstract TypeReference getType();

    public abstract String getName();

    public static Parameter create(TypeReference type, String name) {
      return new AutoValue_MethodEntity_Parameter(type, name);
    }
  }
}
