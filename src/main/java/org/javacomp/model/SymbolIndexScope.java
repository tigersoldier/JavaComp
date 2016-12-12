package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.javacomp.proto.SymbolProto.Symbol;

public class SymbolIndexScope {

  private final Multimap<String, SymbolIndexScope> index;
  @Nullable private final SymbolIndexScope parentScope;
  // The symbol that opens this scope.
  @Nullable private final Symbol symbol;

  public static SymbolIndexScope newGlobalScope() {
    return new SymbolIndexScope(null /* parentScope */, createPseudoSymbol());
  }

  private SymbolIndexScope(@Nullable SymbolIndexScope parentScope, Symbol symbol) {
    this.parentScope = parentScope;
    this.index = HashMultimap.create();
    this.symbol = symbol;
  }

  public SymbolIndexScope findOrCreatePackageScope(List<String> qualifiers) {
    SymbolIndexScope currentScope = this;
    ArrayList<String> currentQualifiers = new ArrayList<>();
    for (String qualifier : qualifiers) {
      SymbolIndexScope qualifierScope = null;
      for (SymbolIndexScope scope : currentScope.getNamedScopes(qualifier, Symbol.Type.QUALIFIER)) {
        qualifierScope = scope;
        break;
      }
      if (qualifierScope == null) {
        Symbol scopeSymbol =
          Symbol.newBuilder()
          .setType(Symbol.Type.QUALIFIER)
          .setSimpleName(qualifier)
          .addAllQualifier(currentQualifiers)
          .build();
        qualifierScope = new SymbolIndexScope(currentScope, scopeSymbol);
        currentScope.indexScope(qualifierScope);
      }
      currentScope = qualifierScope;
      currentQualifiers.add(qualifier);
    }
    return currentScope;
  }

  public SymbolIndexScope addSymbol(Symbol symbol) {
    SymbolIndexScope symbolScope = indexSymbolSimpleName(symbol);
    if (isTypeDeclaraion(symbol.getType())) {
      getGlobalScope().indexSymbolSimpleName(symbol);
    }
    return symbolScope;
  }

  @Nullable
  public SymbolIndexScope getParentScope() {
    return parentScope;
  }

  public SymbolIndexScope getGlobalScope() {
    SymbolIndexScope currentScope = this;
    while (currentScope.getParentScope() != null) {
      currentScope = currentScope.getParentScope();
    }
    return currentScope;
  }

  public List<SymbolIndexScope> getNamedScopes(
      String symbolSimpleName, Symbol.Type... symbolTypes) {
    Set<Symbol.Type> allowedSymbolTypes;
    if (symbolTypes.length == 0) {
      allowedSymbolTypes = EnumSet.allOf(Symbol.Type.class);
    } else {
      allowedSymbolTypes = ImmutableSet.copyOf(symbolTypes);
    }
    ImmutableList.Builder<SymbolIndexScope> builder = new ImmutableList.Builder<>();
    for (SymbolIndexScope scope : index.get(symbolSimpleName)) {
      if (allowedSymbolTypes.contains(scope.getSymbol().getType())) {
        builder.add(scope);
      }
    }
    return builder.build();
  }

  public List<String> getQualifiers() {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
    builder.addAll(getSymbol().getQualifierList());
    builder.add(getSymbol().getSimpleName());
    return builder.build();
  }

  public List<SymbolIndexScope> getAllScopes() {
    return ImmutableList.copyOf(index.values());
  }

  public Symbol getSymbol() {
    return symbol;
  }

  private SymbolIndexScope indexSymbolSimpleName(Symbol symbol) {
    SymbolIndexScope newScope = new SymbolIndexScope(this, symbol);
    indexScope(newScope);
    return newScope;
  }

  private void indexScope(SymbolIndexScope scope) {
    index.put(scope.getSymbol().getSimpleName(), scope);
  }

  private static Symbol createPseudoSymbol() {
    return Symbol.newBuilder().setType(Symbol.Type.PSEUDO).build();
  }

  private static boolean isTypeDeclaraion(Symbol.Type symbolType) {
    switch (symbolType) {
      case CLASS:
      case INTERFACE:
      case ENUM:
      case ANNOTATION:
        return true;
      default:
        return false;
    }
  }
}
