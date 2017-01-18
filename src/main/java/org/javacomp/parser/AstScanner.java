package org.javacomp.parser;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
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
import org.javacomp.model.ClassSymbol;
import org.javacomp.model.FileIndex;
import org.javacomp.model.MethodIndex;
import org.javacomp.model.MethodSymbol;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableSymbol;
import org.javacomp.model.util.NestedRangeMapBuilder;

public class AstScanner extends TreeScanner<Void, SymbolIndex> {
  private static final List<String> UNAVAILABLE_QUALIFIERS = ImmutableList.of();

  private final TypeReferenceScanner typeReferenceScanner = new TypeReferenceScanner();
  private final ParameterScanner parameterScanner = new ParameterScanner(typeReferenceScanner);

  private FileIndex fileIndex = null;
  private List<String> currentQualifiers = new ArrayList<>();
  private EndPosTable endPosTable = null;
  private NestedRangeMapBuilder<SymbolIndex> indexRangeBuilder = null;
  private String filename = null;

  public FileIndex startScan(JCCompilationUnit node, String filename) {
    this.filename = filename;
    super.scan(node, null);
    this.filename = null;
    return this.fileIndex;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, SymbolIndex unused) {
    // Find or create package index
    if (node.getPackageName() != null) {
      List<String> qualifiers = nameToQualifiers(node.getPackageName());
      this.currentQualifiers.addAll(qualifiers);
    }

    this.fileIndex = new FileIndex(filename, this.currentQualifiers);
    this.indexRangeBuilder = new NestedRangeMapBuilder<>();
    this.endPosTable = ((JCCompilationUnit) node).endPositions;

    // Handle imports
    for (ImportTree importTree : node.getImports()) {
      this.fileIndex.addImportedClass(
          typeReferenceScanner.getTypeReference(importTree.getQualifiedIdentifier()));
    }

    // Handle toplevel type declarations (class, interface, enum, annotation, etc).
    for (Tree decl : node.getTypeDecls()) {
      this.scan(decl, this.fileIndex);
    }
    this.fileIndex.setIndexRangeMap(indexRangeBuilder.build());

    // Cleanup
    this.currentQualifiers.clear();
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
    ImmutableList.Builder<TypeReference> interfaceBuilder = new ImmutableList.Builder<>();
    Optional<TypeReference> superClass = Optional.absent();
    if (node.getExtendsClause() != null) {
      superClass = Optional.of(typeReferenceScanner.getTypeReference(node.getExtendsClause()));
    }
    for (Tree implementClause : node.getImplementsClause()) {
      interfaceBuilder.add(typeReferenceScanner.getTypeReference(implementClause));
    }
    ClassSymbol classSymbol =
        new ClassSymbol(
            node.getSimpleName().toString(),
            symbolKind,
            this.currentQualifiers,
            currentIndex,
            superClass,
            interfaceBuilder.build());
    currentIndex.addSymbol(classSymbol);
    addIndexRange((JCTree) node, classSymbol);
    if (this.currentQualifiers != UNAVAILABLE_QUALIFIERS) {
      // Not in a method, can be reached globally.
      this.currentQualifiers.add(classSymbol.getSimpleName());
      this.fileIndex.addGlobalSymbol(classSymbol);
    }

    for (Tree member : node.getMembers()) {
      scan(member, classSymbol);
    }

    if (this.currentQualifiers != UNAVAILABLE_QUALIFIERS) {
      this.currentQualifiers.remove(this.currentQualifiers.size() - 1);
    }
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, SymbolIndex currentIndex) {
    checkArgument(
        currentIndex instanceof ClassSymbol, "Method's parent index must be a class symbol");
    MethodSymbol methodSymbol =
        (MethodSymbol)
            currentIndex
                .getSymbolWithNameAndKind(node.getName().toString(), Symbol.Kind.METHOD)
                .orNull();
    if (methodSymbol == null) {
      methodSymbol = new MethodSymbol(node.getName().toString(), this.currentQualifiers);
    }

    MethodIndex methodIndex = new MethodIndex((ClassSymbol) currentIndex);
    TypeReference returnType;
    if (node.getReturnType() == null) {
      // Constructor doesn't have return type.
      returnType = TypeReference.VOID_TYPE;
    } else {
      returnType = typeReferenceScanner.getTypeReference(node.getReturnType());
    }
    ImmutableList.Builder<MethodSymbol.Parameter> parameterListBuilder =
        new ImmutableList.Builder<>();
    for (Tree parameter : node.getParameters()) {
      parameterListBuilder.add(parameterScanner.getParameter(parameter));
    }

    methodSymbol.addOverload(
        MethodSymbol.Overload.create(methodIndex, returnType, parameterListBuilder.build()));
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
    Range<Integer> range = Range.closed(node.getStartPosition(), node.getEndPosition(endPosTable));
    indexRangeBuilder.put(range, index);
  }

  private static class TypeReferenceScanner extends TreeScanner<Void, Void> {
    private final Deque<String> names = new ArrayDeque<>();

    public TypeReference getTypeReference(Tree node) {
      names.clear();
      scan(node, null);
      if (names.isEmpty()) {
        // Malformed input, no type can be referenced
        return TypeReference.VOID_TYPE;
      }
      return new TypeReference(ImmutableList.copyOf(names));
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree node, Void unused) {
      scan(node.getType(), unused);
      // TODO: handle type parameters.
      return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree node, Void unused) {
      // TODO: handle array types.
      scan(node.getType(), unused);
      return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
      names.addFirst(node.getIdentifier().toString());
      scan(node.getExpression(), unused);
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
      names.addFirst(node.getName().toString());
      return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, Void unused) {
      names.addFirst(node.getPrimitiveTypeKind().name().toLowerCase());
      return null;
    }
  }

  private static class ParameterScanner extends TreeScanner<Void, Void> {
    private final TypeReferenceScanner typeReferenceScanner;
    private String name = "";
    private TypeReference type = TypeReference.VOID_TYPE;

    private ParameterScanner(TypeReferenceScanner typeReferenceScanner) {
      this.typeReferenceScanner = typeReferenceScanner;
    }

    private MethodSymbol.Parameter getParameter(Tree node) {
      name = "";
      type = TypeReference.VOID_TYPE;
      scan(node, null);
      return MethodSymbol.Parameter.create(type, name);
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
      name = node.getName().toString();
      type = typeReferenceScanner.getTypeReference(node.getType());
      return null;
    }
  }
}
