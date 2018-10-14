package org.javacomp.reference;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.sun.source.tree.LineMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiConsumer;
import org.javacomp.file.FileManager;
import org.javacomp.file.TextPosition;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.VariableEntity;
import org.javacomp.project.SimpleModuleManager;
import org.javacomp.testing.TestUtil;
import org.junit.Before;

public class BaseTest {
  protected static final String TEST_DATA_DIR = "src/test/java/org/javacomp/reference/testdata/";
  protected static final String TEST_REFERENCE_CLASS_FILE = "TestReferenceClass.java";
  protected static final String TEST_REFERENCE_CLASS_FILE2 = "TestReferenceClass2.java";
  protected static final String TEST_CLASS_FILE = "TestClass.java";
  protected static final String OTHER_CLASS_FILE = "OtherClass.java";
  protected static final String OTHER_PACKAGE_CLASS_FILE = "other/OtherPackageClass.java";
  protected static final List<String> ALL_FILES =
      ImmutableList.of(
          TEST_CLASS_FILE,
          OTHER_CLASS_FILE,
          OTHER_PACKAGE_CLASS_FILE,
          TEST_REFERENCE_CLASS_FILE,
          TEST_REFERENCE_CLASS_FILE2);

  protected static final String TEST_CLASS_FULL_NAME = "org.javacomp.reference.testdata.TestClass";
  protected static final String OTHER_CLASS_FULL_NAME =
      "org.javacomp.reference.testdata.OtherClass";
  protected static final String OTHER_PACKAGE_CLASS_FULL_NAME =
      "org.javacomp.reference.testdata.other.OtherPackageClass";

  protected final SimpleModuleManager moduleManager = new SimpleModuleManager();
  protected final FileManager fileManager = moduleManager.getFileManager();

  protected Module module;
  protected VariableEntity innerAParam;
  protected VariableEntity otherClassParam;
  protected VariableEntity innerAVar;

  @Before
  public final void parseJavaFiles() throws Exception {
    for (String filename : ALL_FILES) {
      String content = getFileContent(filename);
      Path path = Paths.get(filename);
      fileManager.openFileForSnapshot(path.toUri(), content);
      moduleManager.addOrUpdateFile(Paths.get(filename), /* fixContentForParsing= */ false);
    }
    module = moduleManager.getModule();
    MethodEntity testMethod =
        (MethodEntity) TestUtil.lookupEntity(TEST_CLASS_FULL_NAME + ".testMethod", module);
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

    /**
     * @param filename the name of the file to find {@code symbol} in.
     * @param symbolContext the surrounding text of the symbol. The first character must not be
     *     after the {@code symbol} to be located.
     * @param symbol the content of the symbol to be located.
     */
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
      int pos = locateSymbolRange().lowerEndpoint();
      FileScope fileScope = module.getFileScope(filename).get();
      LineMap lineMap = fileScope.getLineMap().get();
      // LineMap line and column are 1-indexed, while our API is 0-indexed.
      return TextPosition.create(
          (int) lineMap.getLineNumber(pos) - 1, (int) lineMap.getColumnNumber(pos) - 1);
    }

    public Range<Integer> locateSymbolRange() {
      String fileContent = getFileContent(filename);
      int contextStart = fileContent.indexOf(symbolContext);
      assertThat(contextStart).named("location of " + symbolContext).isGreaterThan(-1);
      int start = fileContent.indexOf(symbol, contextStart);
      assertThat(start).named("start").isGreaterThan(-1);

      return Range.closed(start, start + symbol.length());
    }
  }
}
