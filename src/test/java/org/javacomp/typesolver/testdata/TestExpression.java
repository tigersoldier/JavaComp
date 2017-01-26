package org.javacomp.typesolver.testdata;

public class TestExpression {
  public final InnerA innerA = new InnerA();

  public InnerB baseInnerB;

  public InnerC baseMethod();

  public class InnerA extends TestExpression {
    public final InnerB innerB = new InnerB();
  }

  public class InnerB {
    public final int intValue = 0;
    public final String stringValue = "";
    public final InnerC innerC = null;
  }

  public class InnerC {}
}
