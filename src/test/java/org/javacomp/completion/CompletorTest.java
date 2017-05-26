package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.FileContentFixer;
import org.javacomp.parser.FileContentFixer.FixedContent;
import org.javacomp.parser.ParserContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompletorTest {
  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/completion/testdata/";
  private static final String COMPLETION_POINT_MARK = "/** @complete */";
  private static final String INSERTION_POINT_MARK = "/** @insert */";

  private String getFileContent(String filename) {
    String inputFilePath = TEST_DATA_DIR + filename;
    try {
      return new String(Files.readAllBytes(Paths.get(inputFilePath)), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<CompletionCandidate> completeTestFile(String filename) {
    String testDataContent = getFileContent(filename);
    return completeContent(filename, testDataContent);
  }

  private List<CompletionCandidate> completeContent(
      String inputFilePath, String testDataContent, String... otherFiles) {
    ParserContext parserContext = new ParserContext();
    FileContentFixer fileContentFixer = new FileContentFixer(parserContext);

    int completionPoint = testDataContent.indexOf(COMPLETION_POINT_MARK);
    assertThat(completionPoint).isGreaterThan(-1);

    LineMap lineMap = parserContext.tokenize(testDataContent, false).getLineMap();
    // Completion line and column numbers are 0-based, while LineMap values are 1-based.
    int line = (int) lineMap.getLineNumber(completionPoint) - 1;
    int column = (int) lineMap.getColumnNumber(completionPoint) - 1;

    FixedContent fixedContent = fileContentFixer.fixFileContent(testDataContent);

    JCCompilationUnit compilationUnit =
        parserContext.parse(inputFilePath, fixedContent.getContent());
    FileScope inputFileScope =
        new AstScanner(IndexOptions.FULL_INDEX_BUILDER.build())
            .startScan(compilationUnit, inputFilePath, fixedContent.getContent());
    inputFileScope.setAdjustedLineMap(fixedContent.getAdjustedLineMap());
    Module module = new Module();
    module.addOrReplaceFileScope(inputFileScope);

    Module otherModule = new Module();
    module.addDependingModule(otherModule);

    otherModule.addDependingModule(module);

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

    return new Completor().getCompletionCandidates(module, Paths.get(inputFilePath), line, column);
  }

  private static List<String> getCandidateNames(List<CompletionCandidate> candidates) {
    return FluentIterable.from(candidates).transform(candidate -> candidate.getName()).toList();
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
  public void completeMemberSelectionInOtherFile() throws Exception {
    List<CompletionCandidate> candidates =
        completeWithContent(
            "CompleteInMethod.java",
            "new OtherClass().innerClass./** @complete */",
            "OtherClass.java");
    assertThat(getCandidateNames(candidates))
        .containsExactly("innerInnerClass", "getInnerInnerClass", "InnerInnerClass", "toString");
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

  private void assertCompletion(String filename, String toComplete, String... expectedCandidates) {
    assertCompletion(filename, ImmutableList.of(toComplete), expectedCandidates);
  }

  private void assertCompletion(
      String filename, List<String> toCompleteCases, String... expectedCandidates) {
    for (String toComplete : toCompleteCases) {
      List<CompletionCandidate> candidates = completeWithContent(filename, toComplete);
      assertThat(getCandidateNames(candidates))
          .named("Candidates of '" + toComplete + "'")
          .containsExactly((Object[]) expectedCandidates);
    }

    Multimap<Character, String> candidatePrefixMap = HashMultimap.create();
    for (String candidate : expectedCandidates) {
      char prefix = candidate.charAt(0);
      candidatePrefixMap.put(prefix, candidate);
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
        assertThat(getCandidateNames(candidates))
            .named("candidates of '" + toCompleteWithMember + "'")
            .containsExactly((Object[]) expectedCandidates);
        // TODO: implement query prefix
        // .containsExactlyElementsIn(candidatePrefixMap.get(prefix));
      }
    }
  }

  private List<CompletionCandidate> completeWithContent(
      String filename, String toInsert, String... otherFiles) {
    String testDataContent = getFileContent(filename);
    String newContent = testDataContent.replace(INSERTION_POINT_MARK, toInsert);
    return completeContent(filename, newContent, otherFiles);
  }
}
