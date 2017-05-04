package org.javacomp.completion.testdata;

public class CompleteInMethod {
  public CompleteInMethod self = new CompleteInMethod();

  public class AboveClass {
    public final int aboveField;

    public void aboveMethod() {}
  }

  public void completeMethod() {
    AboveClass above = new AboveClass();
    BelowClass below = new BelowClass();
    /** @insert */
  }

  public class BelowClass {
    public final int belowField;

    public void belowMethod() {}
  }
}
