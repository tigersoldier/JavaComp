package org.javacomp.completion.testdata;

public class Parameterized<T> {
  public static class NonParameterized {
    public int nonParameterizedField;
  }

  public T typeParameterT;

  public T getTypeParameterT();

  public BoundParameterized boundParameterized;

  public Parameterized<NonParameterized> parameterizedOfNonParameterized;

  public static class BoundParameterized extends Parameterized<NonParameterized> {}

  public void foo() {
    /** @insert */
  }
}
