package org.javacomp.testing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;

/** Utility methods for writing tests. */
public class TestUtil {
  public static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private static final Context javacContext = new Context();
  private static final JavacFileManager fileManager =
      new JavacFileManager(javacContext, true /* register */, UTF_8);

  private TestUtil() {}

  /** Create a global scope containing parsed files. */
  public static GlobalScope parseFiles(String dirName, String... javaFiles) {
    GlobalScope globalScope = new GlobalScope();
    for (String filename : javaFiles) {
      Path inputFilePath = Paths.get(dirName, filename);
      globalScope.addOrReplaceFileScope(parseFile(inputFilePath));
    }
    return globalScope;
  }

  public static FileScope parseFile(Path filePath) {
    Log javacLog = Log.instance(javacContext);
    String fileContent;
    try {
      fileContent = new String(Files.readAllBytes(filePath), UTF_8);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject(filePath.toString());
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                fileContent,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
    return new AstScanner().startScan(compilationUnit, filePath.toString());
  }

  /** Lookup an entity from global scope with qualified name. */
  public static Entity lookupEntity(String qualifiedName, GlobalScope globalScope) {
    String[] qualifiers = qualifiedName.split("\\.");
    EntityScope currentScope = globalScope;
    Entity entity = null;
    List<String> currentQualifiers = new ArrayList<>();
    for (String qualifier : qualifiers) {
      currentQualifiers.add(qualifier);
      Collection<Entity> entities = currentScope.getAllEntities().get(qualifier);
      assertWithMessage(QUALIFIER_JOINER.join(currentQualifiers)).that(entities).isNotEmpty();
      entity = Iterables.getFirst(entities, null);
      currentScope = entity.getChildScope();
    }
    assertThat(entity).isNotNull();
    return entity;
  }

  public static ExpressionTree parseExpression(String expression) {
    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                expression,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    return parser.parseExpression();
  }
}
