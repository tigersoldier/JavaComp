package org.javacomp.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** Represents a method. */
public class MethodSymbol extends Symbol {
  private final List<Overload> overloads;

  public MethodSymbol(String simpleName, List<String> qualifiers) {
    super(simpleName, Symbol.Kind.METHOD, qualifiers);
    this.overloads = new ArrayList<>();
  }

  @Override
  public SymbolIndex getChildIndex() {
    // Unknown index until we know the overloading.
    return LeafIndex.INSTANCE;
  }

  public ImmutableList<Overload> getOverloads() {
    return ImmutableList.copyOf(overloads);
  }

  public void addOverload(Overload overload) {
    this.overloads.add(overload);
  }

  @AutoValue
  public abstract static class Overload {
    public abstract MethodIndex getMethodIndex();

    public abstract TypeReference getReturnType();

    public abstract ImmutableList<Parameter> getParameters();

    public static Overload create(
        MethodIndex methodIndex, TypeReference returnType, List<Parameter> parameters) {
      return new AutoValue_MethodSymbol_Overload(
          methodIndex, returnType, ImmutableList.copyOf(parameters));
    }
  }

  @AutoValue
  public abstract static class Parameter {
    public abstract TypeReference getType();

    public abstract String getName();

    public static Parameter create(TypeReference type, String name) {
      return new AutoValue_MethodSymbol_Parameter(type, name);
    }
  }
}
