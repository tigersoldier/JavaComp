package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth8;
import com.sun.source.tree.ExpressionTree;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.ModuleScope;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.testing.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExpressionSolverTest {
  private static final String TEST_DIR = "src/test/java/org/javacomp/typesolver/testdata";
  private static final List<String> TEST_FILES =
      ImmutableList.of("TestExpression.java", "TestClass.java", "other/Shadow.java");
  private static final String TOP_LEVEL_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.TestExpression";
  private static final String TEST_CLASS_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.TestClass";
  private static final String SHADOW_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.other.Shadow";

  private final TypeSolver typeSolver = new TypeSolver();
  private final OverloadSolver overloadSolver = new OverloadSolver(typeSolver);
  private final MemberSolver memberSolver = new MemberSolver(typeSolver, overloadSolver);
  private final ExpressionSolver expressionSolver =
      new ExpressionSolver(typeSolver, overloadSolver, memberSolver);

  private ModuleScope moduleScope;
  private ClassEntity topLevelClass;
  private ClassEntity testClassClass;
  private ClassEntity testClassFactoryClass;
  private ClassEntity shadowClass;
  private ClassEntity innerAClass;
  private ClassEntity innerBClass;
  private ClassEntity innerCClass;
  private EntityScope methodScope;

  @Before
  public void setUpTestScope() throws Exception {
    moduleScope = TestUtil.parseFiles(TEST_DIR, TEST_FILES);
    topLevelClass = (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME, moduleScope);
    testClassClass = (ClassEntity) TestUtil.lookupEntity(TEST_CLASS_CLASS_FULL_NAME, moduleScope);
    testClassFactoryClass =
        (ClassEntity)
            TestUtil.lookupEntity(TEST_CLASS_CLASS_FULL_NAME + ".TestClassFactory", moduleScope);
    shadowClass = (ClassEntity) TestUtil.lookupEntity(SHADOW_CLASS_FULL_NAME, moduleScope);
    innerAClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerA", moduleScope);
    innerBClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerB", moduleScope);
    innerCClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerC", moduleScope);
    methodScope =
        TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".method", moduleScope).getChildScope();
  }

  @Test
  public void solveMemberSelection() {
    assertThat(solveExpression("innerA", topLevelClass).getEntity()).isEqualTo(innerAClass);
    assertThat(solveExpression("innerA.innerB", topLevelClass).getEntity()).isEqualTo(innerBClass);
    assertThat(solveExpression("innerA.innerB.innerC", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solveOtherClassMemberSelection() {
    assertThat(solveExpression("testClass", methodScope).getEntity()).isEqualTo(testClassClass);
    assertThat(solveExpression("testClass.shadow", methodScope).getEntity()).isEqualTo(shadowClass);
    assertThat(solveExpression("testClass.FACTORY", methodScope).getEntity())
        .isEqualTo(testClassFactoryClass);
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
  public void solveOtherClassMethodInvocation() {
    assertThat(solveExpression("getTestClass()", methodScope).getEntity())
        .isEqualTo(testClassClass);
    assertThat(solveExpression("getTestClass().getShadow()", methodScope).getEntity())
        .isEqualTo(shadowClass);
  }

  @Test
  public void solveSuperClassMethodInvocation() {
    assertThat(solveExpression("innerA.baseMethod()", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
    assertThat(solveExpression("baseMethod()", innerAClass).getEntity()).isEqualTo(innerCClass);
    assertThat(solveExpression("super.baseMethod()", innerAClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solveArray() {
    SolvedType innerBArray = solveExpression("innerA.innerBArray", topLevelClass);
    assertThat(innerBArray.getEntity()).isSameAs(innerBClass);
    assertThat(innerBArray.isArray()).named("innerBArray.isArray()").isTrue();
  }

  @Test
  public void solveArrayAccess() {
    SolvedType innerBArrayAccess = solveExpression("innerA.innerBArray[0]", topLevelClass);
    assertThat(innerBArrayAccess.getEntity()).isSameAs(innerBClass);
    assertThat(innerBArrayAccess.isArray()).named("innerBArray.isArray()").isFalse();
  }

  @Test
  public void solveArrayLength() {
    assertThat(solveExpression("innerA.innerBArray.length", topLevelClass).getEntity())
        .isSameAs(PrimitiveEntity.INT);
  }

  @Test
  public void solveLocalVariable() {
    String fileContent = TestUtil.readFileContent(Paths.get(TEST_DIR, "TestExpression.java"));
    int posBeforeVarA = fileContent.indexOf("InnerA varA") - 1;
    int posAfterVarA = fileContent.indexOf(";", posBeforeVarA) + 1;
    int posBeforeVarB = fileContent.indexOf("InnerB varB") - 1;
    int posAfterVarB = fileContent.indexOf(";", posBeforeVarB) + 1;

    assertExpressionNotSolved("varA", methodScope, posBeforeVarA);
    assertExpressionNotSolved("varB", methodScope, posBeforeVarB);
    assertThat(solveExpression("varA", methodScope, posAfterVarA).getEntity())
        .isSameAs(innerAClass);
    assertThat(solveExpression("varB", methodScope, posAfterVarB).getEntity())
        .isSameAs(innerBClass);
  }

  @Test
  public void solveClassMemberInMethod() {
    assertThat(solveExpression("innerA", methodScope).getEntity()).isSameAs(innerAClass);
    assertThat(solveExpression("this.innerA", methodScope).getEntity()).isSameAs(innerAClass);
  }

  @Test
  public void solveNewClass() {
    assertThat(solveExpression("new InnerA()", methodScope).getEntity()).isSameAs(innerAClass);
    assertThat(solveExpression("new TestExpression()", methodScope).getEntity())
        .isSameAs(topLevelClass);
    assertThat(solveExpression("new TestClass()", methodScope).getEntity())
        .isSameAs(testClassClass);
    assertThat(solveExpression("testClass.new TestClassFactory()", methodScope).getEntity())
        .isSameAs(testClassFactoryClass);
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope) {
    return solveExpression(expression, baseScope, -1 /* position */);
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope, int position) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, moduleScope, baseScope, position);
    Truth8.assertThat(solvedExpression).named(expression).isPresent();
    return solvedExpression.get();
  }

  private void assertExpressionNotSolved(String expression, EntityScope baseScope, int position) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, moduleScope, baseScope, position);
    Truth8.assertThat(solvedExpression).named(expression).isEmpty();
  }
}
