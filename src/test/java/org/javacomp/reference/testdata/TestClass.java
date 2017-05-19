package org.javacomp.reference.testdata;

import org.javacomp.reference.testdata.other.OtherPackageClass;

public class TestClass {
  public class InnerClassA {
    public InnerClassB getTestBInA() {}

    public InnerClassB testBInA;

    public TestClass getTestClassInA() {}

    public TestClass testClassInA;
  }

  public class InnerClassB {
    public InnerClassA getTestAInB() {}

    public InnerClassA testAInB;

    public TestClass getTestClassInB() {}

    public TestClass testClassInB;
  }

  public InnerClassA getInnerA() {}

  public InnerClassA innerA;

  public InnerClassB innerB;

  public OtherClass getOtherClass() {}

  public OtherClass otherClass;
  public OtherClass OtherClass;

  public OtherPackageClass getOtherPackageClass() {}

  public OtherPackageClass otherPackageClass;

  public void methodWithArgs(InnerClassA innerClassA, OtherClass innerClassB) {}

  public void testMethod(InnerClassA innerAParam, OtherClass otherClassParam) {
    InnerClassA innerAVar = innerAParam.getTestBInA().testAInB.getTestClassInA().innerA;
    OtherClass.InnerClassA otherInnerAVar = otherClassParam.getTestClass().otherClass.getInnerA();
    OtherPackageClass otherPackageInnerAVar = innerAVar.testClassInA.getOtherPackageClass().innerA;
    InnerClassB ignore = innerB;

    getInnerA().testBInA;
    getOtherClass().getTestClass();
    methodWithArgs(innerA, innerAParam.testClassInA);
  }
}
