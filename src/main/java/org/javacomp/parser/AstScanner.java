package org.javacomp.parser;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.javacomp.model.BlockIndex;
import org.javacomp.model.ClassIndex;
import org.javacomp.model.ClassSymbol;
import org.javacomp.model.FileIndex;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.MethodIndex;
import org.javacomp.model.MethodSymbol;
import org.javacomp.model.PackageIndex;
import org.javacomp.model.PackageSymbol;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.javacomp.model.VariableSymbol;
import org.javacomp.model.util.NestedRangeMapBuilder;

public class AstScanner extends TreeScanner<Void, SymbolIndex> {
  private static final List<String> UNAVAILABLE_QUALIFIERS = ImmutableList.of();

  private GlobalIndex globalIndex = null;
  private FileIndex fileIndex = null;
  private List<String> currentQualifiers = new ArrayList<>();
  private EndPosTable endPosTable = null;
  private NestedRangeMapBuilder<SymbolIndex> indexRangeBuilder = null;
  private String filename = null;

  public Void startScan(JCCompilationUnit node, GlobalIndex globalIndex, String filename) {
    this.globalIndex = globalIndex;
    this.filename = filename;
    super.scan(node, globalIndex);
    this.filename = null;
    this.globalIndex = null;
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, SymbolIndex currentIndex) {
    // Find or create package index
    if (node.getPackageName() != null) {
      List<String> qualifiers = nameToQualifiers(node.getPackageName());

      for (String qualifier : qualifiers) {
        Optional<Symbol> packageSymbol =
            currentIndex.getSymbolWithNameAndKind(qualifier, Symbol.Kind.QUALIFIER);
        if (packageSymbol.isPresent()) {
          currentIndex = packageSymbol.get().getChildIndex();
        } else {
          PackageIndex packageIndex = new PackageIndex(currentIndex);
          currentIndex.addSymbol(
              new PackageSymbol(qualifier, this.currentQualifiers, packageIndex));
          currentIndex = packageIndex;
        }
        this.currentQualifiers.add(qualifier);
      }
    }

    this.fileIndex = new FileIndex(currentIndex);
    this.indexRangeBuilder = new NestedRangeMapBuilder<>();
    this.endPosTable = ((JCCompilationUnit) node).endPositions;

    // Map file name to the FileIndex.
    // TODO: if this is an update (e.g. the filename exists in global index), remove all symbols in
    // the existing FileIndex from global index.
    globalIndex.addFileIndex(filename, this.fileIndex);

    // Handle toplevel type declarations (class, interface, enum, annotation, etc).
    for (Tree decl : node.getTypeDecls()) {
      this.scan(decl, this.fileIndex);
    }
    this.fileIndex.setIndexRangeMap(indexRangeBuilder.build());

    // Cleanup
    this.currentQualifiers.clear();
    this.fileIndex = null;
    this.indexRangeBuilder = null;
    this.endPosTable = null;
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, SymbolIndex currentIndex) {
    Symbol.Kind symbolKind;
    switch (node.getKind()) {
      case CLASS:
        symbolKind = Symbol.Kind.CLASS;
        break;
      case INTERFACE:
        symbolKind = Symbol.Kind.INTERFACE;
        break;
      case ENUM:
        symbolKind = Symbol.Kind.ENUM;
        break;
      case ANNOTATION_TYPE:
        symbolKind = Symbol.Kind.ANNOTATION;
        break;
      default:
        throw new IllegalArgumentException("Unknown symbol kind for class: " + node.getKind());
    }
    ClassIndex classIndex = new ClassIndex(currentIndex);
    ClassSymbol classSymbol =
        new ClassSymbol(
            node.getSimpleName().toString(), symbolKind, this.currentQualifiers, classIndex);
    this.globalIndex.addSymbol(classSymbol);
    currentIndex.addSymbol(classSymbol);
    addIndexRange((JCTree) node, classIndex);
    if (this.currentQualifiers != UNAVAILABLE_QUALIFIERS) {
      // Not in a method, can be reached globally.
      this.currentQualifiers.add(classSymbol.getSimpleName());
    }

    for (Tree member : node.getMembers()) {
      scan(member, classIndex);
    }

    if (this.currentQualifiers != UNAVAILABLE_QUALIFIERS) {
      this.currentQualifiers.remove(this.currentQualifiers.size() - 1);
    }
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, SymbolIndex currentIndex) {
    checkArgument(currentIndex instanceof ClassIndex, "Method's parent index must be class index");
    MethodSymbol methodSymbol =
        (MethodSymbol)
            currentIndex
                .getSymbolWithNameAndKind(node.getName().toString(), Symbol.Kind.METHOD)
                .orNull();
    if (methodSymbol == null) {
      methodSymbol = new MethodSymbol(node.getName().toString(), this.currentQualifiers);
    }
    MethodIndex methodIndex = new MethodIndex((ClassIndex) currentIndex);
    methodSymbol.addOverload(methodIndex);
    // TODO: distinguish between static and non-static methods.
    currentIndex.addSymbol(methodSymbol);
    List<String> previousQualifiers = this.currentQualifiers;
    // No symbol defined inside method scope is qualified.
    this.currentQualifiers = UNAVAILABLE_QUALIFIERS;
    if (node.getBody() != null) {
      // Use user.visitBlock because it doesn't create extra BlockIndex.
      super.visitBlock(node.getBody(), methodIndex);
      addIndexRange((JCTree) node, methodIndex);
    }
    this.currentQualifiers = previousQualifiers;
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, SymbolIndex currentIndex) {
    VariableSymbol variableSymbol =
        new VariableSymbol(node.getName().toString(), this.currentQualifiers);
    currentIndex.addSymbol(variableSymbol);
    // TODO: add symbol to global index if it's a non-private static symbol.
    return null;
  }

  @Override
  public Void visitBlock(BlockTree node, SymbolIndex currentIndex) {
    BlockIndex blockIndex = new BlockIndex(currentIndex);
    for (StatementTree statement : node.getStatements()) {
      this.scan(statement, blockIndex);
    }
    addIndexRange((JCTree) node, blockIndex);
    return null;
  }

  private static List<String> nameToQualifiers(ExpressionTree name) {
    Deque<String> stack = new ArrayDeque<>();
    while (name instanceof MemberSelectTree) {
      MemberSelectTree qualifiedName = (MemberSelectTree) name;
      stack.addFirst(qualifiedName.getIdentifier().toString());
      name = qualifiedName.getExpression();
    }
    stack.addFirst(((IdentifierTree) name).getName().toString());
    return ImmutableList.copyOf(stack);
  }

  private void addIndexRange(JCTree node, SymbolIndex index) {
    Range<Integer> range =
        Range.closedOpen(node.getStartPosition(), node.getEndPosition(endPosTable));
    indexRangeBuilder.put(range, index);
  }
}
