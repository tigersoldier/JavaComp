package org.javacomp.parser;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.javacomp.model.SymbolIndexScope;
import org.javacomp.proto.SymbolProto.Symbol;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AstScannerTest {
  private static final String TEST_DATA_PATH =
      "src/test/java/org/javacomp/parser/testdata/TestData.java";

  private final AstScanner scanner = new AstScanner();
  private final SymbolIndexScope globalScope = SymbolIndexScope.newGlobalScope();

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
    scanner.scan(compilationUnit, globalScope);
  }

  @Test
  public void packagesAreIndexed() {
    SymbolIndexScope packageScope = lookupQualifiedName(globalScope, "test.data");
    assertThat(packageScope.getSymbol().getType()).isEqualTo(Symbol.Type.QUALIFIER);
  }

  @Test
  public void classIsIndexedInPackage() {
    SymbolIndexScope classScope = lookupQualifiedName(globalScope, "test.data.TestData");
    assertThat(classScope.getSymbol().getType()).isEqualTo(Symbol.Type.CLASS);
  }

  @Test
  public void classIsIndexedInGlobalScope() {
    SymbolIndexScope classScope = lookupQualifiedName(globalScope, "TestData");
    assertThat(classScope.getSymbol().getType()).isEqualTo(Symbol.Type.CLASS);
  }

  private static SymbolIndexScope lookupQualifiedName(
      SymbolIndexScope scope, String qualifiedName) {
    String[] qualifiers = qualifiedName.split("\\.");
    SymbolIndexScope currentScope = scope;
    for (String qualifier : qualifiers) {
      List<SymbolIndexScope> scopes = currentScope.getNamedScopes(qualifier);
      if (scopes.isEmpty()) {
        throw new RuntimeException(
            "scope "
                + qualifier
                + " not found in "
                + Joiner.on(".").join(currentScope.getQualifiers()));
      }
      currentScope = scopes.get(0);
    }
    return currentScope;
  }
}
