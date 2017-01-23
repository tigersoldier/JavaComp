package org.javacomp.parser;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import org.javacomp.model.FileIndex;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityIndex;
import org.javacomp.model.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AstScannerTest {
  private static final String TEST_DATA_PATH =
      "src/test/java/org/javacomp/parser/testdata/TestData.java";

  private final AstScanner scanner = new AstScanner();

  private JCCompilationUnit compilationUnit;
  private String testDataContent;
  private FileIndex fileIndex;

  @Before
  public void startScan() throws Exception {
    Context javacContext = new Context();
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    testDataContent = new String(Files.readAllBytes(Paths.get(TEST_DATA_PATH)), UTF_8);

    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject("/" + TEST_DATA_PATH);
    Log javacLog = Log.instance(javacContext);
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                testDataContent,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    compilationUnit = parser.parseCompilationUnit();
    fileIndex = scanner.startScan(compilationUnit, TEST_DATA_PATH);
  }

  @Test
  public void fileIndexHasCorrectPackage() {
    assertThat(fileIndex.getPackageQualifiers()).containsExactly("test", "data").inOrder();
  }

  @Test
  public void classIsIndexedInPackage() {
    Entity classEntity = lookupEntity(fileIndex, "TestData");
    assertThat(classEntity.getKind()).isEqualTo(Entity.Kind.CLASS);
  }

  @Test
  public void classIsIndexedGlobally() {
    List<Entity> classEntity = fileIndex.getGlobalEntitiesWithName("TestData");
    assertThat(classEntity).hasSize(1);
    assertThat(classEntity.get(0).getKind()).isEqualTo(Entity.Kind.CLASS);
  }

  @Test
  public void methodIsIndexedInClassIndex() {
    Entity methodEntity = lookupEntity(fileIndex, "TestData.publicIfBlockMethod");
    assertThat(methodEntity.getKind()).isEqualTo(Entity.Kind.METHOD);
  }

  @Test
  public void classStaticFieldIsIndexedInClassIndex() {
    Entity variableEntity = lookupEntity(fileIndex, "TestData.publicStaticIntField");
    assertThat(variableEntity.getKind()).isEqualTo(Entity.Kind.VARIABLE);
  }

  @Test
  public void innerClassIsIndexedInClassIndex() {
    Entity classEntity = lookupEntity(fileIndex, "TestData.PrivateStaticInnerClass");
    assertThat(classEntity.getKind()).isEqualTo(Entity.Kind.CLASS);
    Entity annotationEntity = lookupEntity(fileIndex, "TestData.PublicInnerAnnotation");
    assertThat(annotationEntity.getKind()).isEqualTo(Entity.Kind.ANNOTATION);
    Entity enumEntity = lookupEntity(fileIndex, "TestData.PublicInnerEnum");
    assertThat(enumEntity.getKind()).isEqualTo(Entity.Kind.ENUM);
    Entity interfaceEntity = lookupEntity(fileIndex, "TestData.PublicInnerInterface");
    assertThat(interfaceEntity.getKind()).isEqualTo(Entity.Kind.INTERFACE);
  }

  @Test
  public void enumItemIsIndexedInEnumIndex() {
    Entity variableEntity = lookupEntity(fileIndex, "TestData.PublicInnerEnum.ENUM_VALUE1");
    assertThat(variableEntity.getKind()).isEqualTo(Entity.Kind.VARIABLE);
  }

  @Test
  public void topLevelClassIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("public class TestData {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // class TestData");
    EntityIndex indexAtField = getEntityIndexAfter("publicStaticIntField;");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index).isEqualTo(lookupEntity(fileIndex, "TestData").getChildIndex());
    }
  }

  @Test
  public void innerEnumIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("public enum PublicInnerEnum {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // PublicInnerEnum");
    EntityIndex indexAtField = getEntityIndexAfter("ENUM_VALUE1,");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(lookupEntity(fileIndex, "TestData.PublicInnerEnum").getChildIndex());
    }
  }

  @Test
  public void innerInterfaceIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("public interface PublicInnerInterface {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // PublicInnerInterface");
    EntityIndex indexAtField = getEntityIndexAfter("interfaceMethod();");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index)
          .isEqualTo(lookupEntity(fileIndex, "TestData.PublicInnerInterface").getChildIndex());
    }
  }

  @Test
  public void methodIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("publicIfBlockMethod() {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // publicIfBlockMethod");
    EntityIndex indexAtField = getEntityIndexAfter("methodScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      MethodEntity methodEntity =
          (MethodEntity) lookupEntity(fileIndex, "TestData.publicIfBlockMethod");
      assertThat(index).isEqualTo(methodEntity.getOverloads().get(0).getMethodIndex());
    }
  }

  @Test
  public void ifBlockIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("if (a == 1) {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} else { // end of if");
    EntityIndex indexAtField = getEntityIndexAfter("ifScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("ifScopeVar", Entity.Kind.VARIABLE)).isPresent();
      assertThat(index.getEntitiesWithName("elseScopeVar")).isEmpty();
    }
  }

  @Test
  public void elseBlockIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("else {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // else");
    EntityIndex indexAtField = getEntityIndexAfter("elseScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("elseScopeVar", Entity.Kind.VARIABLE)).isPresent();
      assertThat(index.getEntitiesWithName("ifScopeVar")).isEmpty();
    }
  }

  @Test
  public void whileBlockIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("while (number > 0) {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // while loop");
    EntityIndex indexAtField = getEntityIndexAfter("whileScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("whileScopeVar", Entity.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void forBlockIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("for (String s : input) {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // for loop");
    EntityIndex indexAtField = getEntityIndexAfter("forScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("forScopeVar", Entity.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void switchBlockIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("switch (a) {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // switch");
    EntityIndex indexAtField = getEntityIndexAfter("switchScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("switchScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
      assertThat(index.getEntitiesWithName("caseScopeVar")).isEmpty();
    }
  }

  @Test
  public void switchCaseBlockIndexRange() {
    EntityIndex indexAtStart = getEntityIndexBefore("{ // start of case block");
    EntityIndex indexAtEnd = getEntityIndexBefore("} // end of case block");
    EntityIndex indexAtField = getEntityIndexAfter("caseScopeVar");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("switchScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
      assertThat(index.getEntityWithNameAndKind("caseScopeVar", Entity.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void annonymousClassIndexRange() {
    EntityIndex indexAtStart = getEntityIndexAfter("new PublicInnerInterface() {");
    EntityIndex indexAtEnd = getEntityIndexBefore("} /* end of new PublicInnerInterface *");
    EntityIndex indexAtField = getEntityIndexAfter("privateAnnonymousClassMethod");
    for (EntityIndex index : ImmutableList.of(indexAtStart, indexAtEnd, indexAtField)) {
      assertThat(index.getEntityWithNameAndKind("privateAnnonymousClassMethod", Entity.Kind.METHOD))
          .isPresent();
      assertThat(index.getEntityWithNameAndKind("interfaceMethod", Entity.Kind.METHOD)).isPresent();
    }
  }

  @Test
  public void primitiveTypeReference() {
    MethodEntity methodEntity =
        (MethodEntity) lookupEntity(fileIndex, "TestData.protectedWhileBlockMethod");
    TypeReference intReference =
        methodEntity.getOverloads().get(0).getParameters().get(0).getType();
    assertThat(intReference.getFullName()).containsExactly("int").inOrder();
  }

  @Test
  public void nonPrimitiveTypeReference() {
    MethodEntity methodEntity =
        (MethodEntity) lookupEntity(fileIndex, "TestData.privateForBlockMethod");
    TypeReference intReference =
        methodEntity.getOverloads().get(0).getParameters().get(0).getType();
    assertThat(intReference.getFullName()).containsExactly("java", "util", "List").inOrder();
  }

  @Test
  public void explicitClassImport() {
    assertThat(fileIndex.getImportedClass("Baz")).hasValue(ImmutableList.of("foo", "Bar", "Baz"));
  }

  @Test
  public void testOnDemandClassImport() {
    assertThat(fileIndex.getOnDemandClassImportQualifiers())
        .containsExactly(ImmutableList.of("foo", "bar"), ImmutableList.of("foo", "bar", "baz"));
  }

  private EntityIndex getEntityIndexAfter(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return fileIndex.getEntityIndexAt(pos + subString.length());
  }

  private EntityIndex getEntityIndexBefore(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return fileIndex.getEntityIndexAt(pos);
  }

  private static Entity lookupEntity(EntityIndex index, String qualifiedName) {
    String[] qualifiers = qualifiedName.split("\\.");
    EntityIndex currentIndex = index;
    Entity entity = null;
    for (String qualifier : qualifiers) {
      Collection<Entity> entities = currentIndex.getAllEntities().get(qualifier);
      assertThat(entities).isNotEmpty();
      entity = Iterables.getFirst(entities, null);
      currentIndex = entity.getChildIndex();
    }
    return entity;
  }
}
