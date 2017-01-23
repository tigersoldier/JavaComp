package org.javacomp.model;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A reference to a type for lazy resolution. */
public class TypeReference {
  public static final TypeReference VOID_TYPE = new TypeReference();

  private static final Joiner JOINER = Joiner.on(".");

  private final ImmutableList<String> fullName;

  private Entity resolvedEntity = null;

  public TypeReference(String... fullName) {
    this(ImmutableList.copyOf(fullName));
  }

  public TypeReference(List<String> fullName) {
    this.fullName = ImmutableList.copyOf(fullName);
  }

  public void setResolvedEntity(Entity resolvedEntity) {
    this.resolvedEntity = resolvedEntity;
  }

  public String getSimpleName() {
    return fullName.get(fullName.size() - 1);
  }

  public List<String> getFullName() {
    return fullName;
  }

  @Override
  public String toString() {
    return "TypeReference<" + JOINER.join(fullName) + ">";
  }
}
