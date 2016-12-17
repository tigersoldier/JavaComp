package org.javacomp.parser;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

public class AstScanner extends TreeScanner<Void, SymbolIndex> {
  private static final List<String> EMPTY_QUALIFIERS = ImmutableList.of();

  private GlobalIndex globalIndex = null;
  private FileIndex fileIndex = null;
  private List<String> currentQualifiers = new ArrayList<>();

  public Void startScan(Tree node, GlobalIndex globalIndex) {
    this.globalIndex = globalIndex;
    super.scan(node, globalIndex);
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
    for (Tree decl : node.getTypeDecls()) {
      this.scan(decl, fileIndex);
    }

    this.currentQualifiers.clear();
    this.fileIndex = null;
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
    this.currentQualifiers.add(classSymbol.getSimpleName());
    for (Tree member : node.getMembers()) {
      scan(member, classIndex);
    }
    this.currentQualifiers.remove(this.currentQualifiers.size() - 1);
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
    this.currentQualifiers = EMPTY_QUALIFIERS;
    if (node.getBody() != null) {
      visitBlock(node.getBody(), methodIndex);
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
}
