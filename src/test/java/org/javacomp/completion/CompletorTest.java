package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.javacomp.file.SimpleFileManager;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.ParserContext;
import org.javacomp.project.PositionContext;
import org.javacomp.project.SimpleModuleManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompletorTest {
  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/completion/testdata/";
  private static final String COMPLETION_POINT_MARK = "/** @complete */";
  private static final String INSERTION_POINT_MARK = "/** @insert */";

  private final SimpleModuleManager moduleManager = new SimpleModuleManager();
  private final SimpleFileManager fileManager = new SimpleFileManager();

  private Path getInputFilePath(String filename) {
    return Paths.get(TEST_DATA_DIR + filename).toAbsolutePath();
  }

  private String getFileContent(String filename) {
    return fileManager.getFileContent(getInputFilePath(filename)).get().toString();
  }

  private List<CompletionCandidate> completeTestFile(String filename) {
    String testDataContent = getFileContent(filename);
    return completeContent(filename, testDataContent);
  }

  private CompletionParams createCompletionParams(
      String inputFilename, String testDataContent, String... otherFiles) {

    Path inputFilePath = getInputFilePath(inputFilename);
    ParserContext parserContext = new ParserContext();
    // FileContentFixer fileContentFixer = new FileContentFixer(parserContext);
    // parserContext.setupLoggingSource(inputFilename);

    assertThat(testDataContent).contains(COMPLETION_POINT_MARK);
    int completionPoint = testDataContent.indexOf(COMPLETION_POINT_MARK);
    testDataContent = testDataContent.replace(COMPLETION_POINT_MARK, "");

    LineMap lineMap = parserContext.tokenize(testDataContent, false).getLineMap();
    // Completion line and column numbers are 0-based, while LineMap values are 1-based.
    int line = (int) lineMap.getLineNumber(completionPoint) - 1;
    int column = (int) lineMap.getColumnNumber(completionPoint) - 1;

    moduleManager.getFileManager().openFileForSnapshot(inputFilePath.toUri(), testDataContent);
    moduleManager.addOrUpdateFile(inputFilePath, /* fixContentForParsing= */ true);

    Module otherModule = new Module();
    moduleManager.addDependingModule(otherModule);

    otherModule.addDependingModule(moduleManager.getModule());

    List<String> otherFilesWithObject =
        new ImmutableList.Builder<String>().add(otherFiles).add("Object.java").build();
    for (String otherFile : otherFilesWithObject) {
      String content = getFileContent(otherFile);
      JCCompilationUnit otherCompilationUnit = parserContext.parse(otherFile, content);
      FileScope fileScope =
          new AstScanner(IndexOptions.FULL_INDEX_BUILDER.build())
              .startScan(otherCompilationUnit, otherFile, content);
      otherModule.addOrReplaceFileScope(fileScope);
    }

    return new CompletionParams(line, column);
  }

  private List<CompletionCandidate> completeContent(
      String inputFilename, String testDataContent, String... otherFiles) {
    CompletionParams params = createCompletionParams(inputFilename, testDataContent, otherFiles);
    return new Completor(moduleManager.getFileManager())
        .getCompletionCandidates(
            moduleManager, getInputFilePath(inputFilename), params.line, params.column)
        .candidates();
  }

  private static List<String> getCandidateNames(List<CompletionCandidate> candidates) {
    return FluentIterable.from(candidates).transform(candidate -> candidate.getName()).toList();
  }

  @Test
  public void completeSymbolsInMethod() throws Exception {
    String toComplete = "/** @complete */";
    List<CompletionCandidate> candidates = completeWithContent("CompleteInMethod.java", toComplete);
    assertThat(getCandidateNames(candidates))
        .named("Candidates of '" + toComplete + "'")
        .containsAllOf(
            "STATIC_FIELD",
            "staticMethod",
            "self",
            "fakeString",
            "AboveClass",
            "completeMethod",
            "BelowClass");
  }

  @Test
  public void completeNewStatement() throws Exception {
    List<String> keywords =
        Arrays.stream(KeywordCompletionCandidate.values())
            .map(e -> e.getName())
            .collect(ImmutableList.toImmutableList());
    List<String> expectedMembers =
        ImmutableList.of(
            "CompleteNewStatement",
            "param1",
            "stringParam",
            "CONSTANT",
            "InnerClass",
            "List",
            "subClassMemberField",
            "memberField",
            "memberMethod",
            "staticMethod",
            "staticMethod", // TODO: Fix duplicate.
            "org",
            // From java.lang
            "java",
            "Object",
            // From Object
            "toString",
            "toString");
    assertThat(getCandidateNames(completeTestFile("CompleteNewStatement.java")))
        .containsExactlyElementsIn(Iterables.concat(expectedMembers, keywords));
  }

  @Test
  public void completeMemberSelection() throws Exception {
    String baseAboveCompletion = "above./** @complete */";
    List<String> aboveCases =
        ImmutableList.of(baseAboveCompletion, baseAboveCompletion + "\nabove.aboveMethod();");
    assertCompletion("CompleteInMethod.java", aboveCases, "aboveField", "aboveMethod", "toString");

    String baseBelowCompletion = "below./** @complete */";
    List<String> belowCases =
        ImmutableList.of(
            baseBelowCompletion,
            baseBelowCompletion + "\nbelow.belowMethod();",
            "above.;" + baseBelowCompletion,
            "self.new BelowClass()./** @complete */");
    assertCompletion("CompleteInMethod.java", belowCases, "belowField", "belowMethod", "toString");
  }

  @Test
  public void completeStaticMemberSelection() throws Exception {
    assertCompletion(
        "CompleteInMethod.java",
        ImmutableList.of("BelowClass./** @complete */"),
        "STATIC_BELOW_FIELD",
        "staticBelowMethod");
    assertCompletion(
        "CompleteInMethod.java",
        ImmutableList.of("CompleteInMethod./** @complete */"),
        "AboveClass",
        "BelowClass",
        "STATIC_FIELD",
        "staticMethod");
  }

  @Test
  public void completeTwoDots() throws Exception {
    String toComplete = "above./** @complete */.something";

    List<CompletionCandidate> candidates = completeWithContent("CompleteInMethod.java", toComplete);
    assertThat(getCandidateNames(candidates))
        .named("Candidates of '" + toComplete + "'")
        .containsAllOf("aboveField", "aboveMethod", "toString");
  }

  @Test
  public void completeMemberSelectionInOtherFile() throws Exception {
    List<CompletionCandidate> candidates =
        completeWithContent(
            "CompleteInMethod.java",
            "new OtherClass().innerClass./** @complete */",
            "OtherClass.java");
    assertThat(getCandidateNames(candidates))
        .containsExactly("innerInnerClass", "getInnerInnerClass", "toString");
  }

  @Test
  public void completeImport() throws Exception {
    String baseImportCompletion = "import org.javacomp./** @complete */";
    List<String> cases =
        ImmutableList.of(
            baseImportCompletion,
            baseImportCompletion + "\nimport java.util.List;",
            "import java.util.List;\n" + baseImportCompletion);
    assertCompletion("CompleteOutOfClass.java", cases, "completion");
  }

  @Test
  public void completeInBlock() throws Exception {
    String content =
        "if (true) {\n"
            + "  AboveClass innerAboveClass;\n"
            + "  innerAboveClass./** @complete */\n"
            + "}";
    assertCompletion("CompleteInMethod.java", content, "aboveField", "aboveMethod", "toString");
  }

  @Test
  public void completeJavaLangClasses() throws Exception {
    assertThat(
            getCandidateNames(
                completeWithContent(
                    "CompleteInMethod.java", "/** @complete */", "FakeString.java")))
        .contains("FakeString");
    assertThat(
            getCandidateNames(
                completeWithContent(
                    "CompleteInMethod.java", "fakeString./** @complete */", "FakeString.java")))
        .containsExactly("fakeField", "fakeMethod", "toString");
  }

  @Test
  public void completeIncompleteIfBlock() throws Exception {
    assertThat(
            getCandidateNames(
                completeWithContent(
                    "CompleteInMethod.java", "{if (above.a/** @complete */)}", "FakeString.java")))
        .contains("aboveField");
  }

  @Test
  public void completeWithTypeParameter() throws Exception {
    List<String> testcases =
        ImmutableList.of(
            "boundParameterized.getTypeParameterT()./** @complete */",
            "parameterizedOfNonParameterized.getTypeParameterT()./** @complete */");
    for (String testcase : testcases) {
      assertThat(getCandidateNames(completeWithContent("Parameterized.java", testcase)))
          .named(testcase)
          .contains("nonParameterizedField");
    }
  }

  @Test
  public void completeWithTypeCast() throws Exception {
    assertThat(
            getCandidateNames(
                completeWithContent(
                    "CompleteInMethod.java", "((BelowClass) above)./** @complete */")))
        .contains("belowField");
  }

  @Test
  public void completeImportedClassAndMembers() {
    assertThat(getCandidateNames(completeTestFile("CompleteImported.java")))
        .containsAllOf(
            "staticField",
            "staticMethid",
            "ExplicitInnerClass",
            "onDemandStaticField",
            "onDemandStaticMethod",
            "OnDemandInnerClass");
  }

  private void assertCompletion(String filename, String toComplete, String... expectedCandidates) {
    assertCompletion(filename, ImmutableList.of(toComplete), expectedCandidates);
  }

  private void assertCompletion(
      String filename, List<String> toCompleteCases, String... expectedCandidates) {
    for (String toComplete : toCompleteCases) {
      List<CompletionCandidate> candidates = completeWithContent(filename, toComplete);
      assertThat(extractCompletionPrefixWithContent(filename, toComplete))
          .named("Prefix of " + toComplete)
          .isEqualTo("");
      assertThat(getCandidateNames(candidates))
          .named("Candidates of '" + toComplete + "'")
          .containsExactly((Object[]) expectedCandidates);
    }

    Multimap<Character, String> candidatePrefixMap = HashMultimap.create();
    for (String candidate : expectedCandidates) {
      char prefix = candidate.charAt(0);
      char lowerPrefix = Character.toLowerCase(prefix);
      char upperPrefix = Character.toUpperCase(prefix);
      candidatePrefixMap.put(lowerPrefix, candidate);
      candidatePrefixMap.put(upperPrefix, candidate);
    }

    for (String toComplete : toCompleteCases) {
      int dotPos = toComplete.indexOf("." + COMPLETION_POINT_MARK);
      if (dotPos == -1) {
        continue;
      }

      for (char prefix : candidatePrefixMap.keySet()) {
        String toCompleteWithMember =
            toComplete.substring(0, dotPos + 1) + prefix + toComplete.substring(dotPos + 1);
        List<CompletionCandidate> candidates = completeWithContent(filename, toCompleteWithMember);
        assertThat(extractCompletionPrefixWithContent(filename, toCompleteWithMember))
            .named("prefix of " + toCompleteWithMember)
            .isEqualTo("" + prefix);
        assertThat(getCandidateNames(candidates))
            .named("candidates of '" + toCompleteWithMember + "'")
            .containsExactlyElementsIn(candidatePrefixMap.get(prefix));
      }
    }
  }

  private List<CompletionCandidate> completeWithContent(
      String filename, String toInsert, String... otherFiles) {
    String testDataContent = getFileContent(filename);
    String newContent = testDataContent.replace(INSERTION_POINT_MARK, toInsert);
    return completeContent(filename, newContent, otherFiles);
  }

  private String extractCompletionPrefixWithContent(String filename, String toInsert) {
    String testDataContent = getFileContent(filename);
    String newContent = testDataContent.replace(INSERTION_POINT_MARK, toInsert);
    assertThat(newContent).contains(COMPLETION_POINT_MARK);
    CompletionParams params = createCompletionParams(filename, newContent);
    Path filePath = getInputFilePath(filename);
    PositionContext positionContext =
        PositionContext.createForPosition(moduleManager, filePath, params.line, params.column)
            .get();
    return new Completor(moduleManager.getFileManager())
        .extractCompletionPrefix(
            positionContext.getFileScope(), filePath, params.line, params.column);
  }

  private static class CompletionParams {
    private final int line;
    private final int column;

    private CompletionParams(int line, int column) {
      this.line = line;
      this.column = column;
    }
  }
}
