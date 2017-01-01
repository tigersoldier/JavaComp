package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The index of the whole project. Can reach all scoped symbols (e.g. packages, classes) defined in
 * the project.
 */
public class GlobalIndex implements SymbolIndex {
  // Map of simple names -> FileIndex that defines the name.
  private final Multimap<String, FileIndex> nameToFileMap;
  // Map of filename -> FileIndex.
  private final Map<String, FileIndex> fileIndexMap;
  private final PackageIndex rootPackage;

  public GlobalIndex() {
    this.nameToFileMap = HashMultimap.create();
    this.fileIndexMap = new HashMap<>();
    this.rootPackage = new PackageIndex();
  }

  @Override
  public List<Symbol> getSymbolsWithName(final String simpleName) {
    return FluentIterable.from(nameToFileMap.get(simpleName))
        .transformAndConcat(fileIndex -> fileIndex.getGlobalSymbolsWithName(simpleName))
        .append(rootPackage.getSymbolsWithName(simpleName))
        .toList();
  }

  @Override
  public Optional<Symbol> getSymbolWithNameAndKind(String simpleName, Symbol.Kind symbolKind) {
    for (Symbol symbol : getSymbolsWithName(simpleName)) {
      if (symbol.getKind() == symbolKind) {
        return Optional.of(symbol);
      }
    }
    return Optional.absent();
  }

  @Override
  public Multimap<String, Symbol> getAllSymbols() {
    return FluentIterable.from(fileIndexMap.values())
        .transformAndConcat(fileIndex -> fileIndex.getGlobalSymbols().values())
        .append(rootPackage.getAllSymbols().values())
        .index(symbol -> symbol.getSimpleName());
  }

  @Override
  public Multimap<String, Symbol> getMemberSymbols() {
    return rootPackage.getMemberSymbols();
  }

  @Override
  public void addSymbol(Symbol symbol) {
    throw new UnsupportedOperationException();
  }

  public void addOrReplaceFileIndex(String filename, FileIndex fileIndex) {
    FileIndex existingFileIndex = fileIndexMap.get(filename);
    // Add the new file index to the package first, so that we don't GC the pacakge if
    // the new file and old file are in the same pacakge and is the only file in the package.
    addFileToPackage(fileIndex);

    if (existingFileIndex != null) {
      // Remove old symbol indexes.
      for (String symbolName : existingFileIndex.getGlobalSymbols().keys()) {
        nameToFileMap.remove(symbolName, existingFileIndex);
      }
      removeFileFromPacakge(existingFileIndex);
    }
    fileIndexMap.put(filename, fileIndex);
    for (String symbolName : fileIndex.getGlobalSymbols().keys()) {
      nameToFileMap.put(symbolName, fileIndex);
    }
  }

  @Nullable
  public FileIndex getFileIndex(String filename) {
    return fileIndexMap.get(filename);
  }

  private void addFileToPackage(FileIndex fileIndex) {
    List<String> currentQualifiers = new ArrayList<>();
    PackageIndex currentPackage = rootPackage;
    for (String qualifier : fileIndex.getPackageQualifiers()) {
      Optional<Symbol> packageSymbol =
          currentPackage.getSymbolWithNameAndKind(qualifier, Symbol.Kind.QUALIFIER);
      if (packageSymbol.isPresent()) {
        currentPackage = ((PackageSymbol) packageSymbol.get()).getChildIndex();
      } else {
        PackageIndex packageIndex = new PackageIndex();
        currentPackage.addSymbol(new PackageSymbol(qualifier, currentQualifiers, packageIndex));
        currentPackage = packageIndex;
      }
      currentQualifiers.add(qualifier);
    }
    currentPackage.addFile(fileIndex);
  }

  private void removeFileFromPacakge(FileIndex fileIndex) {
    Deque<PackageSymbol> stack = new ArrayDeque<>();
    PackageIndex currentPackage = rootPackage;
    for (String qualifier : fileIndex.getPackageQualifiers()) {
      Optional<Symbol> optionalPackageSymbol =
          currentPackage.getSymbolWithNameAndKind(qualifier, Symbol.Kind.QUALIFIER);
      if (!optionalPackageSymbol.isPresent()) {
        throw new RuntimeException("Package " + qualifier + " not found");
      }
      PackageSymbol packageSymbol = (PackageSymbol) optionalPackageSymbol.get();
      stack.addFirst(packageSymbol);
      currentPackage = packageSymbol.getChildIndex();
    }
    currentPackage.removeFile(fileIndex);
    while (!currentPackage.hasChildren() && !stack.isEmpty()) {
      PackageSymbol packageSymbol = stack.removeFirst();
      currentPackage = stack.isEmpty() ? rootPackage : stack.peekFirst().getChildIndex();
      currentPackage.removePackage(packageSymbol);
    }
  }
}
