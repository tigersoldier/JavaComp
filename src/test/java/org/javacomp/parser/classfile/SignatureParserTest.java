package org.javacomp.parser.classfile;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.EnumSet;
import org.javacomp.model.SimpleType;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeParameter;
import org.javacomp.model.TypeReference;
import org.javacomp.model.WildcardTypeArgument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SignatureParserTest {
  private static final EnumSet<ClassAccessFlag> NO_CLASS_ACCESS_FLAG =
      EnumSet.noneOf(ClassAccessFlag.class);
  private static final ImmutableMap<String, InnerClassEntry> INNER_CLASS_MAP =
      ImmutableMap.of(
          "foo/Inner1$Inner2",
          InnerClassEntry.create("foo/Inner1", "Inner2", NO_CLASS_ACCESS_FLAG),
          "foo/Inner1$Inner2$Inner3",
          InnerClassEntry.create("foo/Inner1$Inner2", "Inner3", NO_CLASS_ACCESS_FLAG),
          "foo/Inner1$Inner2$Inner3$Inner4",
          InnerClassEntry.create("foo/Inner1$Inner2$Inner3", "Inner4", NO_CLASS_ACCESS_FLAG));

  private static final TypeReference CLASS_BAR = classType(ImmutableList.of("foo"), "Bar").build();
  private static final TypeReference CLASS_BAR_ARRAY = CLASS_BAR.toBuilder().setArray(true).build();
  private static final TypeReference CLASS_FOZ = classType(ImmutableList.of("foo"), "Foz").build();
  private static final TypeReference TYPE_ARGUMENT_X =
      TypeReference.builder()
          .setArray(false)
          .setPrimitive(false)
          .setFullName("X")
          .setTypeArguments()
          .build();
  private static final TypeReference NON_TYPED_INNER_4 =
      TypeReference.formalizedBuilder()
          .setPackageName("foo")
          .setEnclosingClasses(
              simpleReferenceType("Inner1"),
              simpleReferenceType("Inner2"),
              simpleReferenceType("Inner3"))
          .setSimpleName("Inner4")
          .setArray(false)
          .setPrimitive(false)
          .setTypeArguments()
          .build();
  private static final TypeReference TYPED_INNER_4 =
      TypeReference.formalizedBuilder()
          .setPackageName("foo")
          .setEnclosingClasses(
              simpleReferenceType("Inner1"),
              simpleReferenceType("Inner2", CLASS_BAR),
              simpleReferenceType("Inner3"))
          .setSimpleName("Inner4")
          .setArray(false)
          .setPrimitive(false)
          .setTypeArguments(CLASS_FOZ)
          .build();

  @Test
  public void parseClassSignature_simpleSuper() throws Exception {
    String signature = "Lfoo/Bar;";
    ClassSignature expected = ClassSignature.builder().setSuperClass(CLASS_BAR).build();
    ClassSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseClassSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseClassSignature_typedSuper() throws Exception {
    String signature = "Lfoo/Baz<Lfoo/Bar;>;";
    ClassSignature expected = ClassSignature.builder().setSuperClass(classBaz(CLASS_BAR)).build();
    ClassSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseClassSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseClassSignature_simpleSuperAndTypedInterface() throws Exception {
    String signature = "Lfoo/Bar;Lfoo/Baz<Lfoo/Foz;>;";
    ClassSignature expected =
        ClassSignature.builder().setSuperClass(CLASS_BAR).addInterface(classBaz(CLASS_FOZ)).build();
    ClassSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseClassSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseClassSignature_simpleSuperAndTwoInterfaces() throws Exception {
    String signature = "Lfoo/Bar;Lfoo/Foz;Lfoo/Bar;";
    ClassSignature expected =
        ClassSignature.builder()
            .setSuperClass(CLASS_BAR)
            .addInterface(CLASS_FOZ)
            .addInterface(CLASS_BAR)
            .build();
    ClassSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseClassSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseClassSignature_withTypeVariables() throws Exception {
    String signature = "<X:Lfoo/Foz;>Lfoo/Baz<+TX;>;";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of(CLASS_FOZ));
    TypeArgument typeArgumentExtendsX =
        WildcardTypeArgument.create(
            WildcardTypeArgument.Bound.create(
                WildcardTypeArgument.Bound.Kind.EXTENDS, TYPE_ARGUMENT_X));
    ClassSignature expected =
        ClassSignature.builder()
            .addTypeParameter(typeParameterX)
            .setSuperClass(classBaz(typeArgumentExtendsX))
            .build();
    ClassSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseClassSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseMethodSignature_simpleSignature() throws Exception {
    String signature = "()V";
    MethodSignature expected =
        MethodSignature.builder()
            .setResult(TypeReference.VOID_TYPE)
            .setTypeParameters(ImmutableList.of())
            .build();
    MethodSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseMethodSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseMethodSignature_fullSignature() throws Exception {
    String signature = "<X:Lfoo/Bar;>(Lfoo/Baz<TX;>;B)Lfoo/Foz;^TX;^Lfoo/Bar;";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of(CLASS_BAR));
    MethodSignature expected =
        MethodSignature.builder()
            .setResult(CLASS_FOZ)
            .setTypeParameters(ImmutableList.of(typeParameterX))
            .addParameter(classBaz(TYPE_ARGUMENT_X))
            .addParameter(TypeReference.BYTE_TYPE)
            .addThrowsSignature(TYPE_ARGUMENT_X)
            .addThrowsSignature(CLASS_BAR)
            .build();
    MethodSignature parsedSignature =
        new SignatureParser(signature, INNER_CLASS_MAP).parseMethodSignature();
    assertThat(parsedSignature).isEqualTo(expected);
  }

  @Test
  public void parseTypeParameters_singleParameterNoBound() {
    String signature = "<X:>";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of());
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX);
  }

  @Test
  public void parseTypeParameters_singleParameterNoClassBoundOneInterfaceBound() {
    String signature = "<X::Lfoo/Bar;>";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of(CLASS_BAR));
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX);
  }

  @Test
  public void parseTypeParameters_singleParameterOneBound() {
    String signature = "<X:Lfoo/Bar;>";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of(CLASS_BAR));
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX);
  }

  @Test
  public void parseTypeParameters_singleParameterMultipleBounds() {
    String signature = "<X:Lfoo/Bar;:Lfoo/Baz<TX;>;:Lfoo/Foz;>";
    TypeParameter typeParameterX =
        TypeParameter.create(
            "X", ImmutableList.of(CLASS_BAR, classBaz(TYPE_ARGUMENT_X), CLASS_FOZ));
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX);
  }

  @Test
  public void parseTypeParameters_multipleParametersWithNoBound() {
    String signature = "<X:Y:>";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of());
    TypeParameter typeParameterY = TypeParameter.create("Y", ImmutableList.of());
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX, typeParameterY);
  }

  @Test
  public void parseTypeParameters_multipleParametersWithNoClassBoundOneInterfaceBound() {
    String signature = "<X::Lfoo/Bar;Y::Lfoo/Foz;>";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of(CLASS_BAR));
    TypeParameter typeParameterY = TypeParameter.create("Y", ImmutableList.of(CLASS_FOZ));
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX, typeParameterY);
  }

  @Test
  public void parseTypeParameters_multipleParametersWithOneBound() {
    String signature = "<X:[Lfoo/Bar;Y:TX;>";
    TypeParameter typeParameterX = TypeParameter.create("X", ImmutableList.of(CLASS_BAR_ARRAY));
    TypeParameter typeParameterY = TypeParameter.create("Y", ImmutableList.of(TYPE_ARGUMENT_X));
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseTypeParameters())
        .containsExactly(typeParameterX, typeParameterY);
  }

  @Test
  public void parseReferenceTypeSignature_classTypeSignature() {
    String signature = "Lfoo/Bar;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseReferenceTypeSignature())
        .isEqualTo(CLASS_BAR);
  }

  @Test
  public void parseReferenceTypeSignature_classTypeSignatureWithTypeArguments() {
    String signature = "Lfoo/Baz<+Lfoo/Bar;>;";
    TypeArgument extendsBar =
        WildcardTypeArgument.create(
            WildcardTypeArgument.Bound.create(WildcardTypeArgument.Bound.Kind.EXTENDS, CLASS_BAR));
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseReferenceTypeSignature())
        .isEqualTo(classBaz(extendsBar));
  }

  @Test
  public void parseReferenceTypeSignature_typeVariable() {
    String signature = "Lfoo/Baz<Lfoo/Bar;>;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseReferenceTypeSignature())
        .isEqualTo(classBaz(CLASS_BAR));
  }

  @Test
  public void parseReferenceTypeSignature_array() {
    String signature = "[Lfoo/Bar;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseReferenceTypeSignature())
        .isEqualTo(CLASS_BAR_ARRAY);
  }

  @Test
  public void parseFieldReference_referenceType() {
    String signature = "Lfoo/Bar;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseFieldReference())
        .isEqualTo(CLASS_BAR);
  }

  @Test
  public void parseJavaTypeSignature_referenceType() {
    String signature = "Lfoo/Bar;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature())
        .isEqualTo(CLASS_BAR);
  }

  @Test
  public void parseJavaTypeSignature_primitiveTypes() {
    String signature;
    TypeReference expected;
    TypeReference fieldReference;

    signature = "B";
    expected = TypeReference.BYTE_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "C";
    expected = TypeReference.CHAR_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "D";
    expected = TypeReference.DOUBLE_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "F";
    expected = TypeReference.FLOAT_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "I";
    expected = TypeReference.INT_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "J";
    expected = TypeReference.LONG_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "S";
    expected = TypeReference.SHORT_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "Z";
    expected = TypeReference.BOOLEAN_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);

    signature = "B";
    expected = TypeReference.BYTE_TYPE;
    fieldReference = new SignatureParser(signature, INNER_CLASS_MAP).parseJavaTypeSignature();
    assertThat(fieldReference).isEqualTo(expected);
  }

  @Test
  public void parseInnerClass_withAllDollarSigns() throws Exception {
    String signature = "Lfoo/Inner1$Inner2$Inner3$Inner4;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseClassTypeSignature())
        .isEqualTo(NON_TYPED_INNER_4);
  }

  @Test
  public void parseInnerClass_withAllDots() throws Exception {
    String signature = "Lfoo/Inner1.Inner2.Inner3.Inner4;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseClassTypeSignature())
        .isEqualTo(NON_TYPED_INNER_4);
  }

  @Test
  public void parseInnerClass_withMixedDollarSignsAndDots() throws Exception {
    String signature = "Lfoo/Inner1$Inner2.Inner3$Inner4;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseClassTypeSignature())
        .isEqualTo(NON_TYPED_INNER_4);
  }

  @Test
  public void parseInnerClass_withTypeArgumentsAndDollarSigns() throws Exception {
    String signature = "Lfoo/Inner1$Inner2<Lfoo/Bar;>.Inner3$Inner4<Lfoo/Foz;>;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseClassTypeSignature())
        .isEqualTo(TYPED_INNER_4);
  }

  @Test
  public void parseInnerClass_withTypeArgumentsAndDots() throws Exception {
    String signature = "Lfoo/Inner1.Inner2<Lfoo/Bar;>.Inner3.Inner4<Lfoo/Foz;>;";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseClassTypeSignature())
        .isEqualTo(TYPED_INNER_4);
  }

  @Test
  public void parseClassBinaryName_withInnerClasses() throws Exception {
    String signature = "foo/Inner1$Inner2$Inner3$Inner4";
    assertThat(new SignatureParser(signature, INNER_CLASS_MAP).parseClassBinaryName())
        .isEqualTo(NON_TYPED_INNER_4);
  }

  private static SimpleType simpleReferenceType(String name, TypeArgument... typeArguments) {
    return SimpleType.builder()
        .setPrimitive(false)
        .setSimpleName(name)
        .setTypeArguments(ImmutableList.copyOf(typeArguments))
        .build();
  }

  private static TypeReference classBaz(TypeArgument typeArgument) {
    return classType(ImmutableList.of("foo"), "Baz")
        .setTypeArguments(ImmutableList.of(typeArgument))
        .build();
  }

  private static TypeReference.Builder classType(
      ImmutableList<String> packageName, String simpleName) {
    return TypeReference.formalizedBuilder()
        .setArray(false)
        .setPackageName(packageName)
        .setEnclosingClasses(ImmutableList.of())
        .setSimpleName(simpleName)
        .setPrimitive(false)
        .setTypeArguments(ImmutableList.of());
  }
}
