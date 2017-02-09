package org.javacomp.typesolver.testdata;

public class TestOverload {
  public class Foo {}

  public class Subfoo extends Foo {}

  public EmptyParameters overloadMethod() {}

  public Foo overloadMethod(Foo foo1) {}

  public FooFoo overloadMethod(Foo foo1, Foo foo2) {}

  public FooarrayFoo overloadMethod(Foo[] fooArray1, Foo foo2) {}

  public long_long overloadMethod(long long1, long long2) {}

  public long_long_longVariableArity overloadMethod(long long1, long long2, long... longs) {}

  public long_long_long_floatVariableArity overloadMethod(
      long long1, long long2, long long3, float... floats) {}

  public float_float overloadMethod(float float1, float float2) {}

  public float_double overloadMethod(float flaot1, double double1) {}

  public double_double overloadMethod(double double1, double double2) {}

  public long_longFoo overloadMethod(long long1, long long2, Foo foo) {}

  public FooVariableArity overloadMethod(Foo... foos) {}

  public Foo_longFooVariableArity overloadMethod(Foo foo1, long long1, Foo... foos) {}
}
