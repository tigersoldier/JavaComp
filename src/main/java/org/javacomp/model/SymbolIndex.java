package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.List;

public interface SymbolIndex {
  List<Symbol> getSymbolsWithName(String simpleName);

  Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind);

  Multimap<String, Symbol> getAllSymbols();

  void addSymbol(Symbol symbol);
}
