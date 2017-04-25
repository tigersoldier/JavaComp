package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.FluentIterable;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;
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

  private List<CompletionCandidate> completeTestFile(String filename) throws Exception {
    Context javacContext = new Context();
    String inputFilePath = TEST_DATA_DIR + filename;
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    String testDataContent = new String(Files.readAllBytes(Paths.get(inputFilePath)), UTF_8);
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
    assertThat(getCandidateNames(completeTestFile("CompleteNewStatement.java")))
        .containsExactly(
            "CompleteNewStatement",
            "CompleteNewStatement",
            "CONSTANT",
            "memberField",
            "memberMethod",
            "staticMethod",
            "org");
  }

  @Test
  public void completeMemberSelection() throws Exception {
    assertThat(getCandidateNames(completeTestFile("CompleteMember.java")))
        .containsExactly("field", "method");
  }
}
