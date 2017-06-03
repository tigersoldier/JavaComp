package org.javacomp.model;

/**
 * Type variable applying to generic type references.
 *
 * <p>Example:
 *
 * <pre>{@code
 * SomeType<TypeVar1, ? extends SomeType2, ? super SompType3>
 * }</pre>
 */
public interface TypeVariable {
  String toDisplayString();
}
