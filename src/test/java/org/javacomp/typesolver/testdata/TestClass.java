package org.javacomp.typesolver.testdata;

import org.javacomp.typesolver.testdata.ondemand.*;
import org.javacomp.typesolver.testdata.other.BaseClass;
import org.javacomp.typesolver.testdata.other.Shadow;

// The invalid imports below tests canonical names for import statements.
//
// If resolving import paths allows inner classes defined in super classes or
// interfaces, the import statements will cause infinit loop for solving
// BaseInterface. The root cause is when trying to find BaseInterface from
// imported classes, the type solver tries to find inner classes of TestClass
// according the import statements below. If inner classes defined in super
// classes or interfaces are allowed, BaseInterface needs to be solved for
// getting all inner classes, which in turn triggers solving BaseInterface
// from the import statements again.
import org.javacomp.typesolver.testdata.TestClass.BaseInterface;
import org.javacomp.typesolver.testdata.TestClass.*;
import org.javacomp.typesolver.testdata.TestClass.BaseInterfaceFactory.*;

public class TestClass extends BaseClass implements BaseInterface {
  public static final TestClassFactory FACTORY = new TestClassFactory();
  public final Shadow shadow;

  public static TestClassFactory newFactory() {
    return FACTORY;
  }

  public OnDemand getOnDemand() {
    return new OnDemand();
  }

  public Shadow getShadow() {
    return new Shadow();
  }

  public org.javacomp.typesolver.testdata.other.BaseClass getFullyQualifiedBaseClass() {
    return this;
  }

  public NotExist returnTypeNotExistMethod() {}

  public org.doesnotexist.Type returnTypePackageNotExistMethod() {}

  public doesnotexist.Type returnTypeRootPackageNotExistMethod() {}

  public org.javacomp.typesolver.testdata.TestClass.NotExist returnInnerTypeNotExistMethod() {}

  public String returnStringMethod() {}

  public class TestClassFactory extends BaseInnerClass implements BaseInterfaceFactory {}

  public final ParameterizedType<String, ? extends TestClassFactory> getParameterizedType();

  public static class ParameterizedType<A, B extends BaseInnerClass> {
    public A getA();
    public B getB();
    public <C extends B & A> C getC() {}
    public static <D> D staticMethod() {
    }

    public class InnerClass<E> {}
    public static class StaticInnerClass<F> {}
  }

  public static class RecursiveParameterizedType<N extends RecursiveParameterizedType> {}
}
