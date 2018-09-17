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
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeParameter;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.model.WildcardTypeArgument;
import org.javacomp.options.IndexOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AstScannerTest {
  private static final String TEST_DATA_PATH =
      "src/test/java/org/javacomp/parser/testdata/TestData.java";

  private final AstScanner scanner = new AstScanner(IndexOptions.FULL_INDEX_BUILDER.build());

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
    fileScope = scanner.startScan(compilationUnit, TEST_DATA_PATH, testDataContent);
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
    Collection<Entity> testDataEntities = fileScope.getMemberEntities().get("TestData");
    assertThat(testDataEntities).hasSize(1);
    assertThat(Iterables.getOnlyElement(testDataEntities).getKind()).isEqualTo(Entity.Kind.CLASS);
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
  public void fileScopeRange() {
    EntityScope scopeBeforeClass = getEntityScopeBefore("import foo");
    assertThat(scopeBeforeClass).isEqualTo(fileScope);
  }

  @Test
  public void topLevelClassScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("public class TestData {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // class TestData");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      assertThat(scope).isEqualTo(lookupEntity(fileScope, "TestData").getScope());
    }
  }

  @Test
  public void innerEnumScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("public enum PublicInnerEnum {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // PublicInnerEnum");
    EntityScope scopeAtField = getEntityScopeAfter("ENUM_VALUE1,");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      assertThat(scope).isEqualTo(lookupEntity(fileScope, "TestData.PublicInnerEnum").getScope());
    }
  }

  @Test
  public void innerInterfaceScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("public interface PublicInnerInterface {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // PublicInnerInterface");
    EntityScope scopeAtField = getEntityScopeAfter("interfaceMethod();");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      assertThat(scope)
          .isEqualTo(lookupEntity(fileScope, "TestData.PublicInnerInterface").getScope());
    }
  }

  @Test
  public void methodScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("publicIfBlockMethod() {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // publicIfBlockMethod");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      MethodEntity methodEntity =
          (MethodEntity) lookupEntity(fileScope, "TestData.publicIfBlockMethod");
      assertThat(scope).isEqualTo(methodEntity.getScope());
    }
  }

  @Test
  public void ifBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("if (a == 1) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} else { // end of if");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      Truth8.assertThat(getEntity(scope, "ifScopeVar", Entity.Kind.VARIABLE)).isPresent();
      assertThat(scope.getMemberEntities().get("elseScopeVar")).isEmpty();
    }
  }

  @Test
  public void elseBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("else {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // else");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      Truth8.assertThat(getEntity(scope, "elseScopeVar", Entity.Kind.VARIABLE)).isPresent();
      assertThat(scope.getMemberEntities().get("ifScopeVar")).isEmpty();
    }
  }

  @Test
  public void whileBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("while (number > 0) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // while loop");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      Truth8.assertThat(getEntity(scope, "whileScopeVar", Entity.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void forBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("for (String s : input) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // for loop");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      Truth8.assertThat(getEntity(scope, "forScopeVar", Entity.Kind.VARIABLE)).isPresent();
    }
  }

  @Test
  public void switchBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("switch (a) {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // switch");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      Truth8.assertThat(getEntity(scope, "switchScopeVar", Entity.Kind.VARIABLE)).isPresent();
      assertThat(scope.getMemberEntities().get("caseScopeVar")).isEmpty();
    }
  }

  @Test
  public void switchCaseBlockScopeRange() {
    EntityScope scopeAtStart = getEntityScopeBefore("{ // start of case block");
    EntityScope scopeAtEnd = getEntityScopeBefore("} // end of case block");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd)) {
      Truth8.assertThat(getEntity(scope, "caseScopeVar", Entity.Kind.VARIABLE)).isPresent();
    }
    EntityScope switchScope = getEntityScopeAfter("switch (a) {");
    Truth8.assertThat(getEntity(switchScope, "switchScopeVar", Entity.Kind.VARIABLE)).isPresent();
  }

  @Test
  public void annonymousClassScopeRange() {
    EntityScope scopeAtStart = getEntityScopeAfter("new PublicInnerInterface() {");
    EntityScope scopeAtEnd = getEntityScopeBefore("} /* end of new PublicInnerInterface *");
    EntityScope scopeAtField = getEntityScopeBefore(" private void privateAnnonymousClassMethod");
    for (EntityScope scope : ImmutableList.of(scopeAtStart, scopeAtEnd, scopeAtField)) {
      Truth8.assertThat(getEntity(scope, "privateAnnonymousClassMethod", Entity.Kind.METHOD))
          .isPresent();
      Truth8.assertThat(getEntity(scope, "interfaceMethod", Entity.Kind.METHOD)).isPresent();
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
  public void classNameRange() {
    assertEntityNameRange(lookupEntity(fileScope, "TestData"), "class TestData");
    assertEntityNameRange(
        lookupEntity(fileScope, "TestData.PublicInnerInterface"),
        "public interface PublicInnerInterface");
  }

  @Test
  public void methodNameRange() {
    assertEntityNameRange(
        lookupEntity(fileScope, "TestData.publicIfBlockMethod"),
        "public boolean publicIfBlockMethod");
  }

  @Test
  public void fieldNameRange() {
    assertEntityNameRange(
        lookupEntity(fileScope, "TestData.publicStaticIntField"),
        "public static final int publicStaticIntField");
  }

  @Test
  public void parameterNameRange() {
    Entity method = lookupEntity(fileScope, "TestData.protectedWhileBlockMethod");
    Entity parameter =
        Iterables.getOnlyElement(method.getScope().getMemberEntities().get("number"));
    assertEntityNameRange(parameter, "protected int protectedWhileBlockMethod(int number)");
  }

  @Test
  public void variableNameRange() {
    Entity method = lookupEntity(fileScope, "TestData.protectedWhileBlockMethod");
    Entity parameter = Iterables.getOnlyElement(method.getScope().getMemberEntities().get("ret"));
    assertEntityNameRange(parameter, "int ret = 0;  // variable");
  }

  @Test
  public void typeArgument() {
    VariableEntity typeArgumentField =
        (VariableEntity) lookupEntity(fileScope, "TestData.typeArgumentField");

    List<TypeArgument> typeArguments = typeArgumentField.getType().getTypeArguments();
    assertThat(typeArguments).hasSize(3);

    assertThat(typeArguments.get(0)).isInstanceOf(TypeReference.class);
    TypeReference typeArg0 = (TypeReference) typeArguments.get(0);
    assertThat(typeArg0.getFullName()).containsExactly("foo", "bar", "baz", "Baz").inOrder();
    assertThat(typeArg0.getTypeArguments()).isEmpty();

    assertThat(typeArguments.get(1)).isInstanceOf(WildcardTypeArgument.class);
    WildcardTypeArgument typeArg1 = (WildcardTypeArgument) typeArguments.get(1);
    Truth8.assertThat(typeArg1.getBound()).isEmpty();

    assertThat(typeArguments.get(2)).isInstanceOf(WildcardTypeArgument.class);
    WildcardTypeArgument typeArg2 = (WildcardTypeArgument) typeArguments.get(2);
    Truth8.assertThat(typeArg2.getBound()).isPresent();

    WildcardTypeArgument.Bound bound = typeArg2.getBound().get();
    assertThat(bound.getKind()).isEqualTo(WildcardTypeArgument.Bound.Kind.SUPER);

    TypeReference boundType = bound.getTypeReference();
    assertThat(boundType.getFullName()).containsExactly("B");
    assertThat(boundType.getTypeArguments()).hasSize(1);

    assertThat(boundType.getTypeArguments().get(0)).isInstanceOf(WildcardTypeArgument.class);
    WildcardTypeArgument subTypeVar = (WildcardTypeArgument) boundType.getTypeArguments().get(0);
    Truth8.assertThat(subTypeVar.getBound()).isPresent();

    WildcardTypeArgument.Bound subBound = subTypeVar.getBound().get();
    assertThat(subBound.getKind()).isEqualTo(WildcardTypeArgument.Bound.Kind.EXTENDS);
    assertThat(subBound.getTypeReference().getFullName()).containsExactly("C");
  }

  @Test
  public void typeParameter() {
    TypeParameter typeParamA = TypeParameter.create("A", ImmutableList.of());
    TypeParameter typeParamB =
        TypeParameter.create(
            "B",
            ImmutableList.of(
                TypeReference.builder()
                    .setFullName("A")
                    .setPrimitive(false)
                    .setArray(false)
                    .setTypeArguments(ImmutableList.of())
                    .build()));
    TypeParameter typeParamC =
        TypeParameter.create(
            "C",
            ImmutableList.of(
                TypeReference.builder()
                    .setFullName("String")
                    .setPrimitive(false)
                    .setArray(false)
                    .setTypeArguments(ImmutableList.of())
                    .build(),
                TypeReference.builder()
                    .setFullName("List")
                    .setPrimitive(false)
                    .setArray(false)
                    .setTypeArguments(
                        ImmutableList.of(
                            TypeReference.builder()
                                .setFullName("B")
                                .setPrimitive(false)
                                .setArray(false)
                                .setTypeArguments(ImmutableList.of())
                                .build()))
                    .build()));

    ClassEntity parameterizedClassEntity =
        (ClassEntity) lookupEntity(fileScope, "TestData.ParameterizedClass");
    assertThat(parameterizedClassEntity.getTypeParameters())
        .containsExactly(typeParamA, typeParamB)
        .inOrder();
    MethodEntity getCMethod =
        (MethodEntity) lookupEntity(fileScope, "TestData.ParameterizedClass.getC");
    assertThat(getCMethod.getTypeParameters()).containsExactly(typeParamC);
  }

  private void assertEntityNameRange(Entity entity, String locator) {
    assertThat(locator).contains(entity.getSimpleName());
    int start = testDataContent.indexOf(locator);
    assertThat(start).isGreaterThan(-1);
    start = testDataContent.indexOf(entity.getSimpleName(), start);
    assertThat(entity.getSymbolRange().lowerEndpoint()).isEqualTo(start);
    assertThat(
            testDataContent.substring(
                entity.getSymbolRange().lowerEndpoint(), entity.getSymbolRange().upperEndpoint()))
        .isEqualTo(entity.getSimpleName());
  }

  @Test
  public void explicitClassImport() {
    Truth8.assertThat(fileScope.getImportedClass("Baz"))
        .hasValue(ImmutableList.of("foo", "Bar", "Baz"));
  }

  @Test
  public void explicitStaticImport() {
    Truth8.assertThat(fileScope.getImportedStaticMember("baa"))
        .hasValue(ImmutableList.of("foo", "Bar", "baa"));
  }

  @Test
  public void onDemandClassImport() {
    assertThat(fileScope.getOnDemandClassImportQualifiers())
        .containsExactly(ImmutableList.of("foo", "bar"), ImmutableList.of("foo", "bar", "baz"));
  }

  @Test
  public void onDemandStaticImport() {
    assertThat(fileScope.getOnDemandStaticImportQualifiers())
        .containsExactly(ImmutableList.of("foo", "Bar", "Baz"));
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
    EntityScope varScope = getEntityScopeAfter(varName);
    Optional<Entity> varEntity = varScope.getDefiningEntity();
    Truth8.assertThat(varEntity).isPresent();
    assertThat(varEntity.get().getSimpleName()).isEqualTo(varName);
    assertThat(varEntity.get()).isInstanceOf(VariableEntity.class);
    assertThat(((VariableEntity) varEntity.get()).getType().getFullName())
        .containsExactly("foo", "bar", "Baz")
        .inOrder();
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
      Collection<Entity> entities = currentScope.getMemberEntities().get(qualifier);
      assertThat(entities).isNotEmpty();
      entity = Iterables.getFirst(entities, null);
      currentScope = entity.getScope();
    }
    return entity;
  }

  private static Optional<Entity> getEntity(EntityScope scope, String name, Entity.Kind kind) {
    for (Entity entity : scope.getMemberEntities().get(name)) {
      if (entity.getKind() == kind) {
        return Optional.of(entity);
      }
    }
    return Optional.empty();
  }
}
