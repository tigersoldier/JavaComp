package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.FluentIterable;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.javacomp.model.FileIndex;
import org.javacomp.model.GlobalIndex;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompletorTest {
  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/completion/testdata/";
  private static final String COMPLETION_POINT_MARK = "/**@complete*/";

  private List<CompletionCandidate> completeTestFile(String filename) throws Exception {
    Context javacContext = new Context();
    String inputFilePath = TEST_DATA_DIR + filename;
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    String testDataContent = new String(Files.readAllBytes(Paths.get(inputFilePath)), UTF_8);
    int completion_point = testDataContent.indexOf(COMPLETION_POINT_MARK);
    assertThat(completion_point).isGreaterThan(-1);
    String input = testDataContent.substring(0, completion_point);

    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject("/" + inputFilePath);
    Log javacLog = Log.instance(javacContext);
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                input, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
    JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
    FileIndex inputFileIndex = new AstScanner().startScan(compilationUnit, inputFilePath);
    GlobalIndex globalIndex = new GlobalIndex();

    // throw new RuntimeException(String.format("input length: %s, ranges: %s", input.length(), inputFileIndex.getIndexRangeMap()));

    return new Completor()
        .getCompletionCandidates(
            globalIndex, inputFileIndex, compilationUnit, inputFilePath, input);
  }

  private static List<String> getCandidateNames(List<CompletionCandidate> candidates) {
    return FluentIterable.from(candidates).transform(candidate -> candidate.getName()).toList();
  }

  @Test
  public void completeNewStatement() throws Exception {
    assertThat(getCandidateNames(completeTestFile("CompleteNewStatement.java")))
        .containsExactly(
            "CompleteNewStatement", "CONSTANT", "memberField", "memberMethod", "staticMethod");
  }
}
