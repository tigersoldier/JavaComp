package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.RangeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Index of symbols in the scope of a Java source file. */
public class FileIndex implements SymbolIndex {
  private final String filename;
  // Map of simple names -> symbols.
  private final Multimap<String, Symbol> symbols;
  // Simples that can be reached globally.
  // Map of simple names -> symbols.
  private final Multimap<String, Symbol> globalSymbols;
  private final ImmutableList<String> packageQualifiers;
  private final Map<String, List<String>> importedClasses;
  private final List<List<String>> onDemandClassImportQualifiers;
  private RangeMap<Integer, SymbolIndex> indexRangeMap = null;

  public FileIndex(String filename, List<String> packageQualifiers) {
    this.filename = filename;
    this.symbols = HashMultimap.create();
    this.packageQualifiers = ImmutableList.copyOf(packageQualifiers);
    this.globalSymbols = HashMultimap.create();
    this.importedClasses = new HashMap<>();
    this.onDemandClassImportQualifiers = new ArrayList<>();
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
  public Multimap<String, Symbol> getMemberSymbols() {
    return ImmutableMultimap.copyOf(symbols);
  }

  public Optional<List<String>> getImportedClass(String simpleName) {
    return Optional.fromNullable(importedClasses.get(simpleName));
  }

  public void addImportedClass(List<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return;
    }
    importedClasses.put(qualifiers.get(qualifiers.size() - 1), qualifiers);
  }

  /**
   * Returns a list of all on-demand class imported qualifiers added by {@link
   * #addOnDemandClassImport}.
   *
   * <p>Similar to {@link #addOnDemandClassImport}, the returned qualifiers do not include the
   * trailing *.
   */
  public List<List<String>> getOnDemandClassImportQualifiers() {
    return ImmutableList.copyOf(onDemandClassImportQualifiers);
  }

  /**
   * Adds an on-demand class import (e.g. {@code import foo.bar.*}).
   *
   * @param qualifiers the imported package qualifiers without *. For example, if the import
   *     statment is {@code import foo.bar.*}, then the qualifiers are {@code ['foo', 'bar']}
   */
  public void addOnDemandClassImport(List<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return;
    }
    onDemandClassImportQualifiers.add(ImmutableList.copyOf(qualifiers));
  }

  @Override
  public void addSymbol(Symbol symbol) {
    symbols.put(symbol.getSimpleName(), symbol);
  }

  public void setIndexRangeMap(RangeMap<Integer, SymbolIndex> indexRangeMap) {
    this.indexRangeMap = indexRangeMap;
  }

  public RangeMap<Integer, SymbolIndex> getIndexRangeMap() {
    return indexRangeMap;
  }

  @Nullable
  public SymbolIndex getSymbolIndexAt(int position) {
    return indexRangeMap.get(position);
  }

  public void addGlobalSymbol(Symbol symbol) {
    globalSymbols.put(symbol.getSimpleName(), symbol);
  }

  /** @return a multimap of symbol simple name to symbols */
  public Multimap<String, Symbol> getGlobalSymbols() {
    return ImmutableMultimap.copyOf(globalSymbols);
  }

  public List<Symbol> getGlobalSymbolsWithName(String simpleName) {
    return ImmutableList.copyOf(globalSymbols.get(simpleName));
  }

  public List<String> getPackageQualifiers() {
    return packageQualifiers;
  }

  @Override
  public Optional<SymbolIndex> getParentIndex() {
    return Optional.absent();
  }

  public String getFilename() {
    return filename;
  }
}
