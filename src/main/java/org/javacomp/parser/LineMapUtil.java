package org.javacomp.parser;

import com.sun.source.tree.LineMap;

/** Utility methods for working with {@link LineMap}. */
public final class LineMapUtil {
  private LineMapUtil() {}

  public static int getPositionFromZeroBasedLineAndColumn(LineMap lineMap, int line, int column) {
    // LineMap accepts 1-based line.
    return (int)lineMap.getStartPosition(line+1)+column;
  }
}
