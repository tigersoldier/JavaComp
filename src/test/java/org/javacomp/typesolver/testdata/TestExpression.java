package org.javacomp.typesolver.testdata;

public class TestExpression<T> {
  public static final InnerA staticInnerA = new InnerA();

  public final InnerA innerA = new InnerA();
  public final TestClass testClass = new TestClass();

  public InnerB baseInnerB;

  public InnerC baseMethod();

  public InnerB baseMethod(int foo);

  public TestClass getTestClass();

  public LambdaStub lambdaCall(LambdaStub in);

  public T getT(T argument);

  public void method() {
    InnerA varA = new InnerA();
    InnerB varB = new InnerB();
  }

  public class InnerA<A extends InnerB> extends TestExpression<A> {
    public final InnerB innerB = new InnerB();
    public final InnerB[] innerBArray;

    public InnerA() {}

    public A typeParameterA;
    public A getTypeParameterA();

    public static <V extends InnerC> InnerA<V> create(V arg);

    public class InnerInnerA {}
  }

  public class InnerB {
    public final int intValue = 0;
    public final String stringValue = "";
    public final InnerC innerC = null;
  }

  public class InnerC extends InnerB {
    public final int innerCField;
  }

  public static interface LambdaStub {
    void lambdaMethod(String arg);
  }

  public enum InnerEnum {
    ENUM1,
    ENUM2,
    ;
    public int enumInstanceField;
  }
}
