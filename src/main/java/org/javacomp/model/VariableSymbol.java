package org.javacomp.model;

import java.util.List;

/** Represents a variable. */
public class VariableSymbol extends Symbol {

  public VariableSymbol(String simpleName, List<String> qualifiers) {
    super(simpleName, Symbol.Kind.VARIABLE, qualifiers);
  }

  @Override
  public LeafIndex getChildIndex() {
    return LeafIndex.INSTANCE;
  }
}
