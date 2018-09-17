package org.javacomp.reference.testdata;

public class TestReferenceClass {
  public TestReferenceClass(int publicConstructor) {
  }

  public void withLocalVaraiable() {
    String local;
    int localRedefined;
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
}
