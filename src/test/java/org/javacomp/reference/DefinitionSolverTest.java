package org.javacomp.reference;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.javacomp.file.TextPosition;
import org.javacomp.model.Entity;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.VariableEntity;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.ParserContext;
import org.javacomp.testing.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefinitionSolverTest {
  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/reference/testdata/";
  private static final String TEST_CLASS_FILE = "TestClass.java";
  private static final String OTHER_CLASS_FILE = "OtherClass.java";
  private static final String OTHER_PACKAGE_CLASS_FILE = "other/OtherPackageClass.java";
  private static final List<String> ALL_FILES =
      ImmutableList.of(TEST_CLASS_FILE, OTHER_CLASS_FILE, OTHER_PACKAGE_CLASS_FILE);

  private static final String TEST_CLASS_FULL_NAME = "org.javacomp.reference.testdata.TestClass";
  private static final String OTHER_CLASS_FULL_NAME = "org.javacomp.reference.testdata.OtherClass";
  private static final String OTHER_PACKAGE_CLASS_FULL_NAME =
      "org.javacomp.reference.testdata.other.OtherPackageClass";

  private final DefinitionSolver definitionSolver = new DefinitionSolver();

  private GlobalScope globalScope;

  private VariableEntity innerAParam;
  private VariableEntity otherClassParam;
  private VariableEntity innerAVar;

  @Before
  public void parseJavaFiles() {
    ParserContext parserContext = new ParserContext();
    globalScope = new GlobalScope();
    for (String filename : ALL_FILES) {
      String content = getFileContent(filename);
      JCCompilationUnit compilationUnit = parserContext.parse(filename, content);
      FileScope fileScope = new AstScanner().startScan(compilationUnit, filename, content);
      globalScope.addOrReplaceFileScope(fileScope);
    }

    MethodEntity testMethod =
        (MethodEntity) TestUtil.lookupEntity(TEST_CLASS_FULL_NAME + ".testMethod", globalScope);
    innerAParam =
        (VariableEntity)
            Iterables.getOnlyElement(testMethod.getMemberEntities().get("innerAParam"));
    innerAVar =
        (VariableEntity) Iterables.getOnlyElement(testMethod.getMemberEntities().get("innerAVar"));
    otherClassParam =
        (VariableEntity)
            Iterables.getOnlyElement(testMethod.getMemberEntities().get("otherClassParam"));
  }

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
        "methodWithArgs(innerA, innerAParam.testClassInA);",
        "innerA",
        TEST_CLASS_FULL_NAME + ".innerA");
  }

  @Test
  public void testMethodInvocation() {
    assertDefinition(
        TEST_CLASS_FILE,
        "getInnerA().innerBInA;",
        "getInnerA",
        TEST_CLASS_FULL_NAME + ".getInnerA");
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
        "methodWithArgs(innerA, innerAParam.testClassInA);",
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

  private String getFileContent(String filename) {
    Path inputFilePath = Paths.get(TEST_DATA_DIR, filename);
    try {
      return new String(Files.readAllBytes(inputFilePath), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private TextPosition locateSymbol(SymbolLocator symbolLocator) {
    String fileContent = getFileContent(symbolLocator.filename);
    int start = fileContent.indexOf(symbolLocator.symbolContext);
    assertThat(start).named("location of " + symbolLocator.symbolContext).isGreaterThan(-1);
    int pos = fileContent.indexOf(symbolLocator.symbol, start);
    assertThat(pos).named("pos").isGreaterThan(-1);
    FileScope fileScope = globalScope.getFileScope(symbolLocator.filename).get();
    LineMap lineMap = fileScope.getLineMap();
    // LineMap line and column are 1-indexed, while our API is 0-indexed.
    return TextPosition.create(
        (int) lineMap.getLineNumber(pos) - 1, (int) lineMap.getColumnNumber(pos) - 1);
  }

  private static class SymbolLocator {
    private final String filename;
    private final String symbolContext;
    private final String symbol;

    private SymbolLocator(String filename, String symbolContext, String symbol) {
      this.filename = filename;
      this.symbolContext = symbolContext;
      this.symbol = symbol;
    }

    @Override
    public String toString() {
      return String.format(
          "{filename: %s, symbolContext: %s, symbol: %s", filename, symbolContext, symbol);
    }
  }
}
