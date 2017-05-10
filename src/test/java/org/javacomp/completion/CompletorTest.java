package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
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

  private List<CompletionCandidate> completeContent(String inputFilePath, String testDataContent) {
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
    FileScope inputFileScope = new AstScanner().startScan(compilationUnit, inputFilePath);
    inputFileScope.setAdjustedLineMap(fixedContent.getAdjustedLineMap());
    GlobalScope globalScope = new GlobalScope();
    globalScope.addOrReplaceFileScope(inputFileScope);

    return new Completor()
        .getCompletionCandidates(globalScope, Paths.get(inputFilePath), line, column);
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
            "org");
    assertThat(getCandidateNames(completeTestFile("CompleteNewStatement.java")))
        .containsExactlyElementsIn(Iterables.concat(expectedMembers, keywords));
  }

  @Test
  public void completeMemberSelection() throws Exception {
    String baseAboveCompletion = "above./** @complete */";
    List<String> aboveCases =
        ImmutableList.of(baseAboveCompletion, baseAboveCompletion + "\nabove.aboveMethod();");
    for (String aboveCase : aboveCases) {
      List<CompletionCandidate> candidates =
          completeWithContent("CompleteInMethod.java", aboveCase);
      assertThat(getCandidateNames(candidates)).containsExactly("aboveField", "aboveMethod");
    }

    String baseBelowCompletion = "below./** @complete */";
    List<String> belowCases =
        ImmutableList.of(
            baseBelowCompletion,
            baseBelowCompletion + "\nbelow.belowMethod();",
            "above.;" + baseBelowCompletion,
            "self.new BelowClass()./** @complete */");
    for (String belowCase : belowCases) {
      List<CompletionCandidate> candidates =
          completeWithContent("CompleteInMethod.java", belowCase);
      assertThat(getCandidateNames(candidates))
          .named(belowCase)
          .containsExactly("belowField", "belowMethod");
    }
  }

  @Test
  public void completeImport() throws Exception {
    String baseImportCompletion = "import org.javacomp./** @complete */";
    List<String> cases =
        ImmutableList.of(
            baseImportCompletion,
            baseImportCompletion + "\nimport java.util.List;",
            "import java.util.List;\n" + baseImportCompletion);
    for (String importCase : cases) {
      List<CompletionCandidate> candidates =
          completeWithContent("CompleteOutOfClass.java", importCase);
      assertThat(getCandidateNames(candidates)).containsExactly("completion");
    }
  }

  @Test
  public void completeInBlock() throws Exception {
    String content =
        "if (true) {\n"
            + "  AboveClass innerAboveClass;\n"
            + "  innerAboveClass./** @complete */\n"
            + "}";
    List<CompletionCandidate> candidates = completeWithContent("CompleteInMethod.java", content);
    assertThat(getCandidateNames(candidates)).containsExactly("aboveField", "aboveMethod");
  }

  private List<CompletionCandidate> completeWithContent(String filename, String toInsert) {
    String testDataContent = getFileContent(filename);
    String newContent = testDataContent.replace(INSERTION_POINT_MARK, toInsert);
    return completeContent(filename, newContent);
  }
}
