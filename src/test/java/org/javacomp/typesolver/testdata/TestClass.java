package org.javacomp.typesolver.testdata;

import org.javacomp.typesolver.testdata.other.BaseClass;

public class TestClass extends BaseClass implements BaseInterface {
  public static TestClassFactory newFactory() {
    return new TestClassFactory() {};
  }

  public class TestClassFactory extends BaseInnerClass implements BaseInterfaceFactory {}
}
