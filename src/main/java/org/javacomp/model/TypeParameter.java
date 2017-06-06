package org.javacomp.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Type parameter for parameterized classe and method declarations. */
@AutoValue
public abstract class TypeParameter {
  public abstract String getName();

  public abstract ImmutableList<TypeReference> getExtendBounds();

  public static TypeParameter create(String name, List<TypeReference> extendBounds) {
    return new AutoValue_TypeParameter(name, ImmutableList.copyOf(extendBounds));
  }
}
