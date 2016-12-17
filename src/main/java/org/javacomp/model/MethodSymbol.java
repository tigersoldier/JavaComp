package org.javacomp.model;

import java.util.List;
import java.util.ArrayList;

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

  public void addOverload(MethodIndex methodIndex) {
    this.overloads.add(new Overload(methodIndex));
  }

  private static class Overload {
    private final MethodIndex methodIndex;

    private Overload(MethodIndex methodIndex) {
      this.methodIndex = methodIndex;
    }
  }
}
