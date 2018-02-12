package org.javacomp.parser.classfile;

import static com.google.common.truth.Truth.assertThat;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.javacomp.parser.classfile.ConstantPoolInfo.ConstantClassInfo;
import org.javacomp.parser.classfile.ConstantPoolInfo.ConstantUtf8Info;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassFileParserTest {
  private static final String TEST_DATA_DIR =
      "src/test/java/org/javacomp/parser/classfile/testdata/";
  private static final String CLASS_FILE = "TestClass.class";
  private static final String INNER_INTERFACE_FILE = "TestClass$InnerInterface.class";

  private ClassFileInfo testClass;
  private ClassFileInfo innerInterface;
  private Tester testClassTester;
  private Tester innerInterfaceTester;

  @Before
  public void setUpClassFile() throws Exception {
    ClassFileParser parser = new ClassFileParser();
    testClass = parser.parse(Paths.get(TEST_DATA_DIR, CLASS_FILE));
    testClassTester = new Tester(testClass);
    innerInterface = parser.parse(Paths.get(TEST_DATA_DIR, INNER_INTERFACE_FILE));
    innerInterfaceTester = new Tester(innerInterface);
  }

  @Test
  public void thisClassIndex() throws Exception {
    assertThat(testClassTester.getClassName(testClass.getThisClassIndex()))
        .isEqualTo("test/data/TestClass");
  }

  @Test
  public void superClassIndex() throws Exception {
    assertThat(testClassTester.getClassName(testClass.getSuperClassIndex()))
        .isEqualTo("java/util/AbstractList");
  }

  @Test
  public void interfaceIndeces() throws Exception {
    ImmutableList<Integer> interfaceIndeces = testClass.getInterfaceIndeces();
    assertThat(interfaceIndeces).hasSize(1);
    assertThat(testClassTester.getClassName(interfaceIndeces.get(0)))
        .isEqualTo("java/lang/Comparable");
  }

  @Test
  public void fieldNames() throws Exception {
    List<String> fieldNames =
        testClass
            .getFields()
            .stream()
            .map((field) -> testClassTester.getUtf8(field.getNameIndex()))
            .collect(Collectors.toList());
    assertThat(fieldNames)
        .containsExactly(
            "PUBLIC_STATIC_FIELD",
            "LONG_CONSTANT",
            "DOUBLE_CONSTANT",
            "privateStringListField",
            "protectedStringField",
            "packagePrivateBooleanField",
            "innerClass3Field");
  }

  @Test
  public void fieldDescriptor() throws Exception {
    assertThat(testClassTester.getFieldDescriptorByName("privateStringListField"))
        .isEqualTo("Ljava/util/List;");
    assertThat(testClassTester.getFieldDescriptorByName("packagePrivateBooleanField"))
        .isEqualTo("Z");
  }

  @Test
  public void fieldSignature() throws Exception {
    FieldInfo field = testClassTester.getFieldByName("privateStringListField");
    assertThat(testClassTester.getSignature(field.getAttributeInfos()))
        .isEqualTo("Ljava/util/List<Ljava/lang/String;>;");

    field = testClassTester.getFieldByName("innerClass3Field");
    assertThat(testClassTester.getSignature(field.getAttributeInfos()))
        .isEqualTo("Ltest/data/TestClass$InnerClass<Ljava/lang/String;>.InnerClass2.InnerClass3;");
  }

  @Test
  public void methodNames() throws Exception {
    List<String> methodNames =
        testClass
            .getMethods()
            .stream()
            .map((method) -> testClassTester.getUtf8(method.getNameIndex()))
            .collect(Collectors.toList());
    assertThat(methodNames)
        .containsExactly("<init>", "compareTo", "compareTo", "size", "get", "get");
  }

  @Test
  public void methodSignature() throws Exception {
    MethodInfo method = innerInterfaceTester.getMethodByName("genericMethod");
    assertThat(innerInterfaceTester.getSignature(method.getAttributeInfos()))
        .isEqualTo(
            "<X:Ljava/lang/Exception;Y:Ljava/lang/Object;>"
                + "("
                + "Ljava/util/List<TX;>;"
                + "Ljava/lang/String;"
                + "Ljava/util/Map$Entry<+TX;-TY;>;"
                + "Ljava/util/List<*>;"
                + ")"
                + "TX;"
                + "^TX;"
                + "^Ljava/lang/Exception;");
  }

  @Test
  public void methodDescriptor() throws Exception {
    assertThat(innerInterfaceTester.getMethodDescriptorByName("genericMethod"))
        .isEqualTo(
            "(Ljava/util/List;Ljava/lang/String;Ljava/util/Map$Entry;Ljava/util/List;)Ljava/lang/Exception;");
  }

  @Test
  public void classSignature() throws Exception {
    assertThat(testClassTester.getSignature(testClass.getAttributes()))
        .isEqualTo(
            "Ljava/util/AbstractList<Ljava/lang/String;>;Ljava/lang/Comparable<Ltest/data/TestClass;>;");
  }

  @Test
  public void innerInterfaceIndex() throws Exception {
    assertThat(innerInterfaceTester.getClassName(innerInterface.getThisClassIndex()))
        .isEqualTo("test/data/TestClass$InnerInterface");
  }

  @Test
  public void innerClassAttribute() throws Exception {
    assertThat(testClassTester.getInnerClassNames())
        .containsExactly(
            InnerClassName.create(
                "test/data/TestClass$InnerInterface", "test/data/TestClass", "InnerInterface"),
            InnerClassName.create(
                "test/data/TestClass$InnerEnum", "test/data/TestClass", "InnerEnum"),
            InnerClassName.create(
                "test/data/TestClass$InnerAnnotation", "test/data/TestClass", "InnerAnnotation"),
            InnerClassName.create(
                "test/data/TestClass$InnerClass", "test/data/TestClass", "InnerClass"),
            InnerClassName.create(
                "test/data/TestClass$InnerClass$InnerClass2",
                "test/data/TestClass$InnerClass",
                "InnerClass2"),
            InnerClassName.create(
                "test/data/TestClass$InnerClass$InnerClass2$InnerClass3",
                "test/data/TestClass$InnerClass$InnerClass2",
                "InnerClass3"));
    assertThat(innerInterfaceTester.getInnerClassNames())
        .containsExactly(
            InnerClassName.create("java/util/Map$Entry", "java/util/Map", "Entry"),
            InnerClassName.create(
                "test/data/TestClass$InnerInterface", "test/data/TestClass", "InnerInterface"));
  }

  private static class Tester {
    private final ClassFileInfo classFileInfo;

    private Tester(ClassFileInfo classFileInfo) {
      this.classFileInfo = classFileInfo;
    }

    private String getUtf8(int index) {
      return ((ConstantUtf8Info) classFileInfo.getConstantPool().get(index)).getValue();
    }

    private ConstantClassInfo getClassInfo(int index) {
      return (ConstantClassInfo) classFileInfo.getConstantPool().get(index);
    }

    private String getClassName(int classIndex) {
      return getUtf8(getClassInfo(classIndex).getNameIndex());
    }

    private String getFieldName(int fieldIndex) {
      return getUtf8(classFileInfo.getFields().get(fieldIndex).getNameIndex());
    }

    private String getFieldDescriptorByName(String name) {
      return getUtf8(getFieldByName(name).getDescriptorIndex());
    }

    private FieldInfo getFieldByName(String name) {
      for (FieldInfo field : classFileInfo.getFields()) {
        String fieldName = getUtf8(field.getNameIndex());
        if (name.equals(fieldName)) {
          return field;
        }
      }
      throw new RuntimeException("Field " + name + " not found");
    }

    private MethodInfo getMethodByName(String name) {
      for (MethodInfo method : classFileInfo.getMethods()) {
        String methodName = getUtf8(method.getNameIndex());
        if (name.equals(methodName)) {
          return method;
        }
      }
      throw new RuntimeException("Method " + name + " not found");
    }

    private String getMethodDescriptorByName(String name) {
      return getUtf8(getMethodByName(name).getDescriptorIndex());
    }

    private String getSignature(ImmutableList<AttributeInfo> attributes) {
      for (AttributeInfo attribute : attributes) {
        if (attribute instanceof AttributeInfo.Signature) {
          return getUtf8(((AttributeInfo.Signature) attribute).getSignatureIndex());
        }
      }
      throw new RuntimeException("No signature attributes in " + attributes);
    }

    private <T extends AttributeInfo> T getFirstAttribute(Class<T> clazz) {
      for (AttributeInfo attributeInfo : classFileInfo.getAttributes()) {
        if (clazz.isAssignableFrom(attributeInfo.getClass())) {
          @SuppressWarnings("unchecked")
          T ret = (T) attributeInfo;
          return ret;
        }
      }
      throw new RuntimeException(
          "Can not find an attribute of type "
              + clazz.getSimpleName()
              + " from "
              + classFileInfo.getAttributes());
    }

    private List<InnerClassName> getInnerClassNames() {
      AttributeInfo.InnerClass innerClassAttribute =
          getFirstAttribute(AttributeInfo.InnerClass.class);
      return innerClassAttribute
          .getClasses()
          .stream()
          .map(
              classInfo -> {
                String outerClassName = getClassName(classInfo.getOuterClassInfoIndex());
                String innerClassName = getClassName(classInfo.getInnerClassInfoIndex());
                String innerName = getUtf8(classInfo.getInnerNameIndex());
                return InnerClassName.create(innerClassName, outerClassName, innerName);
              })
          .collect(Collectors.toList());
    }
  }

  @AutoValue
  abstract static class InnerClassName {
    abstract String getInnerClassName();

    abstract String getOuterClassName();

    abstract String getInnerNmae();

    static InnerClassName create(String innerClassName, String outerClassName, String innerName) {
      return new AutoValue_ClassFileParserTest_InnerClassName(
          innerClassName, outerClassName, innerName);
    }
  }
}
