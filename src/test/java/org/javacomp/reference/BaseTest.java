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
import java.util.function.BiConsumer;
import org.javacomp.file.TextPosition;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.VariableEntity;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.ParserContext;
import org.javacomp.testing.TestUtil;
import org.junit.Before;

public class BaseTest {
  protected static final String TEST_DATA_DIR = "src/test/java/org/javacomp/reference/testdata/";
  protected static final String TEST_CLASS_FILE = "TestClass.java";
  protected static final String OTHER_CLASS_FILE = "OtherClass.java";
  protected static final String OTHER_PACKAGE_CLASS_FILE = "other/OtherPackageClass.java";
  protected static final List<String> ALL_FILES =
      ImmutableList.of(TEST_CLASS_FILE, OTHER_CLASS_FILE, OTHER_PACKAGE_CLASS_FILE);

  protected static final String TEST_CLASS_FULL_NAME = "org.javacomp.reference.testdata.TestClass";
  protected static final String OTHER_CLASS_FULL_NAME =
      "org.javacomp.reference.testdata.OtherClass";
  protected static final String OTHER_PACKAGE_CLASS_FULL_NAME =
      "org.javacomp.reference.testdata.other.OtherPackageClass";

  protected GlobalScope globalScope;

  protected VariableEntity innerAParam;
  protected VariableEntity otherClassParam;
  protected VariableEntity innerAVar;

  @Before
  public final void parseJavaFiles() {
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

  protected String getFileContent(String filename) {
    Path inputFilePath = Paths.get(TEST_DATA_DIR, filename);
    try {
      return new String(Files.readAllBytes(inputFilePath), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected TextPosition locateSymbol(SymbolLocator symbolLocator) {
    return symbolLocator.locateSymbol();
  }

  protected class SymbolLocator {
    protected final String filename;
    protected final String symbolContext;
    protected final String symbol;

    protected SymbolLocator(String filename, String symbolContext, String symbol) {
      this.filename = filename;
      this.symbolContext = symbolContext;
      this.symbol = symbol;
    }

    @Override
    public String toString() {
      return String.format(
          "{filename: %s, symbolContext: %s, symbol: %s", filename, symbolContext, symbol);
    }

    public String toDebugString(int positionInSymbol) {
      String symbolWithCursor =
          symbol.substring(0, positionInSymbol) + "|" + symbol.substring(positionInSymbol);
      return String.format(
          "{filename: %s, symbolContext: %s, symbol: <%s>",
          filename, symbolContext, symbolWithCursor);
    }

    public void forEachPosition(BiConsumer<TextPosition, String> consumer) {
      TextPosition initialPosition = locateSymbol();
      for (int i = 0; i < symbol.length(); i++) {
        TextPosition textPosition =
            TextPosition.create(initialPosition.getLine(), initialPosition.getCharacter() + i);
        String debugString = toDebugString(i);
        consumer.accept(textPosition, debugString);
      }
    }

    public TextPosition locateSymbol() {
      String fileContent = getFileContent(filename);
      int start = fileContent.indexOf(symbolContext);
      assertThat(start).named("location of " + symbolContext).isGreaterThan(-1);
      int pos = fileContent.indexOf(symbol, start);
      assertThat(pos).named("pos").isGreaterThan(-1);
      FileScope fileScope = globalScope.getFileScope(filename).get();
      LineMap lineMap = fileScope.getLineMap();
      // LineMap line and column are 1-indexed, while our API is 0-indexed.
      return TextPosition.create(
          (int) lineMap.getLineNumber(pos) - 1, (int) lineMap.getColumnNumber(pos) - 1);
    }
  }
}
