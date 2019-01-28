package org.javacomp.completion.testdata;

public class OtherClass {
  public class InnerClass {
    public class InnerInnerClass {}

    public InnerInnerClass innerInnerClass;

    public InnerInnerClass getInnerInnerClass() {
      return null;
    }
  }

  public InnerClass innerClass;

  public InnerClass getInnerClass() {
    return null;
  }

  public static void staticMethod() {}
  public static void staticMethod(int value) {}

  public static final String STATIC_FIELD = "foo";
}
