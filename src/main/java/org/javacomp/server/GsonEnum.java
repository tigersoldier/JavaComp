package org.javacomp.server;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Annotation for serializing enums to JSON. */
@Retention(RUNTIME)
@Target(TYPE)
public @interface GsonEnum {
  /** How enum values should be serialized and deserialized. */
  public enum SerializeType {
    /** Values are serialized or deserialized using their enum ordinal values. */
    ORDINAL,
    /** Values are serialized or deserialized using their lowercased names. */
    LOWERCASE_NAME,
  }

  SerializeType value();
}
