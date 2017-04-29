package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;
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
    Context javacContext = new Context();
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    int completionPoint = testDataContent.indexOf(COMPLETION_POINT_MARK);
    assertThat(completionPoint).isGreaterThan(-1);

    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject("/" + inputFilePath);
    Log javacLog = Log.instance(javacContext);
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                testDataContent,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
    LineMap lineMap = compilationUnit.getLineMap();
    int line = (int) lineMap.getLineNumber(completionPoint);
    int column = (int) lineMap.getColumnNumber(completionPoint);
    FileScope inputFileScope = new AstScanner().startScan(compilationUnit, inputFilePath);
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

  private List<CompletionCandidate> completeWithContent(String filename, String toInsert) {
    String testDataContent = getFileContent(filename);
    String newContent = testDataContent.replace(INSERTION_POINT_MARK, toInsert);
    return completeContent(filename, newContent);
  }
}
