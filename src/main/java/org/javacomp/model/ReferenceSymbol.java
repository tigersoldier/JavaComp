package org.javacomp.model;

import java.util.List;

/** Represents a name referencing a symbol. */
public class ReferenceSymbol extends Symbol {
  private Symbol reference; // The symbol it references to.

  public ReferenceSymbol(String simpleName, List<String> qualifiers) {
    super(simpleName, Symbol.Kind.REFERENCE, qualifiers);
    this.reference = null;
  }

  /**
   * @return If reference is resolved, return the child index of the referenced symbol. Otherwise
   *     return a {@Link LeafIndex}.
   */
  @Override
  public SymbolIndex getChildIndex() {
    if (reference == null) {
      return LeafIndex.INSTANCE;
    } else {
      return reference.getChildIndex();
    }
  }
}
