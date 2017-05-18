package org.javacomp.parser;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.javacomp.logging.JLogger;
import org.javacomp.model.BlockScope;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.model.util.NestedRangeMapBuilder;

public class AstScanner extends TreePathScanner<Void, EntityScope> {
  private static final List<String> UNAVAILABLE_QUALIFIERS = ImmutableList.of();
  private static final String ON_DEMAND_IMPORT_WILDCARD = "*";

  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final TypeReferenceScanner typeReferenceScanner = new TypeReferenceScanner();
  private final ParameterScanner parameterScanner = new ParameterScanner(typeReferenceScanner);

  private FileScope fileScope = null;
  private List<String> currentQualifiers = new ArrayList<>();
  private EndPosTable endPosTable = null;
  private NestedRangeMapBuilder<EntityScope> scopeRangeBuilder = null;
  private String filename = null;
  private String content = null;

  public FileScope startScan(JCCompilationUnit node, String filename, CharSequence content) {
    this.filename = filename;
    this.content = content.toString();
    super.scan(node, null);
    this.filename = null;
    this.content = null;
    return this.fileScope;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, EntityScope unused) {
    // Find or create package scope
    if (node.getPackageName() != null) {
      List<String> qualifiers = nameToQualifiers(node.getPackageName());
      this.currentQualifiers.addAll(qualifiers);
    }

    JCCompilationUnit compilationUnit = (JCCompilationUnit) node;
    this.fileScope = new FileScope(filename, this.currentQualifiers, compilationUnit);
    this.scopeRangeBuilder = new NestedRangeMapBuilder<>();
    this.endPosTable = compilationUnit.endPositions;
    addScopeRange(compilationUnit, this.fileScope);

    // Handle imports
    for (ImportTree importTree : node.getImports()) {
      List<String> qualifiers = nameToQualifiers(importTree.getQualifiedIdentifier());
      if (qualifiers.isEmpty()) {
        continue;
      }
      if (ON_DEMAND_IMPORT_WILDCARD.equals(qualifiers.get(qualifiers.size() - 1))) {
        this.fileScope.addOnDemandClassImport(qualifiers.subList(0, qualifiers.size() - 1));
      } else {
        this.fileScope.addImportedClass(qualifiers);
      }
    }

    // Handle toplevel type declarations (class, interface, enum, annotation, etc).
    for (Tree decl : node.getTypeDecls()) {
      this.scan(decl, this.fileScope);
    }
    this.fileScope.setScopeRangeMap(scopeRangeBuilder.build());

    // Cleanup
    this.currentQualifiers.clear();
    this.scopeRangeBuilder = null;
    this.endPosTable = null;
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, EntityScope currentScope) {
    Entity.Kind entityKind;
    switch (node.getKind()) {
      case CLASS:
        entityKind = Entity.Kind.CLASS;
        break;
      case INTERFACE:
        entityKind = Entity.Kind.INTERFACE;
        break;
      case ENUM:
        entityKind = Entity.Kind.ENUM;
        break;
      case ANNOTATION_TYPE:
        entityKind = Entity.Kind.ANNOTATION;
        break;
      default:
        logger.severe("Unknown entity kind for class: %s", node.getKind());
        return null;
    }
    ImmutableList.Builder<TypeReference> interfaceBuilder = new ImmutableList.Builder<>();
    Optional<TypeReference> superClass = Optional.empty();
    if (node.getExtendsClause() != null) {
      superClass = Optional.of(typeReferenceScanner.getTypeReference(node.getExtendsClause()));
    }
    for (Tree implementClause : node.getImplementsClause()) {
      interfaceBuilder.add(typeReferenceScanner.getTypeReference(implementClause));
    }
    Range<Integer> range = getClassNameRange((JCClassDecl) node);
    ClassEntity classEntity =
        new ClassEntity(
            node.getSimpleName().toString(),
            entityKind,
            this.currentQualifiers,
            currentScope,
            superClass,
            interfaceBuilder.build(),
            range);
    currentScope.addEntity(classEntity);
    addScopeRange((JCTree) node, classEntity);
    if (this.currentQualifiers != UNAVAILABLE_QUALIFIERS) {
      // Not in a method, can be reached globally.
      this.currentQualifiers.add(classEntity.getSimpleName());
      this.fileScope.addGlobalEntity(classEntity);
    }

    for (Tree member : node.getMembers()) {
      scan(member, classEntity);
    }

    if (this.currentQualifiers != UNAVAILABLE_QUALIFIERS) {
      this.currentQualifiers.remove(this.currentQualifiers.size() - 1);
    }
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, EntityScope currentScope) {
    checkArgument(
        currentScope instanceof ClassEntity, "Method's parent scope must be a class entity");
    TypeReference returnType;
    if (node.getReturnType() == null) {
      // Constructor doesn't have return type.
      returnType = TypeReference.EMPTY_TYPE;
    } else {
      returnType = typeReferenceScanner.getTypeReference(node.getReturnType());
    }
    ImmutableList.Builder<VariableEntity> parameterListBuilder = new ImmutableList.Builder<>();
    for (Tree parameter : node.getParameters()) {
      parameterListBuilder.add(parameterScanner.getParameter(parameter, currentScope));
    }

    Range<Integer> range = getMethodNameRange((JCMethodDecl) node);
    MethodEntity methodEntity =
        new MethodEntity(
            node.getName().toString(),
            this.currentQualifiers,
            returnType,
            parameterListBuilder.build(),
            (ClassEntity) currentScope,
            range);
    // TODO: distinguish between static and non-static methods.
    currentScope.addEntity(methodEntity);
    List<String> previousQualifiers = this.currentQualifiers;
    // No entity defined inside method scope is qualified.
    this.currentQualifiers = UNAVAILABLE_QUALIFIERS;
    if (node.getBody() != null) {
      scan(node.getBody(), methodEntity);
      addScopeRange((JCTree) node, methodEntity);
    }
    this.currentQualifiers = previousQualifiers;
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, EntityScope currentScope) {
    Entity.Kind variableKind =
        (currentScope instanceof ClassEntity) ? Entity.Kind.FIELD : Entity.Kind.VARIABLE;
    Range<Integer> range = getVariableNameRange((JCVariableDecl) node);
    VariableEntity variableEntity =
        new VariableEntity(
            node.getName().toString(),
            variableKind,
            this.currentQualifiers,
            typeReferenceScanner.getTypeReference(node.getType()),
            currentScope,
            range);
    logger.fine("adding variable %s to scope %s", variableEntity, currentScope);
    currentScope.addEntity(variableEntity);
    // TODO: add entity to global scope if it's a non-private static entity.
    return null;
  }

  @Override
  public Void visitBlock(BlockTree node, EntityScope currentScope) {
    boolean isMethodBlock =
        (currentScope instanceof MethodEntity)
            && (getCurrentPath().getParentPath().getLeaf() instanceof MethodTree);
    if (!isMethodBlock) {
      currentScope = new BlockScope(currentScope);
    }
    for (StatementTree statement : node.getStatements()) {
      this.scan(statement, currentScope);
    }
    addScopeRange((JCTree) node, currentScope);
    return null;
  }

  private static List<String> nameToQualifiers(Tree name) {
    Deque<String> stack = new ArrayDeque<>();
    while (name instanceof MemberSelectTree) {
      MemberSelectTree qualifiedName = (MemberSelectTree) name;
      stack.addFirst(qualifiedName.getIdentifier().toString());
      name = qualifiedName.getExpression();
    }
    stack.addFirst(((IdentifierTree) name).getName().toString());
    return ImmutableList.copyOf(stack);
  }

  private Range<Integer> getNodeRange(JCTree node) {
    return Range.closed(node.getStartPosition(), node.getEndPosition(endPosTable));
  }

  private void addScopeRange(JCTree node, EntityScope scope) {
    Range<Integer> range = getNodeRange(node);
    scopeRangeBuilder.put(range, scope);
  }

  private static class TypeReferenceScanner extends TreeScanner<Void, Void> {
    private final Deque<String> names = new ArrayDeque<>();
    private boolean isPrimitive = false;
    private boolean isArray = false;

    public TypeReference getTypeReference(Tree node) {
      names.clear();
      isPrimitive = false;
      isArray = false;
      scan(node, null);
      if (names.isEmpty()) {
        // Malformed input, no type can be referenced
        return TypeReference.EMPTY_TYPE;
      }
      return TypeReference.builder()
          .setFullName(names)
          .setPrimitive(isPrimitive)
          .setArray(isArray)
          .build();
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree node, Void unused) {
      scan(node.getType(), unused);
      // TODO: handle type parameters.
      return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree node, Void unused) {
      isArray = true;
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
      isPrimitive = true;
      names.addFirst(node.getPrimitiveTypeKind().name().toLowerCase());
      return null;
    }
  }

  private Range<Integer> getVariableNameRange(JCVariableDecl node) {
    if (node.getName() != null) {
      String name = node.getName().toString();
      List<? extends JCTree> precedentNodes;
      if (node.getType() != null) {
        precedentNodes = ImmutableList.of(node.getType());
      } else {
        precedentNodes = ImmutableList.of();
      }
      return getNodeNameRangeAfter(node, name, precedentNodes);
    } else if (node.getNameExpression() != null) {
      return getNodeRange(node.getNameExpression());
    }
    return getNodeRange(node);
  }

  private Range<Integer> getClassNameRange(JCClassDecl node) {
    if (node.getSimpleName() == null) {
      return getNodeRange(node);
    }

    String name = node.getSimpleName().toString();
    List<? extends JCTree> precedentNodes;
    if (node.getModifiers() != null && node.getModifiers().getAnnotations() != null) {
      precedentNodes = node.getModifiers().getAnnotations();
    } else {
      precedentNodes = ImmutableList.of();
    }
    return getNodeNameRangeAfter(node, name, precedentNodes);
  }

  private Range<Integer> getMethodNameRange(JCMethodDecl node) {
    if (node.getName() == null) {
      return getNodeRange(node);
    }

    String name = node.getName().toString();
    List<JCTree> precedentNodes = new ArrayList<>();
    if (node.getModifiers() != null && node.getModifiers().getAnnotations() != null) {
      precedentNodes.addAll(node.getModifiers().getAnnotations());
    }
    if (node.getTypeParameters() != null) {
      precedentNodes.addAll(node.getTypeParameters());
    }
    if (node.getReturnType() != null) {
      precedentNodes.add(node.getReturnType());
    }
    return getNodeNameRangeAfter(node, name, precedentNodes);
  }

  private Range<Integer> getNodeNameRangeAfter(
      JCTree node, String name, List<? extends JCTree> precedentNodes) {
    int start = node.getStartPosition();
    for (JCTree precedentNode : precedentNodes) {
      start = Math.max(start, precedentNode.getEndPosition(endPosTable));
    }
    start = content.indexOf(name, start);
    if (start > -1 && start < node.getEndPosition(endPosTable)) {
      return Range.closedOpen(start, start + name.length());
    }

    return getNodeRange(node);
  }

  private class ParameterScanner extends TreeScanner<Void, Void> {
    private final TypeReferenceScanner typeReferenceScanner;
    private String name = "";
    private TypeReference type = TypeReference.EMPTY_TYPE;

    private ParameterScanner(TypeReferenceScanner typeReferenceScanner) {
      this.typeReferenceScanner = typeReferenceScanner;
    }

    private VariableEntity getParameter(Tree node, EntityScope currentScope) {
      name = "";
      type = TypeReference.EMPTY_TYPE;
      scan(node, null);

      Range<Integer> range = getVariableNameRange((JCVariableDecl) node);
      return new VariableEntity(
          name,
          Entity.Kind.VARIABLE,
          ImmutableList.of() /* qualifiers */,
          type,
          currentScope,
          range);
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
      name = node.getName().toString();
      type = typeReferenceScanner.getTypeReference(node.getType());
      return null;
    }
  }
}
