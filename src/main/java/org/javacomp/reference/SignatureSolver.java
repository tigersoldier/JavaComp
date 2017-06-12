package org.javacomp.reference;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.Entity;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.parser.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.MemberSolver;
import org.javacomp.typesolver.OverloadSolver;
import org.javacomp.typesolver.TypeSolver;

/** Finds the method signature of a symbol at a given position */
public class SignatureSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final Set<Entity.Kind> METHOD_KIND_SET = EnumSet.of(Entity.Kind.METHOD);

  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;
  private final OverloadSolver overloadSolver;
  private final MemberSolver memberSolver;

  public SignatureSolver() {
    this.typeSolver = new TypeSolver();
    this.overloadSolver = new OverloadSolver(typeSolver);
    this.memberSolver = new MemberSolver(typeSolver, overloadSolver);
    this.expressionSolver = new ExpressionSolver(typeSolver, overloadSolver, memberSolver);
  }

  /**
   * @param module the module of the project
   * @param filePath normalized path of the file to be completed
   * @param line 0-based line number of the completion point
   * @param column 0-based character offset from the beginning of the line to the completion point
   */
  public MethodSignatures getMethodSignatures(Module module, Path filePath, int line, int column) {
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(module, filePath, line, column);

    if (!positionContext.isPresent()) {
      return emptySignature();
    }

    TreePath treePath = positionContext.get().getTreePath();
    Tree childNode = null;

    while (treePath != null) {
      Tree node = treePath.getLeaf();
      if (inMethodParameter(positionContext.get(), node)) {
        return getMethodSignatures(positionContext.get(), (MethodInvocationTree) node, childNode);
      }
      if (node instanceof ClassTree) {
        // Do not solve method signature for annonymous classes.
        break;
      }
      childNode = node;
      treePath = treePath.getParentPath();
    }

    return MethodSignatures.create(ImmutableList.of(), 0 /* activeParameter */);
  }

  private boolean inMethodParameter(PositionContext positionContext, Tree node) {
    if (!(node instanceof MethodInvocationTree)) {
      return false;
    }
    JCMethodInvocation method = (JCMethodInvocation) node;
    int methodNameEndPos =
        method.getMethodSelect().getEndPosition(positionContext.getEndPosTable());
    return positionContext.getPosition() > methodNameEndPos;
  }

  private MethodSignatures getMethodSignatures(
      PositionContext positionContext, MethodInvocationTree method, Tree childNode) {
    List<MethodEntity> methods =
        expressionSolver
            .solveDefinitions(
                method,
                positionContext.getModule(),
                positionContext.getScopeAtPosition(),
                -1 /* position doesn't matter for solving methods. */,
                METHOD_KIND_SET)
            .stream()
            .map(entityWithContext -> (MethodEntity) entityWithContext.getEntity())
            .collect(ImmutableList.toImmutableList());
    if (methods.isEmpty()) {
      return emptySignature();
    }

    MethodEntity activeMethod = methods.get(0);
    int activeParam = 0;
    List<? extends ExpressionTree> arguments = method.getArguments();
    int position = positionContext.getPosition();

    for (int i = 0; i < arguments.size(); i++) {
      JCExpression argument = (JCExpression) arguments.get(i);
      if (argument == childNode) {
        activeParam = i;
        break;
      }

      // Use >= to account for comma after parameters.
      if (argument.getEndPosition(positionContext.getEndPosTable()) >= position) {
        activeParam = i;
        break;
      }
    }
    activeParam = Math.max(0, Math.min(activeParam, activeMethod.getParameters().size() - 1));
    return MethodSignatures.create(methods, activeParam);
  }

  private MethodSignatures emptySignature() {
    return MethodSignatures.create(ImmutableList.of(), 0);
  }
}
