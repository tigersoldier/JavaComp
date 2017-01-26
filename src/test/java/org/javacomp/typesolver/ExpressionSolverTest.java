package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Optional;
import com.sun.source.tree.ExpressionTree;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.SolvedType;
import org.javacomp.testing.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExpressionSolverTest {
  private static final String TEST_DIR = "src/test/java/org/javacomp/typesolver/testdata";
  private static final String TEST_FILE = "TestExpression.java";
  private static final String TOP_LEVEL_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.TestExpression";

  private final ExpressionSolver expressionSolver = new ExpressionSolver(new TypeSolver());

  private GlobalScope globalScope;
  private ClassEntity topLevelClass;
  private ClassEntity innerAEntity;
  private ClassEntity innerBEntity;
  private ClassEntity innerCEntity;

  @Before
  public void setUpTestScope() throws Exception {
    globalScope = TestUtil.parseFiles(TEST_DIR, TEST_FILE);
    topLevelClass = (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME, globalScope);
    innerAEntity =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerA", globalScope);
    innerBEntity =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerB", globalScope);
    innerCEntity =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerC", globalScope);
  }

  @Test
  public void solveMemberSelection() {
    assertThat(solveExpression("innerA", topLevelClass).getEntity()).isEqualTo(innerAEntity);
    assertThat(solveExpression("innerA.innerB", topLevelClass).getEntity()).isEqualTo(innerBEntity);
    assertThat(solveExpression("innerA.innerB.innerC", topLevelClass).getEntity())
        .isEqualTo(innerCEntity);
  }

  @Test
  public void solvedInheritedField() {
    assertThat(solveExpression("baseInnerB", innerAEntity).getEntity()).isEqualTo(innerBEntity);
    assertThat(solveExpression("innerA.baseInnerB", topLevelClass).getEntity())
        .isEqualTo(innerBEntity);
  }

  @Test
  public void solveQualifiedClassField() {
    assertThat(
            solveExpression("org.javacomp.typesolver.testdata.TestExpression.innerA", topLevelClass)
                .getEntity())
        .isEqualTo(innerAEntity);
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, globalScope, baseScope);
    assertThat(solvedExpression).named(expression).isPresent();
    return solvedExpression.get();
  }
}
