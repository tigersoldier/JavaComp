package org.javacomp.completion;

import com.google.common.collect.Multimap;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;

/** Action to perform the requested completion. */
interface CompletionAction {
  public Multimap<String, Symbol> getVisibleSymbols(
      GlobalIndex globalIndex, SymbolIndex completionPointIndex);
}
