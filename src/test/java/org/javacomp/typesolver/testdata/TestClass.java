package org.javacomp.typesolver.testdata;

import org.javacomp.typesolver.testdata.ondemand.*;
import org.javacomp.typesolver.testdata.other.BaseClass;
import org.javacomp.typesolver.testdata.other.Shadow;

public class TestClass extends BaseClass implements BaseInterface {
  public static TestClassFactory newFactory() {
    return new TestClassFactory() {};
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

  public class TestClassFactory extends BaseInnerClass implements BaseInterfaceFactory {}
}
