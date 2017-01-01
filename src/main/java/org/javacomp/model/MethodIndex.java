package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/** Index of symbols in the scope of a method. */
public class MethodIndex implements SymbolIndex {
  // Map of simple names -> symbols.
  private final Multimap<String, Symbol> symbols;
  private final ClassIndex classIndex;

  public MethodIndex(ClassIndex classIndex) {
    this.symbols = HashMultimap.create();
    this.classIndex = classIndex;
  }

  @Override
  public List<Symbol> getSymbolsWithName(String simpleName) {
    ImmutableList.Builder<Symbol> builder = new ImmutableList.Builder<>();
    builder.addAll(symbols.get(simpleName));
    builder.addAll(classIndex.getSymbolsWithName(simpleName));
    // TODO: distinguish between static method and instance method
    return builder.build();
  }

  @Override
  public Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind) {
    for (Symbol symbol : symbols.get(simpleName)) {
      if (symbol.getKind() == symbolKind) {
        return Optional.of(symbol);
      }
    }
    // TODO: distinguish between static method and instance method
    return classIndex.getSymbolWithNameAndKind(simpleName, symbolKind);
  }

  @Override
  public Multimap<String, Symbol> getAllSymbols() {
    ImmutableMultimap.Builder<String, Symbol> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(symbols);
    builder.putAll(classIndex.getAllSymbols());
    // TODO: distinguish between static method and instance method
    return builder.build();
  }

  @Override
  public Multimap<String, Symbol> getMemberSymbols() {
    return ImmutableMultimap.of();
  }

  @Override
  public void addSymbol(Symbol symbol) {
    symbols.put(symbol.getSimpleName(), symbol);
  }
}
