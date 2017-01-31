package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth8;
import com.sun.source.tree.ExpressionTree;
import java.util.Optional;
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
  private ClassEntity innerAClass;
  private ClassEntity innerBClass;
  private ClassEntity innerCClass;

  @Before
  public void setUpTestScope() throws Exception {
    globalScope = TestUtil.parseFiles(TEST_DIR, TEST_FILE);
    topLevelClass = (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME, globalScope);
    innerAClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerA", globalScope);
    innerBClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerB", globalScope);
    innerCClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerC", globalScope);
  }

  @Test
  public void solveMemberSelection() {
    assertThat(solveExpression("innerA", topLevelClass).getEntity()).isEqualTo(innerAClass);
    assertThat(solveExpression("innerA.innerB", topLevelClass).getEntity()).isEqualTo(innerBClass);
    assertThat(solveExpression("innerA.innerB.innerC", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solvedInheritedField() {
    assertThat(solveExpression("baseInnerB", innerAClass).getEntity()).isEqualTo(innerBClass);
    assertThat(solveExpression("innerA.baseInnerB", topLevelClass).getEntity())
        .isEqualTo(innerBClass);
  }

  @Test
  public void solveQualifiedClassField() {
    assertThat(
            solveExpression("org.javacomp.typesolver.testdata.TestExpression.innerA", topLevelClass)
                .getEntity())
        .isEqualTo(innerAClass);
  }

  @Test
  public void solveThis() {
    assertThat(solveExpression("this", topLevelClass).getEntity()).isEqualTo(topLevelClass);
    assertThat(solveExpression("this.innerA", topLevelClass).getEntity()).isEqualTo(innerAClass);
  }

  @Test
  public void solveSuper() {
    assertThat(solveExpression("super.innerA", innerAClass).getEntity()).isEqualTo(innerAClass);
  }

  @Test
  public void solveThisOfSuperClass() {
    assertThat(solveExpression("TestExpression.this", innerAClass).getEntity())
        .isEqualTo(topLevelClass);
  }

  @Test
  public void solveMethodInvocation() {
    assertThat(solveExpression("baseMethod()", topLevelClass).getEntity()).isEqualTo(innerCClass);
    assertThat(solveExpression("baseMethod(42)", topLevelClass).getEntity()).isEqualTo(innerBClass);
    assertThat(solveExpression("this.baseMethod()", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solveSuperClassMethodInvocation() {
    assertThat(solveExpression("innerA.baseMethod()", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
    assertThat(solveExpression("baseMethod()", innerAClass).getEntity()).isEqualTo(innerCClass);
    assertThat(solveExpression("super.baseMethod()", innerAClass).getEntity())
        .isEqualTo(innerCClass);
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, globalScope, baseScope);
    Truth8.assertThat(solvedExpression).named(expression).isPresent();
    return solvedExpression.get();
  }
}
