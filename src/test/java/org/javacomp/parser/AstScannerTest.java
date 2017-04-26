package org.javacomp.parser;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.truth.Truth8;
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
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
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
  private FileScope fileScope;

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
    fileScope = scanner.startScan(compilationUnit, TEST_DATA_PATH);
  }

  @Test
  public void fileScopeHasCorrectPackage() {
    assertThat(fileScope.getPackageQualifiers()).containsExactly("test", "data").inOrder();
  }

  @Test
  public void classIsDeclaredInPackage() {
    Entity classEntity = lookupEntity(fileScope, "TestData");
    assertThat(classEntity.getKind()).isEqualTo(Entity.Kind.CLASS);
  }

  @Test
  public void classIsDeclaredGlobally() {
    List<Entity> classEntity = fileScope.getGlobalEntitiesWithName("TestData");
    assertThat(classEntity).hasSize(1);
    assertThat(classEntity.get(0).getKind()).isEqualTo(Entity.Kind.CLASS);
  }

  @Test
  public void methodIsDeclaredInClassScope() {
    Entity methodEntity = lookupEntity(fileScope, "TestData.publicIfBlockMethod");
    assertThat(methodEntity.getKind()).isEqualTo(Entity.Kind.METHOD);
  }

  @Test
  public void classStaticFieldIsDeclaredInClassScope() {
    Entity variableEntity = lookupEntity(fileScope, "TestData.publicStaticIntField");
    assertThat(variableEntity.getKind()).isEqualTo(Entity.Kind.FIELD);
  }

  @Test
  public void innerClassIsDeclaredInClassScope() {
    Entity classEntity = lookupEntity(fileScope, "TestData.PrivateStaticInnerClass");
    assertThat(classEntity.getKind()).isEqualTo(Entity.Kind.CLASS);
    Entity annotationEntity = lookupEntity(fileScope, "TestData.PublicInnerAnnotation");
    assertThat(annotationEntity.getKind()).isEqualTo(Entity.Kind.ANNOTATION);
    Entity enumEntity = lookupEntity(fileScope, "TestData.PublicInnerEnum");
    assertThat(enumEntity.getKind()).isEqualTo(Entity.Kind.ENUM);
    Entity interfaceEntity = lookupEntity(fileScope, "TestData.PublicInnerInterface");
    assertThat(interfaceEntity.getKind()).isEqualTo(Entity.Kind.INTERFACE);
  }

  @Test
  public void enumItemIsDeclaredInEnumScope() {
    Entity variableEntity = lookupEntity(fileScope, "TestData.PublicInnerEnum.ENUM_VALUE1");
    assertThat(variableEntity.getKind()).isEqualTo(Entity.Kind.FIELD);
  }

  @Test
  public void topLevelClassScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("public class TestData {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // class TestData");
    EntityScope scopeAtField = getEntityScopeAfter("publicStaticIntField;");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      assertThat(scope).isEqualTo(lookupEntity(fileScope, "TestData").getChildScope());
    }
  }

  @Test
  public void innerEnumScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("public enum PublicInnerEnum {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // PublicInnerEnum");
    EntityScope scopeAtField = getEntityScopeAfter("ENUM_VALUE1,");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      assertThat(scope)
          .isEqualTo(lookupEntity(fileScope, "TestData.PublicInnerEnum").getChildScope());
    }
  }

  @Test
  public void innerInterfaceScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("public interface PublicInnerInterface {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // PublicInnerInterface");
    EntityScope scopeAtField = getEntityScopeAfter("interfaceMethod();");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      assertThat(scope)
          .isEqualTo(lookupEntity(fileScope, "TestData.PublicInnerInterface").getChildScope());
    }
  }

  @Test
  public void methodScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("publicIfBlockMethod() {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // publicIfBlockMethod");
    EntityScope scopeAtField = getEntityScopeAfter("methodScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      MethodEntity methodEntity =
          (MethodEntity) lookupEntity(fileScope, "TestData.publicIfBlockMethod");
      assertThat(scope).isEqualTo(methodEntity.getChildScope());
    }
  }

  @Test
  public void ifBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("if (a == 1) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} else { // end of if");
    EntityScope scopeAtField = getEntityScopeAfter("ifScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(scope.getEntityWithNameAndKind("ifScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
      assertThat(scope.getEntitiesWithName("elseScopeVar")).isEmpty();
    }
  }

  @Test
  public void elseBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("else {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // else");
    EntityScope scopeAtField = getEntityScopeAfter("elseScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(scope.getEntityWithNameAndKind("elseScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
      assertThat(scope.getEntitiesWithName("ifScopeVar")).isEmpty();
    }
  }

  @Test
  public void whileBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("while (number > 0) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // while loop");
    EntityScope scopeAtField = getEntityScopeAfter("whileScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(scope.getEntityWithNameAndKind("whileScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
    }
  }

  @Test
  public void forBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("for (String s : input) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // for loop");
    EntityScope scopeAtField = getEntityScopeAfter("forScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(scope.getEntityWithNameAndKind("forScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
    }
  }

  @Test
  public void switchBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("switch (a) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // switch");
    EntityScope scopeAtField = getEntityScopeAfter("switchScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(scope.getEntityWithNameAndKind("switchScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
      assertThat(scope.getEntitiesWithName("caseScopeVar")).isEmpty();
    }
  }

  @Test
  public void switchCaseBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeBefore("{ // start of case block");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // end of case block");
    EntityScope scopeAtField = getEntityScopeAfter("caseScopeVar");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(scope.getEntityWithNameAndKind("switchScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
      Truth8.assertThat(scope.getEntityWithNameAndKind("caseScopeVar", Entity.Kind.VARIABLE))
          .isPresent();
    }
  }

  @Test
  public void annonymousClassScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("new PublicInnerInterface() {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} /* end of new PublicInnerInterface *");
    EntityScope scopeAtField = getEntityScopeAfter("privateAnnonymousClassMethod");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(
              scope.getEntityWithNameAndKind("privateAnnonymousClassMethod", Entity.Kind.METHOD))
          .isPresent();
      Truth8.assertThat(scope.getEntityWithNameAndKind("interfaceMethod", Entity.Kind.METHOD))
          .isPresent();
    }
  }

  @Test
  public void primitiveTypeReference() {
    MethodEntity methodEntity =
        (MethodEntity) lookupEntity(fileScope, "TestData.protectedWhileBlockMethod");
    TypeReference intReference = methodEntity.getParameters().get(0).getType();
    assertThat(intReference.getFullName()).containsExactly("int").inOrder();
  }

  @Test
  public void nonPrimitiveTypeReference() {
    MethodEntity methodEntity =
        (MethodEntity) lookupEntity(fileScope, "TestData.privateForBlockMethod");
    TypeReference intReference = methodEntity.getParameters().get(0).getType();
    assertThat(intReference.getFullName()).containsExactly("java", "util", "List").inOrder();
  }

  @Test
  public void explicitClassImport() {
    Truth8.assertThat(fileScope.getImportedClass("Baz"))
        .hasValue(ImmutableList.of("foo", "Bar", "Baz"));
  }

  @Test
  public void testOnDemandClassImport() {
    assertThat(fileScope.getOnDemandClassImportQualifiers())
        .containsExactly(ImmutableList.of("foo", "bar"), ImmutableList.of("foo", "bar", "baz"));
  }

  @Test
  public void typeOfClassField() {
    VariableEntity intField =
        (VariableEntity) lookupEntity(fileScope, "TestData.publicStaticIntField");
    assertThat(intField.getType().getFullName()).containsExactly("int");
  }

  @Test
  public void typeOfVariableInMethod() {
    String varName = "fullyQualifiedVar";
    EntityScope scopeEnclosingVar = getEntityScopeAfter(varName);
    VariableEntity variable = (VariableEntity) lookupEntity(scopeEnclosingVar, varName);
    assertThat(variable.getType().getFullName()).containsExactly("foo", "bar", "Baz").inOrder();
  }

  @Test
  public void enumValueHasTypeOfTheEnumClass() {
    VariableEntity enumValue =
        (VariableEntity) lookupEntity(fileScope, "TestData.PublicInnerEnum.ENUM_VALUE1");
    assertThat(enumValue.getType().getFullName()).containsExactly("PublicInnerEnum");
  }

  private EntityScope getEntityScopeAfter(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return fileScope.getEntityScopeAt(pos + subString.length());
  }

  private EntityScope getEntityScopeBefore(String subString) {
    assertThat(testDataContent).contains(subString);
    int pos = testDataContent.indexOf(subString);
    return fileScope.getEntityScopeAt(pos);
  }

  private static Entity lookupEntity(EntityScope scope, String qualifiedName) {
    String[] qualifiers = qualifiedName.split("\\.");
    EntityScope currentScope = scope;
    Entity entity = null;
    for (String qualifier : qualifiers) {
      Collection<Entity> entities = currentScope.getAllEntities().get(qualifier);
      assertThat(entities).isNotEmpty();
      entity = Iterables.getFirst(entities, null);
      currentScope = entity.getChildScope();
    }
    return entity;
  }
}
