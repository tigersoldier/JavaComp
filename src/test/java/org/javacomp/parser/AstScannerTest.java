package org.javacomp.parser;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import org.javacomp.model.FileIndex;
import org.javacomp.model.MethodSymbol;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.javacomp.model.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AstScannerTest {
  private static final String TEST_DATA_PATH =
      "src/test/java/org/javacomp/parser/testdata/TestData.java";

  private final AstScanner scanner = new AstScanner();

  private JCCompilationUnit compilationUnit;
  private String testDataContent;
  private FileIndex fileIndex;

  @Before
  public void startScan() throws Exception {
    Context javacContext = new Context();
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    testDataContent = new String(Files.readAllBytes(Paths.get(TEST_DATA_PATH)), UTF_8);

    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject("/" + TEST_DATA_PATH);
    Log javacLog = Log.instance(javacContext);
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                testDataContent,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    compilationUnit = parser.parseCompilationUnit();
    fileIndex = scanner.startScan(compilationUnit, TEST_DATA_PATH);
  }

  @Test
  public void fileIndexHasCorrectPackage() {
    assertThat(fileIndex.getPackageQualifiers()).containsExactly("test", "data").inOrder();
  }

  @Test
  public void classIsIndexedInPackage() {
    Symbol classSymbol = lookupSymbol(fileIndex, "TestData");
    assertThat(classSymbol.getKind()).isEqualTo(Symbol.Kind.CLASS);
  }

  @Test
  public void classIsIndexedGlobally() {
    List<Symbol> classSymbol = fileIndex.getGlobalSymbolsWithName("TestData");
    assertThat(classSymbol).hasSize(1);
    assertThat(classSymbol.get(0).getKind()).isEqualTo(Symbol.Kind.CLASS);
  }

  @Test
  public void methodIsIndexedInClassIndex() {
    Symbol methodSymbol = lookupSymbol(fileIndex, "TestData.publicIfBlockMethod");
    assertThat(methodSymbol.getKind()).isEqualTo(Symbol.Kind.METHOD);
  }

  @Test
  public void classStaticFieldIsIndexedInClassIndex() {
    Symbol variableSymbol = lookupSymbol(fileIndex, "TestData.publicStaticIntField");
    assertThat(variableSymbol.getKind()).isEqualTo(Symbol.Kind.VARIABLE);
  }

  @Test
  public void innerClassIsIndexedInClassIndex() {
    Symbol classSymbol = lookupSymbol(fileIndex, "TestData.PrivateStaticInnerClass");
    assertThat(classSymbol.getKind()).isEqualTo(Symbol.Kind.CLASS);
    Symbol annotationSymbol = lookupSymbol(fileIndex, "TestData.PublicInnerAnnotation");
    assertThat(annotationSymbol.getKind()).isEqualTo(Symbol.Kind.ANNOTATION);
    Symbol enumSymbol = lookupSymbol(fileIndex, "TestData.PublicInnerEnum");
    assertThat(enumSymbol.getKind()).isEqualTo(Symbol.Kind.ENUM);
    Symbol interfaceSymbol = lookupSymbol(fileIndex, "TestData.PublicInnerInterface");
    assertThat(interfaceSymbol.getKind()).isEqualTo(Symbol.Kind.INTERFACE);
  }

  @Test
  public void enumItemIsIndexedInEnumIndex() {
    Symbol variableSymbol = lookupSymbol(fileIndex, "TestData.PublicInnerEnum.ENUM_VALUE1");
    assertThat(variableSymbol.getKind()).isEqualTo(Symbol.Kind.VARIABLE);
  }

  @Test
  public void topLevelClassIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("public class TestData {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // class TestData");
    SymbolIndex indexAtField = getSymbolIndexAfter("publicStaticIntField;");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index).isEqualTo(lookupSymbol(fileIndex, "TestData").getChildIndex());
    }
  }

  @Test
  public void innerEnumIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("public enum PublicInnerEnum {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // PublicInnerEnum");
    SymbolIndex indexAtField = getSymbolIndexAfter("ENUM_VALUE1,");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(lookupSymbol(fileIndex, "TestData.PublicInnerEnum").getChildIndex());
    }
  }

  @Test
  public void innerInterfaceIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("public interface PublicInnerInterface {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // PublicInnerInterface");
    SymbolIndex indexAtField = getSymbolIndexAfter("interfaceMethod();");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(lookupSymbol(fileIndex, "TestData.PublicInnerInterface").getChildIndex());
    }
  }

  @Test
  public void methodIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("publicIfBlockMethod() {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} // publicIfBlockMethod");
    SymbolIndex indexAtField = getSymbolIndexAfter("methodScopeVar");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      MethodSymbol methodSymbol =
          (MethodSymbol) lookupSymbol(fileIndex, "TestData.publicIfBlockMethod");
      assertThat(index).isEqualTo(methodSymbol.getOverloads().get(0).getMethodIndex());
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

  @Test
  public void annonymousClassIndexRange() {
    SymbolIndex indexAtStart = getSymbolIndexAfter("new PublicInnerInterface() {");
    SymbolIndex indexAtEnd = getSymbolIndexBefore("} /* end of new PublicInnerInterface *");
    SymbolIndex indexAtField = getSymbolIndexAfter("privateAnnonymousClassMethod");
    for (SymbolIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getSymbolWithNameAndKind("privateAnnonymousClassMethod", Symbol.Kind.METHOD))
          .isPresent();
      assertThat(index.getSymbolWithNameAndKind("interfaceMethod", Symbol.Kind.METHOD)).isPresent();
    }
  }

  @Test
  public void primitiveTypeReference() {
    MethodSymbol methodSymbol =
        (MethodSymbol) lookupSymbol(fileIndex, "TestData.protectedWhileBlockMethod");
    TypeReference intReference =
        methodSymbol.getOverloads().get(0).getParameters().get(0).getType();
    assertThat(intReference.getFullName()).containsExactly("int").inOrder();
  }

  @Test
  public void nonPrimitiveTypeReference() {
    MethodSymbol methodSymbol =
        (MethodSymbol) lookupSymbol(fileIndex, "TestData.privateForBlockMethod");
    TypeReference intReference =
        methodSymbol.getOverloads().get(0).getParameters().get(0).getType();
    assertThat(intReference.getFullName()).containsExactly("java", "util", "List").inOrder();
  }

  private SymbolIndex getSymbolIndexAfter(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return fileIndex.getSymbolIndexAt(pos + subString.length());
  }

  private SymbolIndex getSymbolIndexBefore(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return fileIndex.getSymbolIndexAt(pos);
  }

  private static Symbol lookupSymbol(SymbolIndex index, String qualifiedName) {
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
