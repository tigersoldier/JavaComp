package org.javacomp.parser;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Optional;
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

  @Before
  public void setUpCompilationUnit() throws Exception {
    Context javacContext = new Context();
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    String input = new String(Files.readAllBytes(Paths.get(TEST_DATA_PATH)), UTF_8);
    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                input, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
    compilationUnit = parser.parseCompilationUnit();
    scanner.startScan(compilationUnit, globalIndex);
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
