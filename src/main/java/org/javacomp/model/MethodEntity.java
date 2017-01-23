package org.javacomp.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** Represents a method. */
public class MethodEntity extends Entity {
  private final List<Overload> overloads;

  public MethodEntity(String simpleName, List<String> qualifiers) {
    super(simpleName, Entity.Kind.METHOD, qualifiers);
    this.overloads = new ArrayList<>();
  }

  @Override
  public EntityScope getChildScope() {
    // Unknown scope until we know the overloading.
    return EmptyScope.INSTANCE;
  }

  public ImmutableList<Overload> getOverloads() {
    return ImmutableList.copyOf(overloads);
  }

  public void addOverload(Overload overload) {
    this.overloads.add(overload);
  }

  @AutoValue
  public abstract static class Overload {
    public abstract MethodScope getMethodScope();

    public abstract TypeReference getReturnType();

    public abstract ImmutableList<Parameter> getParameters();

    public static Overload create(
        MethodScope methodScope, TypeReference returnType, List<Parameter> parameters) {
      return new AutoValue_MethodEntity_Overload(
          methodScope, returnType, ImmutableList.copyOf(parameters));
    }
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
