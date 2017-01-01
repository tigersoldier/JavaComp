package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/** The index of symbols in the scope of a class, interface, enum, or annotation. */
public class ClassIndex implements SymbolIndex {
  // Map of simple names -> symbols.
  private final Multimap<String, Symbol> symbols;
  private final SymbolIndex parentIndex;

  public ClassIndex(SymbolIndex parentIndex) {
    this.symbols = HashMultimap.create();
    this.parentIndex = parentIndex;
  }

  @Override
  public List<Symbol> getSymbolsWithName(String simpleName) {
    // TODO: check imports.
    // TODO: check super class and interfaces
    ImmutableList.Builder<Symbol> builder = new ImmutableList.Builder<>();
    builder.addAll(symbols.get(simpleName));
    builder.addAll(parentIndex.getSymbolsWithName(simpleName));
    return builder.build();
  }

  @Override
  public Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind) {
    for (Symbol symbol : symbols.get(simpleName)) {
      if (symbol.getKind() == symbolKind) {
        return Optional.of(symbol);
      }
    }
    // TODO: check imports.
    // TODO: check super class and interfaces
    return parentIndex.getSymbolWithNameAndKind(simpleName, symbolKind);
  }

  @Override
  public Multimap<String, Symbol> getAllSymbols() {
    ImmutableMultimap.Builder<String, Symbol> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(symbols);
    builder.putAll(parentIndex.getAllSymbols());
    // TODO: check imports.
    // TODO: check super class and interfaces
    return builder.build();
  }

  @Override
  public Multimap<String, Symbol> getMemberSymbols() {
    return ImmutableMultimap.copyOf(symbols);
  }

  @Override
  public void addSymbol(Symbol symbol) {
    symbols.put(symbol.getSimpleName(), symbol);
  }
}
