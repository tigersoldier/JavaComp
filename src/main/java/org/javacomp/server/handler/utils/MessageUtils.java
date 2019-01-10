package org.javacomp.server.handler.utils;

import static com.google.common.base.Preconditions.checkState;

import com.sun.source.tree.LineMap;
import java.nio.file.Paths;
import org.javacomp.file.EditHistory.AppliedEdit;
import org.javacomp.model.FileScope;
import org.javacomp.protocol.Location;
import org.javacomp.protocol.Position;
import org.javacomp.protocol.Range;
import org.javacomp.protocol.TextEdit;

/** Utility functions for building protocol messages. */
public final class MessageUtils {
  private MessageUtils() {}

  /**
   * Builds a {@link Location} message for a range in a file.
   *
   * @param fileScope which file the location is pointed to
   * @param range 0-based offset range in the file. The range must be bounded
   */
  public static Location buildLocationForFile(
      FileScope fileScope, com.google.common.collect.Range<Integer> range) {
    Location location = new Location();
    location.range = buildRangeForFile(fileScope, range);
    location.uri = Paths.get(fileScope.getFilename()).toUri();

    return location;
  }

  /**
   * Builds a {@link Range} message for a range in a file.
   *
   * @param fileScope which file the range is in
   * @param range 0-based offset range in the file. The range must be bounded
   */
  public static Range buildRangeForFile(
      FileScope fileScope, com.google.common.collect.Range<Integer> range) {
    return new Range(
        /* start= */ buildPositionForFile(fileScope, range.lowerEndpoint()),
        /* end= */ buildPositionForFile(fileScope, range.upperEndpoint()));
  }

  /**
   * Builds a {@link Position} message for an offset in a file.
   *
   * @param fileScope which file the range is in
   * @param offset 0-based offset in the file
   */
  public static Position buildPositionForFile(FileScope fileScope, int offset) {
    checkState(
        fileScope.getLineMap().isPresent(),
        "File %s doesn't have line map",
        fileScope.getFilename());
    LineMap lineMap = fileScope.getLineMap().get();

    return new Position(
        (int) lineMap.getLineNumber(offset) - 1, (int) lineMap.getColumnNumber(offset) - 1);
  }

  public static TextEdit buildTextEdit(AppliedEdit appliedEdit) {
    return new TextEdit(
        Range.createFromTextRange(appliedEdit.getTextRange()), appliedEdit.getNewText());
  }
}
