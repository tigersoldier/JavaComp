package org.javacomp.reference.testdata;

public class TestReferenceClass2 {
  public int publicField;
  protected bool protectedField;
  long packagePrivateField;
  private short privateField;

  public TestReferenceClass(int publicConstructor) {
  }

  private void privateMethod() {
  }

  public void publicMethod() {
  }

  public void withLocalVaraiable() {
    String local;
    int localRedefined;
    bool methodParameter;
    if (true) {
      String localRedefined;
      int scopeRedefined;
      scopeRedefined = 2;
      localRedefined = "" + scopeRedefined;
    }

    localRedefined = 3;
    local = "local";

    for (int scopeRedefined : someList) {
      long localRedefined = scopeRedefined;
    }

    for (int scopeRedefined = 0; scopeRedefined < 10; scopeRedefined++) {
      long forLoopLocal = scopeRedefined;
    }

    {
      int scoped = 4;
      scoped += localRedefined;
      bool scopeRedefined = true;
    }
  }

  private int withMethodParameter(int param1, String methodParameter) {
    privateMethod();
    publicMethod();
    return param1 + methodParameter.length();
  }

  private void withClassMember() {
    privateMethod();
    publicMethod();
    this.publicField = 1;
    this.privateField = 2;
    new TestReferenceClass().publicMethod();
    new TestReferenceClass().publicField = 1;
  }
}
