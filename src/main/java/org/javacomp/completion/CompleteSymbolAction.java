package org.javacomp.completion;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;

/** An action that returns any visible symbols as completion candidates. */
class CompleteSymbolAction implements CompletionAction {
  @Override
  public Multimap<String, Symbol> getVisibleSymbols(
      GlobalIndex globalIndex, SymbolIndex completionPointIndex) {
    ImmutableMultimap.Builder<String, Symbol> symbolMapBuilder = new ImmutableMultimap.Builder<>();
    symbolMapBuilder.putAll(globalIndex.getAllSymbols());
    symbolMapBuilder.putAll(completionPointIndex.getAllSymbols());
    return symbolMapBuilder.build();
  }
}
