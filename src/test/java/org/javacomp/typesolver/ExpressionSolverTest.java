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
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedArrayType;
import org.javacomp.model.SolvedEntityType;
import org.javacomp.model.SolvedNullType;
import org.javacomp.model.SolvedReferenceType;
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
  private ClassEntity innerInnerAClass;
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
    innerInnerAClass =
        (ClassEntity)
            TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME + ".InnerA.InnerInnerA", module);
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
    assertThat(solveEntityExpression("innerA", topLevelClass).getEntity()).isEqualTo(innerAClass);
    assertThat(solveEntityExpression("innerA.innerB", topLevelClass).getEntity())
        .isEqualTo(innerBClass);
    assertThat(solveEntityExpression("innerA.innerB.innerC", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solveMemberSelectionWithTypeParameter() {
    assertThat(solveEntityExpression("typeParameterA", innerAClass).getEntity())
        .isEqualTo(innerBClass);
    assertThat(solveEntityExpression("innerA.typeParameterA", innerAClass).getEntity())
        .isEqualTo(innerBClass);
  }

  @Test
  public void solveOtherClassMemberSelection() {
    assertThat(solveEntityExpression("testClass", methodScope).getEntity())
        .isEqualTo(testClassClass);
    assertThat(solveEntityExpression("testClass.shadow", methodScope).getEntity())
        .isEqualTo(shadowClass);
    assertThat(solveEntityExpression("testClass.FACTORY", methodScope).getEntity())
        .isEqualTo(testClassFactoryClass);
  }

  @Test
  public void solvedInheritedField() {
    assertThat(solveEntityExpression("baseInnerB", innerAClass).getEntity()).isEqualTo(innerBClass);
    assertThat(solveEntityExpression("innerA.baseInnerB", topLevelClass).getEntity())
        .isEqualTo(innerBClass);
  }

  @Test
  public void solveQualifiedClass() {
    assertThat(
            solveEntityExpression("org.javacomp.typesolver.testdata.TestExpression", topLevelClass)
                .getEntity())
        .isEqualTo(topLevelClass);
    assertThat(
            solveEntityExpression("org.javacomp.typesolver.testdata.other.Shadow", topLevelClass)
                .getEntity())
        .isEqualTo(shadowClass);
    assertThat(
            solveEntityExpression(
                    "org.javacomp.typesolver.testdata.other.BaseClass.BaseInnerClass",
                    topLevelClass)
                .getEntity())
        .isEqualTo(baseInnerClass);
  }

  @Test
  public void solveQualifiedClassField() {
    assertThat(
            solveEntityExpression(
                    "org.javacomp.typesolver.testdata.TestExpression.innerA", topLevelClass)
                .getEntity())
        .isEqualTo(innerAClass);
  }

  @Test
  public void solveThis() {
    assertThat(solveEntityExpression("this", topLevelClass).getEntity()).isEqualTo(topLevelClass);
    assertThat(solveEntityExpression("this.innerA", topLevelClass).getEntity())
        .isEqualTo(innerAClass);

    SolvedEntityType innerAThis = solveEntityExpression("this", innerAClass);
    assertThat(innerAThis.getEntity()).isEqualTo(innerAClass);
    assertThat(innerAThis).isInstanceOf(SolvedReferenceType.class);
    Optional<SolvedType> typeParameterA =
        ((SolvedReferenceType) innerAThis).getTypeParameters().getTypeParameter("A");
    Truth8.assertThat(typeParameterA).isPresent();
    assertThat(typeParameterA.get()).isInstanceOf(SolvedReferenceType.class);
    assertThat(((SolvedReferenceType) typeParameterA.get()).getEntity()).isEqualTo(innerBClass);
  }

  @Test
  public void solveSuper() {
    assertThat(solveEntityExpression("super.innerA", innerAClass).getEntity())
        .isEqualTo(innerAClass);

    assertThat(solveEntityExpression("super.getT(null)", innerAClass).getEntity())
        .isEqualTo(innerBClass);
  }

  @Test
  public void solveQualifiedThis() {
    SolvedEntityType qualifiedThis = solveEntityExpression("InnerA.this", innerInnerAClass);
    assertThat(qualifiedThis.getEntity()).isEqualTo(innerAClass);
    assertThat(qualifiedThis).isInstanceOf(SolvedReferenceType.class);
    Optional<SolvedType> typeParameterA =
        ((SolvedReferenceType) qualifiedThis).getTypeParameters().getTypeParameter("A");
    Truth8.assertThat(typeParameterA).isPresent();
    assertThat(typeParameterA.get()).isInstanceOf(SolvedEntityType.class);
    assertThat(((SolvedEntityType) typeParameterA.get()).getEntity()).isEqualTo(innerBClass);
  }

  @Test
  public void solveMethodInvocation() {
    assertThat(solveEntityExpression("baseMethod()", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
    assertThat(solveEntityExpression("baseMethod(42)", topLevelClass).getEntity())
        .isEqualTo(innerBClass);
    assertThat(solveEntityExpression("this.baseMethod()", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solveMethodReturnTypeWithTypeParameters() {
    assertThat(solveEntityExpression("getTypeParameterA()", innerAClass).getEntity())
        .isEqualTo(innerBClass);
    assertThat(solveEntityExpression("innerA.getTypeParameterA()", innerAClass).getEntity())
        .isEqualTo(innerBClass);
  }

  // TODO: enable this test once TypeSolver is fixed.
  // @Test
  public void solveSuperClassMethodReturnTypeWithTypeParameters() {
    assertThat(solveEntityExpression("getT()", innerAClass).getEntity()).isEqualTo(innerBClass);
  }

  @Test
  public void solveOtherClassMethodInvocation() {
    assertThat(solveEntityExpression("getTestClass()", methodScope).getEntity())
        .isEqualTo(testClassClass);
    assertThat(solveEntityExpression("getTestClass().getShadow()", methodScope).getEntity())
        .isEqualTo(shadowClass);
  }

  @Test
  public void solveSuperClassMethodInvocation() {
    assertThat(solveEntityExpression("innerA.baseMethod()", topLevelClass).getEntity())
        .isEqualTo(innerCClass);
    assertThat(solveEntityExpression("baseMethod()", innerAClass).getEntity())
        .isEqualTo(innerCClass);
    assertThat(solveEntityExpression("super.baseMethod()", innerAClass).getEntity())
        .isEqualTo(innerCClass);
  }

  @Test
  public void solveArray() {
    SolvedType solvedInnerBArray =
        solveExpression("innerA.innerBArray", topLevelClass, -1 /* position */);
    assertThat(solvedInnerBArray)
        .named("innerBArray.isArray()")
        .isInstanceOf(SolvedArrayType.class);
    SolvedArrayType innerBArray = (SolvedArrayType) solvedInnerBArray;
    assertThat(innerBArray.getBaseType()).isInstanceOf(SolvedEntityType.class);
    assertThat(((SolvedEntityType) innerBArray.getBaseType()).getEntity()).isSameAs(innerBClass);
  }

  @Test
  public void solveArrayAccess() {
    SolvedEntityType innerBArrayAccess =
        solveEntityExpression("innerA.innerBArray[0]", topLevelClass);
    assertThat(innerBArrayAccess.getEntity()).isSameAs(innerBClass);
  }

  @Test
  public void solveArrayLength() {
    assertThat(solveEntityExpression("innerA.innerBArray.length", topLevelClass).getEntity())
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
    assertThat(solveEntityExpression("varA", methodScope, posAfterVarA).getEntity())
        .isSameAs(innerAClass);
    assertThat(solveEntityExpression("varB", methodScope, posAfterVarB).getEntity())
        .isSameAs(innerBClass);
  }

  @Test
  public void solveClassMemberInMethod() {
    assertThat(solveEntityExpression("innerA", methodScope).getEntity()).isSameAs(innerAClass);
    assertThat(solveEntityExpression("this.innerA", methodScope).getEntity()).isSameAs(innerAClass);
  }

  @Test
  public void solveNewClass() {
    assertThat(solveEntityExpression("new InnerA()", methodScope).getEntity())
        .isSameAs(innerAClass);
    assertThat(solveEntityExpression("new TestExpression()", methodScope).getEntity())
        .isSameAs(topLevelClass);
    assertThat(solveEntityExpression("new TestClass()", methodScope).getEntity())
        .isSameAs(testClassClass);
    assertThat(solveEntityExpression("testClass.new TestClassFactory()", methodScope).getEntity())
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
    assertThat(solveEntityExpression("123", methodScope).getEntity()).isSameAs(PrimitiveEntity.INT);
    assertThat(solveEntityExpression("123L", methodScope).getEntity())
        .isSameAs(PrimitiveEntity.LONG);
    assertThat(solveEntityExpression("12.3f", methodScope).getEntity())
        .isSameAs(PrimitiveEntity.FLOAT);
    assertThat(solveEntityExpression("12.3", methodScope).getEntity())
        .isSameAs(PrimitiveEntity.DOUBLE);
    assertThat(solveEntityExpression("false", methodScope).getEntity())
        .isSameAs(PrimitiveEntity.BOOLEAN);
    assertThat(solveEntityExpression("true", methodScope).getEntity())
        .isSameAs(PrimitiveEntity.BOOLEAN);
    assertThat(solveEntityExpression("'c'", methodScope).getEntity())
        .isSameAs(PrimitiveEntity.CHAR);
    assertThat(solveExpression("null", methodScope, -1 /* position */))
        .isInstanceOf(SolvedNullType.class);
    assertThat(solveEntityExpression("\"123\"", methodScope).getEntity()).isSameAs(fakeStringClass);
  }

  private Entity solveDefinition(String expression, EntityScope baseScope) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    List<EntityWithContext> solvedExpression =
        expressionSolver.solveDefinitions(
            expressionTree, module, baseScope, -1 /* position */, EnumSet.allOf(Entity.Kind.class));
    assertThat(solvedExpression).named(expression).isNotEmpty();
    return solvedExpression.get(0).getEntity();
  }

  private SolvedType solveExpression(String expression, EntityScope baseScope, int position) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, module, baseScope, position);
    Truth8.assertThat(solvedExpression).named(expression).isPresent();
    return solvedExpression.get();
  }

  private SolvedEntityType solveEntityExpression(String expression, EntityScope baseScope) {
    return solveEntityExpression(expression, baseScope, -1 /* position */);
  }

  private SolvedEntityType solveEntityExpression(
      String expression, EntityScope baseScope, int position) {
    SolvedType solvedType = solveExpression(expression, baseScope, position);
    assertThat(solvedType).named(expression).isInstanceOf(SolvedEntityType.class);
    return (SolvedEntityType) solvedType;
  }

  private void assertExpressionNotSolved(String expression, EntityScope baseScope, int position) {
    ExpressionTree expressionTree = TestUtil.parseExpression(expression);
    Optional<SolvedType> solvedExpression =
        expressionSolver.solve(expressionTree, module, baseScope, position);
    Truth8.assertThat(solvedExpression).named(expression).isEmpty();
  }
}
