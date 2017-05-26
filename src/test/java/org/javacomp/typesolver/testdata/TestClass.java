package org.javacomp.typesolver.testdata;

import org.javacomp.typesolver.testdata.ondemand.*;
import org.javacomp.typesolver.testdata.other.BaseClass;
import org.javacomp.typesolver.testdata.other.Shadow;

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

  public FakeString returnFakeStringMethod() {}

  public class TestClassFactory extends BaseInnerClass implements BaseInterfaceFactory {}
}
