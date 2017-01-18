package org.javacomp.model;

/** The actual type solved for {@link TypeReference}. */
public class SolvedType {
  private final ClassSymbol classSymbol;

  public SolvedType(ClassSymbol classSymbol) {
    this.classSymbol = classSymbol;
  }

  public ClassSymbol getClassSymbol() {
    return classSymbol;
  }
}
