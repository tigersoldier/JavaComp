package org.javacomp.completion.testdata;

public class CompleteInMethod {
  public CompleteInMethod self = new CompleteInMethod();
  public FakeString fakeString;

  public class AboveClass {
    public static final int STATIC_ABOVE_FIELD;
    public final int aboveField;

    public static void staticAboveMethod() {}
    public void aboveMethod() {}
  }

  public void completeMethod() {
    AboveClass above = new AboveClass();
    BelowClass below = new BelowClass();
    /** @insert */
  }

  public class BelowClass {
    public static final int STATIC_BELOW_FIELD;
    public final int belowField;

    public static void staticBelowMethod() {}
    public void belowMethod() {}
  }
}
