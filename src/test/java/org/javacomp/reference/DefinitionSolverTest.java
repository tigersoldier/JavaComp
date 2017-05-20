package org.javacomp.reference;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;
import org.javacomp.file.TextPosition;
import org.javacomp.model.Entity;
import org.javacomp.testing.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefinitionSolverTest extends BaseTest {

  private final DefinitionSolver definitionSolver = new DefinitionSolver();

  @Test
  public void testIndexVariable() {
    assertDefinition(
        TEST_CLASS_FILE,
        "innerAParam.getTestBInA().testAInB.getTestClassInA().innerA;",
        "innerAParam",
        innerAParam);
    assertDefinition(
        TEST_CLASS_FILE,
        "otherClassParam.getTestClass().otherClass.getInnerA();",
        "otherClassParam",
        otherClassParam);
    assertDefinition(
        TEST_CLASS_FILE,
        "innerAVar.testClassInA.getOtherPackageClass().innerA;",
        "innerAVar",
        innerAVar);
    assertDefinition(
        TEST_CLASS_FILE,
        "InnerClassB ignore = innerB;",
        "innerB",
        TEST_CLASS_FULL_NAME + ".innerB");
    assertDefinition(
        TEST_CLASS_FILE,
        "overloadMethod(innerA, innerAParam.testClassInA);",
        "innerA",
        TEST_CLASS_FULL_NAME + ".innerA");
  }

  @Test
  public void testMethodInvocation() {
    assertDefinition(
        TEST_CLASS_FILE, "getInnerA().testBInA;", "getInnerA", TEST_CLASS_FULL_NAME + ".getInnerA");
    assertDefinition(
        TEST_CLASS_FILE,
        "getOtherClass().getTestClass();",
        "getOtherClass",
        TEST_CLASS_FULL_NAME + ".getOtherClass");
  }

  @Test
  public void testMemberSelectVariable() {
    assertDefinition(
        TEST_CLASS_FILE,
        "innerAParam.getTestBInA().testAInB.getTestClassInA().innerA;",
        "testAInB",
        TEST_CLASS_FULL_NAME + ".InnerClassB.testAInB");
    assertDefinition(
        TEST_CLASS_FILE,
        "getTestBInA().testAInB.getTestClassInA().innerA;",
        "innerA",
        TEST_CLASS_FULL_NAME + ".innerA");
    assertDefinition(
        TEST_CLASS_FILE,
        "getTestClass().otherClass.getInnerA();",
        "otherClass",
        TEST_CLASS_FULL_NAME + ".otherClass");
    assertDefinition(
        TEST_CLASS_FILE,
        "testClassInA.getOtherPackageClass().innerA;",
        "innerA",
        OTHER_PACKAGE_CLASS_FULL_NAME + ".innerA");
    assertDefinition(
        TEST_CLASS_FILE,
        "overloadMethod(innerA, innerAParam.testClassInA);",
        "testClassInA",
        TEST_CLASS_FULL_NAME + ".InnerClassA.testClassInA");
  }

  @Test
  public void testMemberSelectMethod() {
    assertDefinition(
        TEST_CLASS_FILE,
        "innerAParam.getTestBInA().testAInB.getTestClassInA().innerA;",
        "getTestBInA",
        TEST_CLASS_FULL_NAME + ".InnerClassA.getTestBInA");
    assertDefinition(
        TEST_CLASS_FILE,
        "innerAParam.getTestBInA().testAInB.getTestClassInA().innerA;",
        "getTestClassInA",
        TEST_CLASS_FULL_NAME + ".InnerClassA.getTestClassInA");
    assertDefinition(
        TEST_CLASS_FILE,
        "otherClassParam.getTestClass().otherClass.getInnerA();",
        "getTestClass",
        OTHER_CLASS_FULL_NAME + ".getTestClass");
    assertDefinition(
        TEST_CLASS_FILE,
        "otherClassParam.getTestClass().otherClass.getInnerA();",
        "getInnerA",
        OTHER_CLASS_FULL_NAME + ".getInnerA");
    assertDefinition(
        TEST_CLASS_FILE,
        "testClassInA.getOtherPackageClass().innerA;",
        "getOtherPackageClass",
        TEST_CLASS_FULL_NAME + ".getOtherPackageClass");
  }

  @Test
  public void testVariableType() {
    assertDefinition(
        TEST_CLASS_FILE,
        "InnerClassB testBInA",
        "InnerClassB",
        TEST_CLASS_FULL_NAME + ".InnerClassB");
    assertDefinition(
        TEST_CLASS_FILE,
        "InnerClassA innerA",
        "InnerClassA",
        TEST_CLASS_FULL_NAME + ".InnerClassA");
    assertDefinition(TEST_CLASS_FILE, "OtherClass otherClass", "OtherClass", OTHER_CLASS_FULL_NAME);
    assertDefinition(
        TEST_CLASS_FILE,
        "OtherPackageClass otherPackageClass",
        "OtherPackageClass",
        OTHER_PACKAGE_CLASS_FULL_NAME);
    assertDefinition(
        TEST_CLASS_FILE,
        "InnerClassA innerAParam",
        "InnerClassA",
        TEST_CLASS_FULL_NAME + ".InnerClassA");
    assertDefinition(
        TEST_CLASS_FILE,
        "OtherClass.InnerClassA otherInnerAVar",
        "OtherClass",
        OTHER_CLASS_FULL_NAME);
    assertDefinition(
        TEST_CLASS_FILE, "OtherClass otherClassParam", "OtherClass", OTHER_CLASS_FULL_NAME);
    assertDefinition(
        TEST_CLASS_FILE,
        "OtherClass.InnerClassA otherInnerAVar",
        "InnerClassA",
        OTHER_CLASS_FULL_NAME + ".InnerClassA");
  }

  @Test
  public void testMethodReturnType() {
    assertDefinition(
        TEST_CLASS_FILE,
        "InnerClassB getTestBInA()",
        "InnerClassB",
        TEST_CLASS_FULL_NAME + ".InnerClassB");
    assertDefinition(
        TEST_CLASS_FILE,
        "InnerClassA getInnerA()",
        "InnerClassA",
        TEST_CLASS_FULL_NAME + ".InnerClassA");
    assertDefinition(
        TEST_CLASS_FILE, "OtherClass getOtherClass()", "OtherClass", OTHER_CLASS_FULL_NAME);
    assertDefinition(
        TEST_CLASS_FILE,
        "OtherPackageClass getOtherPackageClass()",
        "OtherPackageClass",
        OTHER_PACKAGE_CLASS_FULL_NAME);
  }

  private void assertDefinition(
      String filename, String symbolContext, String symbol, Entity expected) {
    SymbolLocator symbolLocator = new SymbolLocator(filename, symbolContext, symbol);
    TextPosition pos = locateSymbol(symbolLocator);
    assertThat(
            definitionSolver.getDefinitionEntities(
                globalScope, Paths.get(symbolLocator.filename), pos.getLine(), pos.getCharacter()))
        .named(symbolLocator.toString())
        .containsExactly(expected);
  }

  private void assertDefinition(
      String filename, String symbolContext, String symbol, String expectedQualifiedNamed) {
    Entity expected = TestUtil.lookupEntity(expectedQualifiedNamed, globalScope);
    assertDefinition(filename, symbolContext, symbol, expected);
  }
}
