package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.truth.Truth8;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.SolvedEntityType;
import org.javacomp.model.SolvedReferenceType;
import org.javacomp.model.SolvedType;
import org.javacomp.model.SolvedTypeParameters;
import org.javacomp.model.TypeReference;
import org.javacomp.testing.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TypeSolverTest {
  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/typesolver/testdata/";
  private static final String[] TEST_FILES = {
    "BaseInterface.java", "TestClass.java",
  };
  private static final String[] OTHER_FILES = {
    "other/BaseClass.java", "other/Shadow.java",
  };

  private static final String[] ON_DEMAND_FILES = {
    "ondemand/OnDemand.java", "ondemand/Shadow.java",
  };

  private static final String[] FAKE_JDK_FILES = {"fakejdk/String.java"};

  private static final String TEST_DATA_PACKAGE = "org.javacomp.typesolver.testdata";
  private static final String TEST_DATA_OTHER_PACKAGE = TEST_DATA_PACKAGE + ".other";
  private static final String TEST_DATA_ONDEMAND_PACKAGE = TEST_DATA_PACKAGE + ".ondemand";
  private static final String TEST_CLASS_FULL_NAME = TEST_DATA_PACKAGE + ".TestClass";
  private static final String TEST_CLASS_FACTORY_FULL_NAME =
      TEST_CLASS_FULL_NAME + ".TestClassFactory";
  private static final String PARAMETERIZED_TYPE_FULL_NAME =
      TEST_CLASS_FULL_NAME + ".ParameterizedType";
  private static final String BASE_INTERFACE_FULL_NAME = TEST_DATA_PACKAGE + ".BaseInterface";
  private static final String BASE_INTERFACE_FACTORY_FULL_NAME =
      BASE_INTERFACE_FULL_NAME + ".BaseInterfaceFactory";
  private static final String BASE_CLASS_FULL_NAME = TEST_DATA_OTHER_PACKAGE + ".BaseClass";
  private static final String OTHER_SHADOW_CLASS_FULL_NAME = TEST_DATA_OTHER_PACKAGE + ".Shadow";
  private static final String BASE_INNER_CLASS_FULL_NAME = BASE_CLASS_FULL_NAME + ".BaseInnerClass";
  private static final String ON_DEMAND_CLASS_FULL_NAME = TEST_DATA_ONDEMAND_PACKAGE + ".OnDemand";
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");

  private final TypeSolver typeSolver = new TypeSolver();

  private Module testModule;
  private Module otherModule;
  private Module onDemandModule;
  private Module fakeJdkModule;

  @Before
  public void setUpTestScope() throws Exception {
    testModule = TestUtil.parseFiles(TEST_DATA_DIR, TEST_FILES);
    otherModule = TestUtil.parseFiles(TEST_DATA_DIR, OTHER_FILES);
    onDemandModule = TestUtil.parseFiles(TEST_DATA_DIR, ON_DEMAND_FILES);
    fakeJdkModule = TestUtil.parseFiles(TEST_DATA_DIR, FAKE_JDK_FILES);
    testModule.addDependingModule(otherModule);
    testModule.addDependingModule(onDemandModule);
    testModule.addDependingModule(fakeJdkModule);
  }

  @Test
  public void solveBaseInterfaceInTheSamePackage() {
    ClassEntity testClass = (ClassEntity) TestUtil.lookupEntity(TEST_CLASS_FULL_NAME, testModule);
    TypeReference baseInterfaceReference = testClass.getInterfaces().get(0);
    Optional<SolvedEntityType> solvedType =
        solveEntityType(baseInterfaceReference, testModule, testClass);
    Truth8.assertThat(solvedType).named("Base interface of " + TEST_CLASS_FULL_NAME).isPresent();
    assertThat(solvedType.get().getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_INTERFACE_FULL_NAME, testModule));
  }

  @Test
  public void solveClassDefinedInSuperInterface() {
    ClassEntity testClass =
        (ClassEntity) TestUtil.lookupEntity(TEST_CLASS_FACTORY_FULL_NAME, testModule);
    TypeReference baseInterfaceReference = testClass.getInterfaces().get(0);
    Optional<SolvedEntityType> solvedType =
        solveEntityType(baseInterfaceReference, testModule, testClass);
    Truth8.assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_INTERFACE_FACTORY_FULL_NAME, testModule));
  }

  @Test
  public void solveInnerClass() {
    SolvedEntityType baseInterface = solveMethodReturnType(TEST_CLASS_FULL_NAME + ".newFactory");
    assertThat(baseInterface.getEntity())
        .isSameAs(TestUtil.lookupEntity(TEST_CLASS_FACTORY_FULL_NAME, testModule));
  }

  @Test
  public void solveBaseClassInOtherPackage() {
    ClassEntity testClass = (ClassEntity) TestUtil.lookupEntity(TEST_CLASS_FULL_NAME, testModule);
    TypeReference baseClassReference = testClass.getSuperClass().get();
    Optional<SolvedEntityType> solvedType =
        solveEntityType(baseClassReference, testModule, testClass);
    Truth8.assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_CLASS_FULL_NAME, otherModule));
  }

  @Test
  public void solveInnerClassInBaseClassFromOtherPackage() {
    ClassEntity testClass =
        (ClassEntity) TestUtil.lookupEntity(TEST_CLASS_FACTORY_FULL_NAME, testModule);
    TypeReference baseInnerClassReference = testClass.getSuperClass().get();
    Optional<SolvedEntityType> solvedType =
        solveEntityType(baseInnerClassReference, testModule, testClass);
    Truth8.assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_INNER_CLASS_FULL_NAME, otherModule));
  }

  @Test
  public void solveOnDemandClassImport() {
    SolvedEntityType onDemandClass = solveMethodReturnType(TEST_CLASS_FULL_NAME + ".getOnDemand");
    assertThat(onDemandClass.getEntity())
        .isSameAs(TestUtil.lookupEntity(ON_DEMAND_CLASS_FULL_NAME, onDemandModule));
  }

  @Test
  public void onDemandPackageClassShouldBeShadowed() {
    // both other and ondemand package define Shadow class. Shadow from other package should be used
    // since it's explicitly imported.
    SolvedEntityType shadowClass = solveMethodReturnType(TEST_CLASS_FULL_NAME + ".getShadow");
    assertThat(shadowClass.getEntity())
        .isSameAs(TestUtil.lookupEntity(OTHER_SHADOW_CLASS_FULL_NAME, otherModule));
  }

  @Test
  public void solveFullyQualifiedType() {
    SolvedEntityType fullyQualifiedBaseClass =
        solveMethodReturnType(TEST_CLASS_FULL_NAME + ".getFullyQualifiedBaseClass");
    assertThat(fullyQualifiedBaseClass.getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_CLASS_FULL_NAME, otherModule));
  }

  @Test
  public void solveNonExistentType() {
    // Should not throw exceptions.
    assertNotSolved(TEST_CLASS_FULL_NAME + ".returnTypeNotExistMethod");
    assertNotSolved(TEST_CLASS_FULL_NAME + ".returnInnerTypeNotExistMethod");
    assertNotSolved(TEST_CLASS_FULL_NAME + ".returnTypePackageNotExistMethod");
    assertNotSolved(TEST_CLASS_FULL_NAME + ".returnTypeRootPackageNotExistMethod");
  }

  @Test
  public void solveJavaLangType() {
    SolvedEntityType fakeStringClass =
        solveMethodReturnType(TEST_CLASS_FULL_NAME + ".returnStringMethod");
    assertThat(fakeStringClass.getEntity())
        .isSameAs(TestUtil.lookupEntity("java.lang.String", fakeJdkModule));
  }

  @Test
  public void solveTypeParametersInScope() {
    SolvedEntityType typeParameterB = solveMethodReturnType(PARAMETERIZED_TYPE_FULL_NAME + ".getB");
    assertThat(typeParameterB.getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_CLASS_FULL_NAME + ".BaseInnerClass", otherModule));

    SolvedEntityType typeParameterC = solveMethodReturnType(PARAMETERIZED_TYPE_FULL_NAME + ".getC");
    assertThat(typeParameterC.getEntity())
        .isSameAs(TestUtil.lookupEntity(BASE_CLASS_FULL_NAME + ".BaseInnerClass", otherModule));
  }

  @Test
  public void solveTypeParametersWithTypeArguments() {
    SolvedEntityType parameterizedType =
        solveMethodReturnType(TEST_CLASS_FULL_NAME + ".getParameterizedType");
    assertThat(parameterizedType.getEntity())
        .isSameAs(TestUtil.lookupEntity(TEST_CLASS_FULL_NAME + ".ParameterizedType", testModule));

    assertThat(parameterizedType).isInstanceOf(SolvedReferenceType.class);
    SolvedTypeParameters solvedTypeParameters =
        ((SolvedReferenceType) parameterizedType).getTypeParameters();
    Optional<SolvedType> typeParameterB = solvedTypeParameters.getTypeParameter("B");
    Truth8.assertThat(typeParameterB)
        .named("Type parameter B of getParameterizedType()")
        .isPresent();
    assertThat(typeParameterB.get())
        .named("Type parameter B of getParameterizedType()")
        .isInstanceOf(SolvedEntityType.class);
    assertThat(((SolvedEntityType) typeParameterB.get()).getEntity())
        .named("Type parameter B of getParameterizedType()")
        .isSameAs(TestUtil.lookupEntity(TEST_CLASS_FACTORY_FULL_NAME, testModule));
  }

  private Optional<SolvedEntityType> solveEntityType(
      TypeReference typeReference, Module module, EntityScope parentScope) {
    return typeSolver
        .solve(typeReference, parentScope, module)
        .map(t -> (t instanceof SolvedEntityType) ? (SolvedEntityType) t : null);
  }

  private SolvedEntityType solveMethodReturnType(String qualifiedMethodName) {
    MethodEntity method = (MethodEntity) TestUtil.lookupEntity(qualifiedMethodName, testModule);
    assertThat(method).named(qualifiedMethodName).isNotNull();
    TypeReference methodReturnType = method.getReturnType();
    Optional<SolvedEntityType> solvedType =
        solveEntityType(methodReturnType, testModule, method.getChildScope());
    Truth8.assertThat(solvedType).named(methodReturnType.toString()).isPresent();
    return solvedType.get();
  }

  private void assertNotSolved(String qualifiedMethodName) {
    MethodEntity method = (MethodEntity) TestUtil.lookupEntity(qualifiedMethodName, testModule);
    assertThat(method).named(qualifiedMethodName).isNotNull();
    TypeReference methodReturnType = method.getReturnType();
    Optional<SolvedEntityType> solvedType =
        solveEntityType(
            methodReturnType, testModule, method.getChildScope().getParentScope().get());
    Truth8.assertThat(solvedType).named(methodReturnType.toString()).isEmpty();
  }
}
