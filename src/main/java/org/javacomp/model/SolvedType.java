package org.javacomp.model;

/** The actual type solved for {@link TypeReference}. */
public class SolvedType {
  private final ClassEntity classEntity;

  public SolvedType(ClassEntity classEntity) {
    this.classEntity = classEntity;
  }

  public ClassEntity getClassEntity() {
    return classEntity;
  }
}
