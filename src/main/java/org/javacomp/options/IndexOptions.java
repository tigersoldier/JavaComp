package org.javacomp.options;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Options on how a Java file should be indexed. */
@AutoValue
public abstract class IndexOptions {
  /** Indexes everything possible. */
  public static final IndexOptions.Builder FULL_INDEX_BUILDER =
      IndexOptions.builder().setShouldIndexMethodContent(true).setShouldIndexNonPublic(true);
  /** Indexes only public classes/methods/etc... without indexing the contents. */
  public static final IndexOptions.Builder PUBLIC_READONLY_BUILDER =
      IndexOptions.builder().setShouldIndexNonPublic(false).setShouldIndexMethodContent(false);

  public abstract boolean shouldIndexNonPublic();

  public abstract boolean shouldIndexMethodContent();

  public abstract ImmutableList<String> ignorePaths();

  public static Builder builder() {
    return new AutoValue_IndexOptions.Builder().setIgnorePaths(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract IndexOptions build();

    public abstract Builder setShouldIndexNonPublic(boolean value);

    public abstract Builder setShouldIndexMethodContent(boolean value);

    public Builder setIgnorePaths(List<String> ignorePaths) {
      return setIgnorePaths(ImmutableList.copyOf(ignorePaths));
    }

    public abstract Builder setIgnorePaths(ImmutableList<String> ignorePaths);
  }
}
