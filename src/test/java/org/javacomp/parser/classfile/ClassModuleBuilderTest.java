package org.javacomp.parser.classfile;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.SimpleType;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.model.WildcardTypeArgument;
import org.javacomp.testing.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassModuleBuilderTest {

  private static final String TEST_DATA_DIR =
      "src/test/java/org/javacomp/parser/classfile/testdata/";

  @Test
  public void createModuleFromJarFile() throws Exception {
    Path jarPath = Paths.get(TEST_DATA_DIR, "testdata.jar");
    Module module = ClassModuleBuilder.createForJarFile(jarPath).createModule();
    assertModuleIsExpected(module);
  }

  @Test
  public void createModuleFromDirectory() throws Exception {
    Path rootPath = Paths.get(TEST_DATA_DIR);
    Module module = new ClassModuleBuilder(rootPath).createModule();
    assertModuleIsExpected(module);
  }

  @Test
  public void addInnerClassesFirst() throws Exception {
    Path rootPath = Paths.get(TEST_DATA_DIR);
    Module module = new ClassModuleBuilder(rootPath).processDirectory(rootPath);
    assertModuleIsExpected(module);
  }

  private void assertModuleIsExpected(Module module) {
    TypeReference typeOfString =
        createFormalizedTypeBuilder()
            .setPackageName("java", "lang")
            .setSimpleName("String")
            .build();

    // Class and inner classes
    ClassEntity testClass = (ClassEntity) TestUtil.lookupEntity("test.data.TestClass", module);
    ClassEntity testClass2 = (ClassEntity) TestUtil.lookupEntity("test.data.TestClass2", module);
    ClassEntity innerClass =
        (ClassEntity) TestUtil.lookupEntity("test.data.TestClass.InnerClass", module);
    ClassEntity innerClass2 =
        (ClassEntity) TestUtil.lookupEntity("test.data.TestClass.InnerClass.InnerClass2", module);
    ClassEntity innerClass3 =
        (ClassEntity)
            TestUtil.lookupEntity("test.data.TestClass.InnerClass.InnerClass2.InnerClass3", module);
    ClassEntity innerInterface =
        (ClassEntity) TestUtil.lookupEntity("test.data.TestClass.InnerInterface", module);
    ClassEntity innerEnum =
        (ClassEntity) TestUtil.lookupEntity("test.data.TestClass.InnerEnum", module);
    ClassEntity innerAnnotation =
        (ClassEntity) TestUtil.lookupEntity("test.data.TestClass.InnerAnnotation", module);

    // Class kinds
    assertThat(testClass.getKind()).isEqualTo(Entity.Kind.CLASS);
    assertThat(innerClass.getKind()).isEqualTo(Entity.Kind.CLASS);
    assertThat(innerAnnotation.getKind()).isEqualTo(Entity.Kind.ANNOTATION);
    assertThat(innerInterface.getKind()).isEqualTo(Entity.Kind.INTERFACE);
    assertThat(innerEnum.getKind()).isEqualTo(Entity.Kind.ENUM);

    // Static
    assertThat(testClass.isStatic()).isEqualTo(false);
    assertThat(innerClass.isStatic()).isEqualTo(false);
    assertThat(innerClass2.isStatic()).isEqualTo(false);
    assertThat(innerClass3.isStatic()).isEqualTo(false);
    assertThat(innerInterface.isStatic()).isEqualTo(true);
    assertThat(innerAnnotation.isStatic()).isEqualTo(true);
    assertThat(innerEnum.isStatic()).isEqualTo(true);

    // Super class and interfaces
    TypeReference typeOfSuperClass =
        createFormalizedTypeBuilder()
            .setPackageName("java", "util")
            .setSimpleName("AbstractList")
            .setTypeArguments(typeOfString)
            .build();
    TypeReference typeOfTestClass =
        createFormalizedTypeBuilder()
            .setPackageName("test", "data")
            .setSimpleName("TestClass")
            .build();
    TypeReference typeOfInterface =
        createFormalizedTypeBuilder()
            .setPackageName("java", "lang")
            .setSimpleName("Comparable")
            .setTypeArguments(typeOfTestClass)
            .build();
    assertThat(testClass.getSuperClass().get()).isEqualTo(typeOfSuperClass);
    assertThat(testClass.getInterfaces()).containsExactly(typeOfInterface);

    // Fields and their types
    VariableEntity publicStaticField = testClass.getFieldWithName("PUBLIC_STATIC_FIELD").get();
    assertThat(publicStaticField.isStatic()).isEqualTo(true);
    assertThat(publicStaticField.getType()).isEqualTo(TypeReference.INT_TYPE);

    VariableEntity innerClass3Field = testClass.getFieldWithName("innerClass3Field").get();
    assertThat(innerClass3Field.isStatic()).isEqualTo(false);
    TypeReference typeOfInnerClass3Field =
        createFormalizedTypeBuilder()
            .setPackageName("test", "data")
            .setEnclosingClasses(
                createSimpleType("TestClass"),
                createSimpleType("InnerClass", typeOfString),
                createSimpleType("InnerClass2"))
            .setSimpleName("InnerClass3")
            .build();
    assertThat(innerClass3Field.getType()).isEqualTo(typeOfInnerClass3Field);

    // Methods
    MethodEntity genericMethod = innerInterface.getMethodsWithName("genericMethod").get(0);
    TypeReference typeOfX = createUnformalizedTypeBuilder().setFullName("X").build();
    TypeReference typeOfY = createUnformalizedTypeBuilder().setFullName("Y").build();
    TypeArgument extendsX =
        WildcardTypeArgument.create(
            WildcardTypeArgument.Bound.create(WildcardTypeArgument.Bound.Kind.EXTENDS, typeOfX));
    TypeArgument superY =
        WildcardTypeArgument.create(
            WildcardTypeArgument.Bound.create(WildcardTypeArgument.Bound.Kind.SUPER, typeOfY));
    TypeArgument noBoundType = WildcardTypeArgument.create(Optional.empty());
    TypeReference typeOfArg0 =
        createFormalizedTypeBuilder()
            .setPackageName("java", "util")
            .setSimpleName("List")
            .setTypeArguments(typeOfX)
            .build();
    TypeReference typeOfArg2 =
        createFormalizedTypeBuilder()
            .setPackageName("java", "util")
            .setEnclosingClasses(createSimpleType("Map"))
            .setSimpleName("Entry")
            .setTypeArguments(extendsX, superY)
            .build();
    TypeReference typeOfArg3 =
        createFormalizedTypeBuilder()
            .setPackageName("java", "util")
            .setSimpleName("List")
            .setTypeArguments(noBoundType)
            .build();
    assertThat(genericMethod.isStatic()).isEqualTo(false);
    assertThat(genericMethod.getReturnType()).isEqualTo(typeOfX);
    assertThat(genericMethod.getParameters().get(0).getType()).isEqualTo(typeOfArg0);
    assertThat(genericMethod.getParameters().get(1).getType()).isEqualTo(typeOfString);
    assertThat(genericMethod.getParameters().get(2).getType()).isEqualTo(typeOfArg2);
    assertThat(genericMethod.getParameters().get(3).getType()).isEqualTo(typeOfArg3);
  }

  private static TypeReference.Builder createUnformalizedTypeBuilder() {
    return TypeReference.builder().setPrimitive(false).setArray(false).setTypeArguments();
  }

  private static TypeReference.Builder createFormalizedTypeBuilder() {
    return TypeReference.formalizedBuilder()
        .setEnclosingClasses()
        .setPrimitive(false)
        .setArray(false)
        .setTypeArguments();
  }

  private static SimpleType createSimpleType(String simpleName, TypeArgument... typeArguments) {
    return SimpleType.builder()
        .setSimpleName(simpleName)
        .setPrimitive(false)
        .setTypeArguments(typeArguments)
        .build();
  }

  private static class ListDirectoryStream implements DirectoryStream<Path> {
    private final ImmutableList<Path> pathList;

    private ListDirectoryStream(Path... paths) {
      this.pathList = ImmutableList.copyOf(paths);
    }

    @Override
    public Iterator<Path> iterator() {
      return pathList.iterator();
    }

    @Override
    public void close() {}
  }
}
