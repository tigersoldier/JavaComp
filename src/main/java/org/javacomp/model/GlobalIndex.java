package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/**
 * The index of the whole project. Can reach all scoped symbols (e.g. packages, classes) defined in
 * the project.
 */
public class GlobalIndex implements SymbolIndex {
  // Map of simple names -> symbols.
  private final Multimap<String, Symbol> symbols;

  public GlobalIndex() {
    this.symbols = HashMultimap.create();
  }

  @Override
  public List<Symbol> getSymbolsWithName(String simpleName) {
    return ImmutableList.copyOf(symbols.get(simpleName));
  }

  @Override
  public Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind) {
    for (Symbol symbol : symbols.get(simpleName)) {
      if (symbol.getKind() == symbolKind) {
        return Optional.of(symbol);
      }
    }
    return Optional.absent();
  }

  @Override
  public Multimap<String, Symbol> getAllSymbols() {
    return ImmutableMultimap.copyOf(symbols);
  }

  @Override
  public void addSymbol(Symbol symbol) {
    symbols.put(symbol.getSimpleName(), symbol);
  }
}
