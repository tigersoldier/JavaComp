package org.javacomp.completion;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Records the parameters and results of a completion request. */
@AutoValue
public abstract class CachedCompletion {

  /** A special {@link CachedCompletion} that should match none of the completion requests. */
  public static final CachedCompletion NONE =
      CachedCompletion.builder()
          .setFilePath(Paths.get(""))
          .setLine(-1)
          .setColumn(-1)
          .setPrefix("\n")
          .setCompletionCandidates(ImmutableList.of())
          .build();

  public abstract Path getFilePath();

  public abstract int getLine();

  public abstract int getColumn();

  public abstract String getPrefix();

  public abstract ImmutableList<CompletionCandidate> getCompletionCandidates();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_CachedCompletion.Builder();
  }

  /**
   * Check if the completor is processing a completion request that is an incremental completion of
   * the cached completion.
   */
  public boolean isIncrementalCompletion(Path filePath, int line, int column, String prefix) {
    if (!getFilePath().equals(filePath)) {
      return false;
    }
    if (getLine() != line) {
      return false;
    }
    if (getColumn() > column) {
      return false;
    }
    if (!prefix.startsWith(getPrefix())) {
      return false;
    }
    if (prefix.length() - getPrefix().length() != column - getColumn()) {
      // FIXME: This may break for complicated Unicodes.
      return false;
    }
    return true;
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setFilePath(Path filePath);

    public abstract Builder setLine(int line);

    public abstract Builder setColumn(int column);

    public abstract Builder setPrefix(String prefix);

    public abstract Builder setCompletionCandidates(
        ImmutableList<CompletionCandidate> completionCandidates);

    public abstract CachedCompletion build();
  }
}
