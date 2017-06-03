package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.Module;
import org.javacomp.model.SolvedType;
import org.javacomp.model.TypeReference;
import org.javacomp.model.TypeVariable;
import org.javacomp.testing.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OverloadSolverTest {
  private static final String TEST_DIR = "src/test/java/org/javacomp/typesolver/testdata";
  private static final String TEST_FILE = "TestOverload.java";
  private static final String TOP_LEVEL_CLASS_FULL_NAME =
      "org.javacomp.typesolver.testdata.TestOverload";
  private static final String OVERLOAD_METHOD_NAME = "overloadMethod";
  private static final TypeReference FOO_TYPE = referenceType("Foo");
  private static final TypeReference FOO_ARRAY_TYPE = referenceArrayType("Foo");
  private static final TypeReference SUBFOO_TYPE = referenceType("Subfoo");
  private static final TypeReference LONG_TYPE = primitiveType("long");

  private final TypeSolver typeSolver = new TypeSolver();
  private final OverloadSolver overloadSolver = new OverloadSolver(typeSolver);

  private Module module;
  private ClassEntity topLevelClass;

  @Before
  public void setUpTestScope() throws Exception {
    module = TestUtil.parseFiles(TEST_DIR, TEST_FILE);
    topLevelClass = (ClassEntity) TestUtil.lookupEntity(TOP_LEVEL_CLASS_FULL_NAME, module);
  }

  @Test
  public void testSolveEmptyArguments() {
    assertThat(solveOverload()).isEqualTo("EmptyParameters");
  }

  @Test
  public void testExactMatch() {
    assertThat(solveOverload(FOO_TYPE)).isEqualTo("Foo");
    assertThat(solveOverload(FOO_TYPE, FOO_TYPE)).isEqualTo("FooFoo");
    assertThat(solveOverload(FOO_ARRAY_TYPE, FOO_TYPE)).isEqualTo("FooarrayFoo");
    assertThat(solveOverload(LONG_TYPE, LONG_TYPE)).isEqualTo("long_long");
    assertThat(solveOverload(LONG_TYPE, LONG_TYPE, FOO_TYPE)).isEqualTo("long_longFoo");
  }

  @Test
  public void testMatchSuperClass() {
    assertThat(solveOverload(SUBFOO_TYPE)).isEqualTo("Foo");
    assertThat(solveOverload(SUBFOO_TYPE, SUBFOO_TYPE)).isEqualTo("FooFoo");
    assertThat(solveOverload(FOO_ARRAY_TYPE, SUBFOO_TYPE)).isEqualTo("FooarrayFoo");
    assertThat(solveOverload(LONG_TYPE, LONG_TYPE)).isEqualTo("long_long");
    assertThat(solveOverload(LONG_TYPE, LONG_TYPE, SUBFOO_TYPE)).isEqualTo("long_longFoo");
  }

  @Test
  public void testWideningPrimitiveType() {
    assertThat(solveOverload(primitiveType("byte"), primitiveType("short"))).isEqualTo("long_long");
  }

  @Test
  public void testVariableArity() {
    assertThat(solveOverload(FOO_TYPE, FOO_TYPE, FOO_TYPE)).isEqualTo("FooVariableArity");
    assertThat(solveOverload(FOO_TYPE, LONG_TYPE)).isEqualTo("Foo_longFooVariableArity");
    assertThat(solveOverload(FOO_TYPE, LONG_TYPE, FOO_TYPE)).isEqualTo("Foo_longFooVariableArity");
    assertThat(solveOverload(FOO_TYPE, LONG_TYPE, FOO_TYPE, FOO_TYPE))
        .isEqualTo("Foo_longFooVariableArity");
    assertThat(solveOverload(FOO_TYPE, LONG_TYPE, FOO_TYPE, FOO_TYPE, FOO_TYPE))
        .isEqualTo("Foo_longFooVariableArity");
  }

  @Test
  public void testPassArrayToVariableArityArg() {
    assertThat(solveOverload(FOO_ARRAY_TYPE)).isEqualTo("FooVariableArity");
    assertThat(solveOverload(FOO_TYPE, LONG_TYPE, FOO_ARRAY_TYPE))
        .isEqualTo("Foo_longFooVariableArity");
  }

  @Test
  public void testMostSpecificMethod() {
    // All these invocations can match signature of
    // (long,long), (float,float), (float,double), (double,double)
    assertThat(solveOverload(primitiveType("byte"), primitiveType("byte"))).isEqualTo("long_long");
    assertThat(solveOverload(primitiveType("char"), primitiveType("char"))).isEqualTo("long_long");
    assertThat(solveOverload(primitiveType("short"), primitiveType("short")))
        .isEqualTo("long_long");
    assertThat(solveOverload(primitiveType("int"), primitiveType("int"))).isEqualTo("long_long");
    assertThat(solveOverload(primitiveType("long"), primitiveType("long"))).isEqualTo("long_long");

    // exact match is the most specific
    assertThat(solveOverload(primitiveType("float"), primitiveType("float")))
        .isEqualTo("float_float");
    assertThat(solveOverload(primitiveType("float"), primitiveType("double")))
        .isEqualTo("float_double");
    assertThat(solveOverload(primitiveType("double"), primitiveType("double")))
        .isEqualTo("double_double");

    // Two applicable methods: long_long_longVariableArity and long_long_long_floatVariableArity
    assertThat(solveOverload(LONG_TYPE, LONG_TYPE, LONG_TYPE))
        .isEqualTo("long_long_longVariableArity");
    assertThat(solveOverload(LONG_TYPE, LONG_TYPE, LONG_TYPE, LONG_TYPE))
        .isEqualTo("long_long_longVariableArity");
  }

  // TODO: add tests for auto-boxing when we have boxed type indexed.

  /**
   * @param argumentTypes type references of arguments to be passed to method invocation
   * @return the name of the return type of the solved method overload
   */
  private String solveOverload(TypeReference... argumentTypes) {
    @SuppressWarnings("unchecked")
    List<Entity> methods = typeSolver.findClassMethods(OVERLOAD_METHOD_NAME, topLevelClass, module);
    List<Optional<SolvedType>> solvedArgumentTypes = new ArrayList<>();
    for (TypeReference argumentType : argumentTypes) {
      solvedArgumentTypes.add(typeSolver.solve(argumentType, module, topLevelClass));
    }
    return overloadSolver
        .solve(methods, solvedArgumentTypes, module)
        .getReturnType()
        .getSimpleName();
  }

  private static TypeReference referenceType(String name) {
    return TypeReference.builder()
        .setFullName(name)
        .setArray(false)
        .setPrimitive(false)
        .setTypeVariables(ImmutableList.<TypeVariable>of())
        .build();
  }

  private static TypeReference referenceArrayType(String name) {
    return TypeReference.builder()
        .setFullName(name)
        .setArray(true)
        .setPrimitive(false)
        .setTypeVariables(ImmutableList.<TypeVariable>of())
        .build();
  }

  private static TypeReference primitiveType(String name) {
    return TypeReference.builder()
        .setFullName(name)
        .setArray(false)
        .setPrimitive(true)
        .setTypeVariables(ImmutableList.<TypeVariable>of())
        .build();
  }

  private static TypeReference primitiveArrayType(String name) {
    return TypeReference.builder()
        .setFullName(name)
        .setArray(true)
        .setPrimitive(true)
        .setTypeVariables(ImmutableList.<TypeVariable>of())
        .build();
  }
}
