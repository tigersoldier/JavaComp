package org.javacomp.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** Represents primitive types. */
public class PrimitiveEntity extends Entity {
  public static final PrimitiveEntity BYTE = new PrimitiveEntity("byte");
  public static final PrimitiveEntity SHORT = new PrimitiveEntity("short");
  public static final PrimitiveEntity INT = new PrimitiveEntity("int");
  public static final PrimitiveEntity LONG = new PrimitiveEntity("long");
  public static final PrimitiveEntity FLOAT = new PrimitiveEntity("float");
  public static final PrimitiveEntity DOUBLE = new PrimitiveEntity("double");
  public static final PrimitiveEntity CHAR = new PrimitiveEntity("char");
  public static final PrimitiveEntity BOOLEAN = new PrimitiveEntity("boolean");

  private static final Map<String, PrimitiveEntity> TYPE_MAP =
      new ImmutableMap.Builder<String, PrimitiveEntity>()
          .put(BYTE.getSimpleName(), BYTE)
          .put(SHORT.getSimpleName(), SHORT)
          .put(INT.getSimpleName(), INT)
          .put(LONG.getSimpleName(), LONG)
          .put(FLOAT.getSimpleName(), FLOAT)
          .put(DOUBLE.getSimpleName(), DOUBLE)
          .put(CHAR.getSimpleName(), CHAR)
          .put(BOOLEAN.getSimpleName(), BOOLEAN)
          .build();

  public static PrimitiveEntity get(String simpleName) {
    if (!TYPE_MAP.containsKey(simpleName)) {
      // How can this happen?
      return new PrimitiveEntity(simpleName);
    }
    return TYPE_MAP.get(simpleName);
  }

  private PrimitiveEntity(String simpleName) {
    super(simpleName, Entity.Kind.PRIMITIVE, ImmutableList.<String>of() /* qualifiers */);
  }

  @Override
  public EmptyScope getChildScope() {
    return EmptyScope.INSTANCE;
  }

  @Override
  public String toString() {
    return "PrimitiveEntity: " + getSimpleName();
  }
}
