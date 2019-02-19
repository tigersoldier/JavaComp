package org.javacomp.reference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.Module;
import org.javacomp.model.SolvedEntityType;
import org.javacomp.model.SolvedType;
import org.javacomp.model.VariableEntity;
import org.javacomp.project.ModuleManager;
import org.javacomp.project.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.MemberSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Finds the definition of a symbol at a given position */
public class DefinitionSolver {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Set<Entity.Kind> ALLOWED_ENTITY_KINDS =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(VariableEntity.ALLOWED_KINDS)
          .addAll(ClassEntity.ALLOWED_KINDS)
          .add(Entity.Kind.METHOD)
          .build();
  private static final Set<Entity.Kind> ALLOWED_ENTITY_KINDS_FOR_TYPE_REFERENCE =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(ClassEntity.ALLOWED_KINDS)
          .add(Entity.Kind.QUALIFIER)
          .build();

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
   * @param moduleManager the module manager of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public List<? extends Entity> getDefinitionEntities(
      ModuleManager moduleManager, Path filePath, int line, int column) {
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(moduleManager, filePath, line, column);

    if (!positionContext.isPresent()) {
      return ImmutableList.of();
    }

    return getDefinitionEntities(positionContext.get().getModule(), positionContext.get());
  }

  List<? extends Entity> getDefinitionEntities(Module module, PositionContext positionContext) {
    TreePath treePath = positionContext.getTreePath();
    Tree leafTree = treePath.getLeaf();
    TreePath parentPath = treePath.getParentPath();
    Tree parentTree = parentPath != null ? parentPath.getLeaf() : null;

    if (leafTree instanceof LiteralTree) {
      // LiteralTree is also an ExpressionTree. We don't want to show type definitions for literal
      // constants.
      return ImmutableList.of();
    } else if (leafTree instanceof ExpressionTree) {
      Set<Entity.Kind> allowedKinds = ALLOWED_ENTITY_KINDS;
      if (treeIsMethodName(leafTree, parentTree)) {
        // parentTree is the method we need to solve.
        leafTree = parentTree;
      } else if (treeIsNewClassIdent(leafTree, parentTree)) {
        // parentTree is the new class expression we need to solve.
        leafTree = parentTree;
      } else if (treeIsTypeReference(treePath)) {
        allowedKinds = ALLOWED_ENTITY_KINDS_FOR_TYPE_REFERENCE;
      }
      return expressionSolver
          .solveDefinitions(
              (ExpressionTree) leafTree,
              positionContext.getModule(),
              positionContext.getScopeAtPosition(),
              positionContext.getPosition(),
              allowedKinds)
          .stream()
          .map(entityWithContext -> entityWithContext.getEntity())
          .collect(ImmutableList.toImmutableList());
    }
    return ImmutableList.of();
  }

  private boolean treeIsMethodName(Tree tree, Tree parentTree) {
    return parentTree instanceof MethodInvocationTree
        && ((MethodInvocationTree) parentTree).getMethodSelect() == tree;
  }

  private boolean treeIsNewClassIdent(Tree tree, Tree parentTree) {
    if (!(tree instanceof IdentifierTree)) {
      return false;
    }
    if (!(parentTree instanceof NewClassTree)) {
      return false;
    }
    if (((NewClassTree) parentTree).getIdentifier() != tree) {
      return false;
    }
    return true;
  }

  /** Determines whether the leaf of {@code treePath} is referencing to a type or not. */
  private boolean treeIsTypeReference(TreePath treePath) {
    TreePath parentPath = treePath.getParentPath();
    while (parentPath != null && parentPath.getLeaf() instanceof MemberSelectTree) {
      treePath = parentPath;
      parentPath = treePath.getParentPath();
    }

    if (parentPath == null) {
      return false;
    }

    Tree tree = treePath.getLeaf();
    Tree parentTree = parentPath.getLeaf();

    if (parentTree instanceof VariableTree) {
      return ((VariableTree) parentTree).getType() == tree;
    }

    if (parentTree instanceof MethodTree) {
      return ((MethodTree) parentTree).getReturnType() == tree;
    }

    return false;
  }

  private List<Optional<SolvedType>> solveMethodArgs(
      List<? extends ExpressionTree> args, EntityScope baseScope, Module module) {
    return args.stream()
        .map(
            expression ->
                expressionSolver
                    .solve(expression, module, baseScope, ((JCTree) expression).getStartPosition())
                    .map(entityWithContext -> entityWithContext.toSolvedType()))
        .collect(ImmutableList.toImmutableList());
  }

  private static List<Entity> toEntityList(Optional<? extends Entity> entity) {
    if (entity.isPresent()) {
      return ImmutableList.of(entity.get());
    }
    return ImmutableList.of();
  }

  private static List<Entity> solvedTypeToEntityList(Optional<SolvedType> solvedType) {
    return toEntityList(
        solvedType
            .filter(t -> t instanceof SolvedEntityType)
            .map(t -> ((SolvedEntityType) t).getEntity()));
  }
}
