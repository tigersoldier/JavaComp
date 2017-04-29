package org.javacomp.completion.testdata;

public class CompleteInMethod {

  public class AboveClass {
    public final int aboveField;

    public void aboveMethod() {}
  }

  public void completeMethod() {
    AboveClass above = new AboveClass();
    /** @insert */
  }

  public class BelowClass {
    public final int belowField;

    public void belowMethod() {}
  }
}
