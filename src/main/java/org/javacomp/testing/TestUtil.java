package org.javacomp.testing;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
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
import org.javacomp.model.Module;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;

/** Utility methods for writing tests. */
public class TestUtil {
  public static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  public static final Path DUMMY_PATH = Paths.get("/dummy/path");

  private static final Context javacContext = new Context();
  private static final JavacFileManager fileManager =
      new JavacFileManager(javacContext, true /* register */, UTF_8);

  private TestUtil() {}

  /** Create a module containing parsed files. */
  public static Module parseFiles(String dirName, String... javaFiles) {
    return parseFiles(dirName, ImmutableList.copyOf(javaFiles));
  }

  public static Module parseFiles(String dirName, List<String> javaFiles) {
    Module module = new Module();
    for (String filename : javaFiles) {
      Path inputFilePath = Paths.get(dirName, filename);
      module.addOrReplaceFileScope(parseFile(inputFilePath));
    }
    return module;
  }

  /** The path of the file added for the content is {@link #DUMMY_PATH}. */
  public static Module parseContent(String content) {
    Module module = new Module();
    module.addOrReplaceFileScope(parseFileContent(content, DUMMY_PATH));
    return module;
  }

  public static String readFileContent(Path filePath) {
    try {
      return new String(Files.readAllBytes(filePath), UTF_8);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static FileScope parseFile(Path filePath) {
    String fileContent = readFileContent(filePath);
    return parseFileContent(fileContent, filePath);
  }

  public static FileScope parseFileContent(String content, Path filePath) {
    Log javacLog = Log.instance(javacContext);
    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject(filePath.toString());
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                content, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
    JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
    return new AstScanner(IndexOptions.FULL_INDEX_BUILDER.build())
        .startScan(compilationUnit, filePath.toString(), content);
  }

  /** Lookup an entity from module with qualified name. */
  public static Entity lookupEntity(String qualifiedName, Module module) {
    String[] qualifiers = qualifiedName.split("\\.");
    EntityScope currentScope = module.getRootPackage();
    Entity entity = null;
    List<String> currentQualifiers = new ArrayList<>();
    for (String qualifier : qualifiers) {
      currentQualifiers.add(qualifier);
      Collection<Entity> entities = currentScope.getMemberEntities().get(qualifier);
      assertThat(entities)
          .named("Entities for '%s'", QUALIFIER_JOINER.join(currentQualifiers))
          .isNotEmpty();
      entity = Iterables.getFirst(entities, null);
      currentScope = entity.getScope();
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
