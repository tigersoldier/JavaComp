package org.javacomp.model;

/** The actual type solved for {@link TypeReference}. */
public interface SolvedType {
  /** Converts to type reference. */
  TypeReference toTypeReference();
}
