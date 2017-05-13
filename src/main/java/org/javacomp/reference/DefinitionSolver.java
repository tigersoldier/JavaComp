package org.javacomp.reference;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.TypeReference;
import org.javacomp.parser.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.MemberSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Finds the definition of a symbol at a given position */
public class DefinitionSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();
  private static final Set<Entity.Kind> ENTITY_KINDS_METHOD_ONLY = EnumSet.of(Entity.Kind.METHOD);
  private static final Set<Entity.Kind> ENTITY_KINDS_VARIABLE_AND_FIELD =
      EnumSet.of(Entity.Kind.VARIABLE, Entity.Kind.FIELD);

  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;
  private final OverloadSolver overloadSolver;
  private final MemberSolver memberSolver;

  public DefinitionSolver() {
    this.typeSolver = new TypeSolver();
    this.overloadSolver = new OverloadSolver(typeSolver);
    this.memberSolver = new MemberSolver(typeSolver, overloadSolver);
    this.expressionSolver = new ExpressionSolver(typeSolver, overloadSolver, memberSolver);
  }

  /**
   * @param globalScope the global scope of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public List<? extends Entity> getDefinitionEntities(
      GlobalScope globalScope, Path filePath, int line, int column) {
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(globalScope, filePath, line, column);

    if (!positionContext.isPresent()) {
      return ImmutableList.of();
    }

    TreePath treePath = positionContext.get().getTreePath();
    TreePath parentPath = treePath.getParentPath();
    Tree leafTree = treePath.getLeaf();
    Tree parentTree = parentPath != null ? parentPath.getLeaf() : null;

    if (leafTree instanceof MemberSelectTree) {
      return getMemberSelectDefinition(
          positionContext.get(), (MemberSelectTree) leafTree, parentTree);
    } else if (leafTree instanceof IdentifierTree) {
      return getIdentifierDefinition(positionContext.get(), (IdentifierTree) leafTree, parentTree);
    } else {
      return ImmutableList.of();
    }
  }

  private List<Entity> getMemberSelectDefinition(
      PositionContext positionContext, MemberSelectTree tree, Tree parentTree) {
    GlobalScope globalScope = positionContext.getGlobalScope();
    String identifier = tree.getIdentifier().toString();
    Optional<SolvedType> solvedType =
        expressionSolver.solve(
            tree.getExpression(),
            positionContext.getGlobalScope(),
            positionContext.getScopeAtPosition());
    if (!solvedType.isPresent()) {
      return ImmutableList.of();
    }

    if (parentTree instanceof MethodInvocationTree) {
      List<? extends ExpressionTree> args = ((MethodInvocationTree) parentTree).getArguments();
      return getMethodDefinition(identifier, args, solvedType.get(), globalScope);
    } else {
      return toEntityList(
          memberSolver.findNonMethodMember(identifier, solvedType.get(), globalScope));
    }
  }

  private List<Entity> getMethodDefinition(
      String identifier,
      List<? extends ExpressionTree> args,
      SolvedType baseType,
      GlobalScope globalScope) {
    Optional<? extends Entity> entity =
        memberSolver.findMethodMember(
            identifier,
            solveMethodArgs(args, baseType.getEntity().getChildScope(), globalScope),
            baseType,
            globalScope);
    return toEntityList(entity);
  }

  private List<? extends Entity> getIdentifierDefinition(
      PositionContext positionContext, IdentifierTree identifier, Tree parentTree) {
    if (parentTree instanceof MethodInvocationTree
        && ((MethodInvocationTree) parentTree).getMethodSelect() == identifier) {
      // Method invocation without member selection.
      @SuppressWarnings("unchecked")
      List<MethodEntity> methods =
          (List<MethodEntity>)
              (List)
                  typeSolver.findEntitiesInScope(
                      identifier.getName().toString(),
                      positionContext.getGlobalScope(),
                      positionContext.getScopeAtPosition(),
                      ENTITY_KINDS_METHOD_ONLY);
      if (methods.isEmpty()) {
        return ImmutableList.of();
      }
      return ImmutableList.of(
          overloadSolver.solve(
              methods,
              solveMethodArgs(
                  ((MethodInvocationTree) parentTree).getArguments(),
                  positionContext.getScopeAtPosition(),
                  positionContext.getGlobalScope()),
              positionContext.getGlobalScope()));
    } else if (parentTree instanceof VariableTree
        && ((VariableTree) parentTree).getType() == identifier) {
      // Type of a variable;
      TypeReference typeReference =
          TypeReference.builder()
              .setFullName(identifier.getName().toString())
              .setPrimitive(false)
              .setArray(false)
              .build();
      Optional<SolvedType> variableType =
          typeSolver.solve(
              typeReference,
              positionContext.getGlobalScope(),
              positionContext.getScopeAtPosition());
      return solvedTypeToEntityList(variableType);
    } else {
      return typeSolver.findEntitiesInScope(
          identifier.getName().toString(),
          positionContext.getGlobalScope(),
          positionContext.getScopeAtPosition(),
          ENTITY_KINDS_VARIABLE_AND_FIELD);
    }
  }

  private List<Optional<SolvedType>> solveMethodArgs(
      List<? extends ExpressionTree> args, EntityScope baseScope, GlobalScope globalScope) {
    return args.stream()
        .map(expression -> expressionSolver.solve(expression, globalScope, baseScope))
        .collect(ImmutableList.toImmutableList());
  }

  private static List<Entity> toEntityList(Optional<? extends Entity> entity) {
    if (entity.isPresent()) {
      return ImmutableList.of(entity.get());
    }
    return ImmutableList.of();
  }

  private static List<Entity> solvedTypeToEntityList(Optional<SolvedType> solvedType) {
    return toEntityList(solvedType.map(t -> t.getEntity()));
  }
}
