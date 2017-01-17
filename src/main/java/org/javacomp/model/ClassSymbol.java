package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Represents a class, interface, enum, or annotation. */
public class ClassSymbol extends Symbol implements SymbolIndex {
  private static final Set<Symbol.Kind> ALLOWED_KINDS =
      EnumSet.of(
          Symbol.Kind.CLASS, Symbol.Kind.INTERFACE, Symbol.Kind.ANNOTATION, Symbol.Kind.ENUM);

  // Map of simple names -> symbols.
  private final Multimap<String, Symbol> symbols;
  private final SymbolIndex parentIndex;
  private final Optional<TypeReference> superClass;
  private final ImmutableList<TypeReference> interfaces;

  public ClassSymbol(
      String simpleName,
      Symbol.Kind kind,
      List<String> qualifiers,
      SymbolIndex parentIndex,
      Optional<TypeReference> superClass,
      ImmutableList<TypeReference> interfaces) {
    super(simpleName, kind, qualifiers);
    checkArgument(
        ALLOWED_KINDS.contains(kind),
        "Invalid symbol kind %s, allowed kinds are %s",
        kind,
        ALLOWED_KINDS);
    this.symbols = HashMultimap.create();
    this.parentIndex = parentIndex;
    this.superClass = superClass;
    this.interfaces = ImmutableList.copyOf(interfaces);
  }

  @Override
  public ClassSymbol getChildIndex() {
    return this;
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

  public ImmutableList<TypeReference> getInterfaces() {
    return interfaces;
  }

  public Optional<TypeReference> getSuperClass() {
    return superClass;
  }

  @Override
  public Optional<SymbolIndex> getParentIndex() {
    return Optional.of(parentIndex);
  }
}
