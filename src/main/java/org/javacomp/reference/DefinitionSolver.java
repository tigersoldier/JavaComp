package org.javacomp.reference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.ModuleScope;
import org.javacomp.model.SolvedType;
import org.javacomp.model.VariableEntity;
import org.javacomp.parser.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.MemberSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Finds the definition of a symbol at a given position */
public class DefinitionSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();
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
   * @param moduleScope the global scope of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public List<? extends Entity> getDefinitionEntities(
      ModuleScope moduleScope, Path filePath, int line, int column) {
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(moduleScope, filePath, line, column);

    if (!positionContext.isPresent()) {
      return ImmutableList.of();
    }

    TreePath treePath = positionContext.get().getTreePath();
    Tree leafTree = treePath.getLeaf();
    TreePath parentPath = treePath.getParentPath();
    Tree parentTree = parentPath != null ? parentPath.getLeaf() : null;

    if (leafTree instanceof ExpressionTree) {
      Set<Entity.Kind> allowedKinds = ALLOWED_ENTITY_KINDS;
      if (treeIsMethodName(leafTree, parentTree)) {
        // parentTree is the method we need to solve.
        leafTree = parentTree;
      } else if (treeIsTypeReference(treePath)) {
        allowedKinds = ALLOWED_ENTITY_KINDS_FOR_TYPE_REFERENCE;
      }
      return expressionSolver.solveDefinitions(
          (ExpressionTree) leafTree,
          positionContext.get().getModuleScope(),
          positionContext.get().getScopeAtPosition(),
          positionContext.get().getPosition(),
          allowedKinds);
    }
    return ImmutableList.of();
  }

  private boolean treeIsMethodName(Tree tree, Tree parentTree) {
    return parentTree instanceof MethodInvocationTree
        && ((MethodInvocationTree) parentTree).getMethodSelect() == tree;
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
      List<? extends ExpressionTree> args, EntityScope baseScope, ModuleScope moduleScope) {
    return args.stream()
        .map(
            expression ->
                expressionSolver.solve(
                    expression, moduleScope, baseScope, ((JCTree) expression).getStartPosition()))
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
