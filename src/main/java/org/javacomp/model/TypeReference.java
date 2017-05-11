package org.javacomp.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Collection;

/** A reference to a type for lazy resolution. */
@AutoValue
public abstract class TypeReference {
  public static final TypeReference EMPTY_TYPE =
      TypeReference.builder().setFullName().setPrimitive(false).setArray(false).build();

  private static final Joiner JOINER = Joiner.on(".");

  public abstract ImmutableList<String> getFullName();

  public abstract boolean isPrimitive();

  public abstract boolean isArray();

  public static Builder builder() {
    return new AutoValue_TypeReference.Builder();
  }

  public String getSimpleName() {
    ImmutableList<String> fullName = getFullName();
    if (fullName.isEmpty()) {
      return "";
    }
    return fullName.get(fullName.size() - 1);
  }

  @Override
  public String toString() {
    return "TypeReference<" + JOINER.join(getFullName()) + (isArray() ? "[]>" : ">");
  }

  public String toSimpleString() {
    return getSimpleName();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setFullName(ImmutableList<String> fullName);

    public abstract Builder setPrimitive(boolean isPrimitive);

    public abstract Builder setArray(boolean isArray);

    public abstract TypeReference build();

    public Builder setFullName(String... fullName) {
      return setFullName(ImmutableList.copyOf(fullName));
    }

    public Builder setFullName(Collection<String> fullName) {
      return setFullName(ImmutableList.copyOf(fullName));
    }
  }
}
