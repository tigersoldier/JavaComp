package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth8;
import com.sun.source.tree.ExpressionTree;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.NullEntity;
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
      ImmutableList.of("TestExpression.java", "TestClass.java");
  private static final List<String> OTHER_FILES =
      ImmutableList.of("other/BaseClass.java", "other/Shadow.java");
  private static final List<String> FAKE_JDK_FILES = ImmutableList.of("fakejdk/String.java");
  private static final String TOP_LEVEL_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.TestExpression";
  private static final String TEST_CLASS_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.TestClass";
  private static final String SHADOW_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.other.Shadow";
  private static final String BASE_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.other.BaseClass";

  private final TypeSolver typeSolver = new TypeSolver();
  private final OverloadSolver overloadSolver = new OverloadSolver(typeSolver);
  private final MemberSolver memberSolver = new MemberSolver(typeSolver, overloadSolver);
  private final ExpressionSolver expressionSolver =
      new ExpressionSolver(typeSolver, overloadSolver, memberSolver);

  private Module module;
  private Module otherModule;
  private Module fakeJdkModule;
  private ClassEntity topLevelClass;
  private ClassEntity testClassClass;
  private ClassEntity testClassFactoryClass;
  private ClassEntity shadowClass;
  private ClassEntity innerAClass;
  private ClassEntity innerBClass;
  private ClassEntity innerCClass;
  private ClassEntity baseInnerClass;
  private ClassEntity fakeStringClass;
  private MethodEntity lambdaCallMethod;
  private EntityScope methodScope;

  @Before
  public void setUpTestScope() throws Exception {
    module = TestUtil.parseFiles(TEST_DIR, TEST_FILES);
    otherModule = TestUtil.parseFiles(TEST_DIR, OTHER_FILES);
    fakeJdkModule = TestUtil.parseFiles(TEST_DIR, FAKE_JDK_FILES);
    module.addDependingModule(otherModule);
    module.addDependingModule(fakeJdkModule);

    topLevelClass = (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME, module);
    testClassClass = (ClassEntity) TestUtil.lookupEntity(TEST_CLASS_CLASS_FULL_NAME, module);
    testClassFactoryClass =
        (ClassEntity)
            TestUtil.lookupEntity(TEST_CLASS_CLASS_FULL_NAME + ".TestClassFactory", module);
    shadowClass = (ClassEntity) TestUtil.lookupEntity(SHADOW_CLASS_FULL_NAME, otherModule);
    baseInnerClass =
        (ClassEntity) TestUtil.lookupEntity(BASE_CLASS_FULL_NAME + ".BaseInnerClass", otherModule);
    innerAClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerA", module);
    innerBClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerB", module);
    innerCClass =
        (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerC", module);
    fakeStringClass = (ClassEntity) TestUtil.lookupEntity("java.lang.String", fakeJdkModule);
    lambdaCallMethod =
        (MethodEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".lambdaCall", module);
    methodScope =
        TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".method", module).getChildScope();
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
  public void solveQualifiedClass() {
    assertThat(
            solveExpression("org.javacomp.typesolver.testdata.TestExpression", topLevelClass)
                .getEntity())
        .isEqualTo(topLevelClass);
    assertThat(
            solveExpression("org.javacomp.typesolver.testdata.other.Shadow", topLevelClass)
                .getEntity())
        .isEqualTo(shadowClass);
    assertThat(
            solveExpression(
                    "org.javacomp.typesolver.testdata.other.BaseClass.BaseInnerClass",
                    topLevelClass)
                .getEntity())
        .isEqualTo(baseInnerClass);
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

  @Test
  public void solveMethodWithLambdaAsParameter() {
    assertThat(solveDefinition("lambdaCall((arg) -> {return;})", methodScope))
        .isSameAs(lambdaCallMethod);
  }

  @Test
  public void solveJavaLangClass() {
    assertThat(solveDefinition("String", methodScope)).isSameAs(fakeStringClass);
  }

  @Test
  public void solveLiterals() {
    assertThat(solveExpression("123", methodScope).getEntity()).isSameAs(PrimitiveEntity.INT);
    assertThat(solveExpression("123L", methodScope).getEntity()).isSameAs(PrimitiveEntity.LONG);
    assertThat(solveExpression("12.3f", methodScope).getEntity()).isSameAs(PrimitiveEntity.FLOAT);
    assertThat(solveExpression("12.3", methodScope).getEntity()).isSameAs(PrimitiveEntity.DOUBLE);
    assertThat(solveExpression("false", methodScope).getEntity()).isSameAs(PrimitiveEntity.BOOLEAN);
    assertThat(solveExpression("true", methodScope).getEntity()).isSameAs(PrimitiveEntity.BOOLEAN);
    assertThat(solveExpression("'c'", methodScope).getEntity()).isSameAs(PrimitiveEntity.CHAR);
    assertThat(solveExpression("null", methodScope).getEntity()).isSameAs(NullEntity.INSTANCE);
    assertThat(solveExpression("\"123\"", methodScope).getEntity()).isSameAs(fakeStringClass);
  }

  private Entity solveDefinition(String expression, EntityScope baseScope) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    List<Entity> solvedExpression =
        expressionSolver.solveDefinitions(
            expressionTree, module, baseScope, -1 /* position */, EnumSet.allOf(Entity.Kind.class));
    assertThat(solvedExpression).named(expression).isNotEmpty();
    return solvedExpression.get(0);
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope) {
    return solveExpression(expression, baseScope, -1 /* position */);
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope, int position) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, module, baseScope, position);
    Truth8.assertThat(solvedExpression).named(expression).isPresent();
    return solvedExpression.get();
  }

  private void assertExpressionNotSolved(String expression, EntityScope baseScope, int position) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, module, baseScope, position);
    Truth8.assertThat(solvedExpression).named(expression).isEmpty();
  }
}
