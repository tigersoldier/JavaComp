package org.javacomp.parser;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.MethodSymbol;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AstScannerTest {
  private static final String TEST_DATA_PATH =
      "src/test/java/org/javacomp/parser/testdata/TestData.java";

  private final AstScanner scanner = new AstScanner();
  private final GlobalIndex globalIndex = new GlobalIndex();

  private JCCompilationUnit compilationUnit;
  private String testDataContent;

  @Before
  public void startScan() throws Exception {
    Context javacContext = new Context();
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    testDataContent = new String(Files.readAllBytes(Paths.get(TEST_DATA_PATH)), UTF_8);
    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                testDataContent,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    compilationUnit = parser.parseCompilationUnit();
    scanner.startScan(compilationUnit, globalIndex, TEST_DATA_PATH);
  }

  @Test
  public void packagesAreIndexed() {
    Symbol packageSymbol = lookupQualifiedName(globalIndex, "test.data");
    assertThat(packageSymbol.getKind()).isEqualTo(Symbol.Kind.QUALIFIER);
  }

  @Test
  public void classIsIndexedInPackage() {
    Symbol classSymbol = lookupQualifiedName(globalIndex, "test.data.TestData");
    assertThat(classSymbol.getKind()).isEqualTo(Symbol.Kind.CLASS);
  }

  @Test
  public void classIsIndexedInGlobalIndex() {
    Optional<Symbol> classSymbol =
        globalIndex.getSymbolWithNameAndKind("TestData", Symbol.Kind.CLASS);
    assertThat(classSymbol).isPresent();
    assertThat(classSymbol.get().getKind()).isEqualTo(Symbol.Kind.CLASS);
  }

  @Test
  public void methodIsIndexedInClassIndex() {
    Symbol methodSymbol =
        lookupQualifiedName(globalIndex, "test.data.TestData.publicIfBlockMethod");
    assertThat(methodSymbol.getKind()).isEqualTo(Symbol.Kind.METHOD);
  }

  @Test
  public void classStaticFieldIsIndexedInClassIndex() {
    Symbol variableSymbol =
        lookupQualifiedName(globalIndex, "test.data.TestData.publicStaticIntField");
    assertThat(variableSymbol.getKind()).isEqualTo(Symbol.Kind.VARIABLE);
  }

  @Test
  public void innerClassIsIndexedInClassIndex() {
    Symbol classSymbol =
        lookupQualifiedName(globalIndex, "test.data.TestData.PrivateStaticInnerClass");
    assertThat(classSymbol.getKind()).isEqualTo(Symbol.Kind.CLASS);
    Symbol annotationSymbol =
        lookupQualifiedName(globalIndex, "test.data.TestData.PublicInnerAnnotation");
    assertThat(annotationSymbol.getKind()).isEqualTo(Symbol.Kind.ANNOTATION);
    Symbol enumSymbol = lookupQualifiedName(globalIndex, "test.data.TestData.PublicInnerEnum");
    assertThat(enumSymbol.getKind()).isEqualTo(Symbol.Kind.ENUM);
    Symbol interfaceSymbol =
        lookupQualifiedName(globalIndex, "test.data.TestData.PublicInnerInterface");
    assertThat(interfaceSymbol.getKind()).isEqualTo(Symbol.Kind.INTERFACE);
  }

  @Test
  public void enumItemIsIndexedInEnumIndex() {
    Symbol variableSymbol =
        lookupQualifiedName(globalIndex, "test.data.TestData.PublicInnerEnum.ENUM_VALUE1");
    assertThat(variableSymbol.getKind()).isEqualTo(Symbol.Kind.VARIABLE);
  }

  @Test
  public void topLevelClassIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("public class TestData {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // class TestData");
    SymbolIndex indexAtField = getSymbolIndexAfter("publicStaticIntField;");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(lookupQualifiedName(globalIndex, "test.data.TestData").getChildIndex());
    }
  }

  @Test
  public void innerEnumIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("public enum PublicInnerEnum {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // PublicInnerEnum");
    SymbolIndex indexAtField = getSymbolIndexAfter("ENUM_VALUE1,");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(
              lookupQualifiedName(globalIndex, "test.data.TestData.PublicInnerEnum")
                  .getChildIndex());
    }
  }

  @Test
  public void innerInterfaceIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("public interface PublicInnerInterface {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // PublicInnerInterface");
    SymbolIndex indexAtField = getSymbolIndexAfter("interfaceMethod();");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(
              lookupQualifiedName(globalIndex, "test.data.TestData.PublicInnerInterface")
                  .getChildIndex());
    }
  }

  @Test
  public void methodIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("publicIfBlockMethod() {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // publicIfBlockMethod");
    SymbolIndex indexAtField = getSymbolIndexAfter("methodScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      MethodSymbol methodSymbol =
          (MethodSymbol) lookupQualifiedName(globalIndex, "test.data.TestData.publicIfBlockMethod");
      assertThat(index).isEqualTo(methodSymbol.getOverloadIndexes().get(0));
    }
  }

  @Test
  public void ifBlockIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("if (a == 1) {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} else { // end of if");
    SymbolIndex indexAtField = getSymbolIndexAfter("ifScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("ifScopeVar", Symbol.Kind.VARIABLE)).isPresent();
      assertThat(index.getSymbolsWithName("elseScopeVar")).isEmpty();
    }
  }

  @Test
  public void elseBlockIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("else {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // else");
    SymbolIndex indexAtField = getSymbolIndexAfter("elseScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("elseScopeVar", Symbol.Kind.VARIABLE)).isPresent();
      assertThat(index.getSymbolsWithName("ifScopeVar")).isEmpty();
    }
  }

  @Test
  public void whileBlockIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("while (number > 0) {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // while loop");
    SymbolIndex indexAtField = getSymbolIndexAfter("whileScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("whileScopeVar", Symbol.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void forBlockIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("for (String s : input) {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // for loop");
    SymbolIndex indexAtField = getSymbolIndexAfter("forScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("forScopeVar", Symbol.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void switchBlockIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("switch (a) {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // switch");
    SymbolIndex indexAtField = getSymbolIndexAfter("switchScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("switchScopeVar", Symbol.Kind.VARIABLE))
          .isPresent();
      assertThat(index.getSymbolsWithName("caseScopeVar")).isEmpty();
    }
  }

  @Test
  public void switchCaseBlockIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexBefore("{ // start of case block");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // end of case block");
    SymbolIndex indexAtField = getSymbolIndexAfter("caseScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("switchScopeVar", Symbol.Kind.VARIABLE))
          .isPresent();
      assertThat(index.getSymbolWithNameAndKind("caseScopeVar", Symbol.Kind.VARIABLE)).isPresent();
    }
  }

  private SymbolIndex getSymbolIndexAfter(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return globalIndex.getFileIndex(TEST_DATA_PATH).getSymbolIndexAt(pos + subString.length());
  }

  private SymbolIndex getSymbolIndexBefore(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return globalIndex.getFileIndex(TEST_DATA_PATH).getSymbolIndexAt(pos);
  }

  private static Symbol lookupQualifiedName(SymbolIndex index, String qualifiedName) {
    String[] qualifiers = qualifiedName.split("\\.");
    SymbolIndex currentIndex = index;
    Symbol symbol = null;
    for (String qualifier : qualifiers) {
      Collection<Symbol> symbols = currentIndex.getAllSymbols().get(qualifier);
      assertThat(symbols).isNotEmpty();
      symbol = Iterables.getFirst(symbols, null);
      currentIndex = symbol.getChildIndex();
    }
    return symbol;
  }
}
