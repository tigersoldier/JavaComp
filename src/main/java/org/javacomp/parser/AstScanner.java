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
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
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

  public FileScope startScan(JCCompilationUnit node, String filename) {
    this.filename = filename;
    super.scan(node, null);
    this.filename = null;
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
    ClassEntity classEntity =
        new ClassEntity(
            node.getSimpleName().toString(),
            entityKind,
            this.currentQualifiers,
            currentScope,
            superClass,
            interfaceBuilder.build());
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

    MethodEntity methodEntity =
        new MethodEntity(
            node.getName().toString(),
            this.currentQualifiers,
            returnType,
            parameterListBuilder.build(),
            (ClassEntity) currentScope);
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
    VariableEntity variableEntity =
        new VariableEntity(
            node.getName().toString(),
            variableKind,
            this.currentQualifiers,
            typeReferenceScanner.getTypeReference(node.getType()),
            currentScope);
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

  private void addScopeRange(JCTree node, EntityScope scope) {
    Range<Integer> range = Range.closed(node.getStartPosition(), node.getEndPosition(endPosTable));
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

  private static class ParameterScanner extends TreeScanner<Void, Void> {
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
      return new VariableEntity(
          name, Entity.Kind.VARIABLE, ImmutableList.of() /* qualifiers */, type, currentScope);
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
      name = node.getName().toString();
      type = typeReferenceScanner.getTypeReference(node.getType());
      return null;
    }
  }
}
