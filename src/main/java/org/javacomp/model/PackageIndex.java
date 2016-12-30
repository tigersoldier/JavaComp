package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Index of sub packages and files in a package. */
public class PackageIndex implements SymbolIndex {
  // Map of simple names -> subPackages.
  private final Multimap<String, PackageSymbol> subPackages;
  private final Set<FileIndex> files;

  public PackageIndex() {
    this.subPackages = HashMultimap.create();
    this.files = new HashSet<>();
  }

  @Override
  public List<Symbol> getSymbolsWithName(String simpleName) {
    ImmutableList.Builder<Symbol> builder = new ImmutableList.Builder<>();
    builder.addAll(subPackages.get(simpleName));
    for (FileIndex fileIndex : files) {
      builder.addAll(fileIndex.getSymbolsWithName(simpleName));
    }
    return builder.build();
  }

  @Override
  public Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind) {
    for (Symbol symbol : subPackages.get(simpleName)) {
      if (symbol.getKind() == symbolKind) {
        return Optional.of(symbol);
      }
    }
    for (FileIndex fileIndex : files) {
      Optional<Symbol> symbol = fileIndex.getSymbolWithNameAndKind(simpleName, symbolKind);
      if (symbol.isPresent()) {
        return symbol;
      }
    }
    return Optional.absent();
  }

  @Override
  public Multimap<String, Symbol> getAllSymbols() {
    ImmutableMultimap.Builder<String, Symbol> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(subPackages);
    for (FileIndex fileIndex : files) {
      builder.putAll(fileIndex.getAllSymbols());
    }
    return builder.build();
  }

  @Override
  public void addSymbol(Symbol symbol) {
    checkArgument(
        symbol instanceof PackageSymbol,
        "Only sub package can be added to a package. Found " + symbol.getClass().getSimpleName());
    subPackages.put(symbol.getSimpleName(), (PackageSymbol) symbol);
  }

  public void removePackage(PackageSymbol symbol) {
    subPackages.remove(symbol.getSimpleName(), symbol);
  }

  public void addFile(FileIndex fileIndex) {
    files.add(fileIndex);
  }

  public void removeFile(FileIndex fileIndex) {
    files.remove(fileIndex);
  }

  /** @return whether the package has sub packages or files. */
  public boolean hasChildren() {
    return !(subPackages.isEmpty() && files.isEmpty());
  }
}
