package org.javacomp.reference;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.truth.Truth8;
import java.nio.file.Paths;
import java.util.Optional;
import org.javacomp.file.TextPosition;
import org.javacomp.model.FileScope;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReferenceSolverTest extends BaseTest {

  private final ReferenceSolver referenceSolver = new ReferenceSolver(fileManager);

  @Test
  public void testLocalVariableWithNameInStringAnOtherVariables() {
    // Must not contain the string "local" and the variable localRedefined.
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "String local;",
        "local",
        ref(TEST_REFERENCE_CLASS_FILE, "local = \"local\""));
  }

  @Test
  public void testLocalVariableRedefined() {
    // Must not contain localVariable in the if and for blocks.
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "int localRedefined;",
        "localRedefined",
        ref(TEST_REFERENCE_CLASS_FILE, "localRedefined = 3"),
        ref(TEST_REFERENCE_CLASS_FILE, "ocalRedefined = localRedefined;"),
        ref(TEST_REFERENCE_CLASS_FILE, "scoped += localRedefined;"));
  }

  @Test
  public void testVariableInBlockScope() {
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "int scoped = 4;",
        "scoped",
        ref(TEST_REFERENCE_CLASS_FILE, "scoped += localRedefined"));
  }

  @Test
  public void testVariableRedefinedInBlockScope() {
    // In the if scope
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "int scopeRedefined;",
        "scopeRedefined",
        ref(TEST_REFERENCE_CLASS_FILE, "scopeRedefined = 2;"),
        ref(TEST_REFERENCE_CLASS_FILE, "localRedefined = \"\" + scopeRedefined;"));
  }

  @Test
  public void testFooLoopInitializer() {
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "for (int scopeRedefined = 0;",
        "scopeRedefined",
        ref(TEST_REFERENCE_CLASS_FILE, "copeRedefined = 0; scopeRedefined < 10;"),
        ref(TEST_REFERENCE_CLASS_FILE, "copeRedefined < 10; scopeRedefined++"),
        ref(TEST_REFERENCE_CLASS_FILE, "long forLoopLocal = scopeRedefined"));

    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "for (int scopeRedefined :",
        "scopeRedefined",
        ref(TEST_REFERENCE_CLASS_FILE, "long localRedefined = scopeRedefined"));
  }

  @Test
  public void testMethodParameter() {
    String methodNameLine = "int withMethodParameter(int param1, String methodParameter)";
    String returnLine = "return param1 + methodParameter.length();";
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        returnLine,
        "param1",
        ref(TEST_REFERENCE_CLASS_FILE, returnLine));
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        methodNameLine,
        "param1",
        ref(TEST_REFERENCE_CLASS_FILE, returnLine));

    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        methodNameLine,
        "methodParameter",
        ref(TEST_REFERENCE_CLASS_FILE, returnLine));
  }

  @Ignore("Not implemented yet")
  @Test
  public void testPublicMethod() {
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "public void publicMethod()",
        "publicMethod",
        ref(TEST_REFERENCE_CLASS_FILE, "publicMethod();"),
        ref(TEST_REFERENCE_CLASS_FILE2, "new TestReferenceClass().publicMethod()"));
  }

  @Test
  public void testPrivateMethod() {
    assertReference(
        TEST_REFERENCE_CLASS_FILE,
        "private void privateMethod()",
        "privateMethod",
        ref(TEST_REFERENCE_CLASS_FILE, "privateMethod();"));
  }

  private static class ReferenceSpec {
    private final String filename;
    private final String symbolContext;

    private ReferenceSpec(String filename, String symbolContext) {
      this.filename = filename;
      this.symbolContext = symbolContext;
    }
  }

  private static ReferenceSpec ref(String filename, String symbolContext) {
    return new ReferenceSpec(filename, symbolContext);
  }

  private void assertReference(
      String filename, String symbolContext, String symbol, ReferenceSpec... expected) {
    ImmutableMultimap.Builder<FileScope, Range<Integer>> builder =
        new ImmutableMultimap.Builder<>();
    for (ReferenceSpec spec : expected) {
      builder.put(
          module.getFileScope(spec.filename).get(),
          new SymbolLocator(spec.filename, spec.symbolContext, symbol).locateSymbolRange());
    }
    SymbolLocator symbolLocator = new SymbolLocator(filename, symbolContext, symbol);
    TextPosition pos = locateSymbol(symbolLocator);
    Multimap<FileScope, Range<Integer>> expectedLocations = builder.build();
    Multimap<FileScope, Range<Integer>> actualLocations =
        referenceSolver.findReferences(
            module, Paths.get(filename), pos.getLine(), pos.getCharacter());
    assertThat(actualLocations)
        .named(
            String.format(
                "Expected:\n%sActual:\n%s",
                buildLocationString(expectedLocations), buildLocationString(actualLocations)))
        .isEqualTo(expectedLocations);
  }

  private String buildLocationString(Multimap<FileScope, Range<Integer>> locations) {
    StringBuilder sb = new StringBuilder();
    for (FileScope fileScope : locations.keySet()) {
      Optional<CharSequence> content =
          fileManager.getFileContent(Paths.get(fileScope.getFilename()));
      Truth8.assertThat(content).named("Content for %s", fileScope.getFilename()).isPresent();
      String actualContent = content.get().toString();
      sb.append("  ");
      sb.append(fileScope.getFilename());
      sb.append(":\n");
      for (Range<Integer> range : locations.get(fileScope)) {
        int start = range.lowerEndpoint();
        int end = range.upperEndpoint();
        int lastNewLine = actualContent.lastIndexOf("\n", Math.max(start - 1, 0));
        if (lastNewLine < 0) {
          lastNewLine = 0;
        }
        int nextNewLine = actualContent.indexOf("\n", end);
        if (nextNewLine < 0) {
          nextNewLine = actualContent.length();
        }

        assertThat(start).isGreaterThan(lastNewLine);
        sb.append("    ");
        sb.append(range);
        sb.append(actualContent.substring(lastNewLine + 1, start));
        sb.append("「");
        sb.append(actualContent.substring(start, end));
        sb.append("」");
        sb.append(actualContent.substring(end, nextNewLine));
        sb.append("\n");
      }
    }
    return sb.toString();
  }
}
