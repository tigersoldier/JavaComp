package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/** An index containing no symbol. */
public class LeafIndex implements SymbolIndex {
  public static final LeafIndex INSTANCE = new LeafIndex();

  private LeafIndex() {}

  @Override
  public List<Symbol> getSymbolsWithName(String simpleName) {
    return ImmutableList.of();
  }

  @Override
  public Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind) {
    return Optional.absent();
  }

  @Override
  public Multimap<String, Symbol> getAllSymbols() {
    return ImmutableMultimap.of();
  }

  @Override
  public Multimap<String, Symbol> getMemberSymbols() {
    return ImmutableMultimap.of();
  }

  @Override
  public void addSymbol(Symbol symbol) {
    throw new UnsupportedOperationException("No symbol is allowed to be added to a LeafIndex.");
  }

  @Override
  public Optional<SymbolIndex> getParentIndex() {
    return Optional.absent();
  }
}
