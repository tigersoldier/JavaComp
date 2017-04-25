package org.javacomp.completion.testdata;

public class CompleteNewStatement {

  public void completeMember() {
    SubClass subclass = new SubClass();
    subclass./** @complete */
    subclass.field = 0;
  }

  public class SubClass {
    public final int field;
    public void method() {}
  }
}
