package org.javacomp.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.LineMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AdjustedLineMapTest {
  private String originalContent;
  private SimpleLineMap originalLineMap;
  private List<Insertion> insertions;
  private AdjustedLineMap adjustedLineMap;

  @Before
  public void setUp() {
    originalContent =
        "\n"
            + "insertStart\n"
            + "insertMiddle\n"
            + "notChanged\n"
            + "insertEnd\n"
            + "insertAll\n"
            + "\n"
            + "FIN\n"
            + "\n"
            + "EOF";
    originalLineMap = new SimpleLineMap(originalContent);
    ImmutableList<Insertion> insertions =
        ImmutableList.of(
            createInsertion(1, 1, "123"),
            createInsertion(2, 1, "1234"),
            createInsertion(3, 5, "12345"),
            createInsertion(5, 10, "123456"),
            createInsertion(6, 1, "12"),
            createInsertion(6, 7, "12"),
            createInsertion(6, 10, "12"),
            createInsertion(8, 1, "1"),
            createInsertion(8, 2, "1"),
            createInsertion(8, 3, "1"));
    adjustedLineMap =
        new AdjustedLineMap.Builder()
            .setOriginalLineMap(originalLineMap)
            .addInsertions(insertions)
            .build();
    assertThat(Insertion.applyInsertions(originalContent, insertions).toString())
        .isEqualTo(
            "123\n"
                + "1234insertStart\n"
                + "inse12345rtMiddle\n"
                + "notChanged\n"
                + "insertEnd123456\n"
                + "12insert12All12\n"
                + "\n"
                + "1F1I1N\n"
                + "\n"
                + "EOF");
  }

  private Insertion createInsertion(long line, long column, String content) {
    long pos = originalLineMap.getPosition(line, column);
    return Insertion.create((int) pos, content);
  }

  @Test
  public void testGetStartPosition() {
    assertThat(adjustedLineMap.getStartPosition(1)).isEqualTo(0);
    assertThat(adjustedLineMap.getStartPosition(2)).isEqualTo(4);
    assertThat(adjustedLineMap.getStartPosition(3)).isEqualTo(20);
    assertThat(adjustedLineMap.getStartPosition(4)).isEqualTo(38);
    assertThat(adjustedLineMap.getStartPosition(5)).isEqualTo(49);
    assertThat(adjustedLineMap.getStartPosition(6)).isEqualTo(65);
    assertThat(adjustedLineMap.getStartPosition(7)).isEqualTo(81);
    assertThat(adjustedLineMap.getStartPosition(8)).isEqualTo(82);
    assertThat(adjustedLineMap.getStartPosition(9)).isEqualTo(89);
    assertThat(adjustedLineMap.getStartPosition(10)).isEqualTo(90);
  }

  @Test
  public void testGetPosition() {
    assertThat(adjustedLineMap.getPosition(1, 1)).isEqualTo(0);
    assertThat(adjustedLineMap.getPosition(1, 2)).isEqualTo(4);
    assertThat(adjustedLineMap.getPosition(2, 1)).isEqualTo(4);
    assertThat(adjustedLineMap.getPosition(2, 2)).isEqualTo(9);
    assertThat(adjustedLineMap.getPosition(3, 5)).isEqualTo(24);
    assertThat(adjustedLineMap.getPosition(3, 6)).isEqualTo(30);
    assertThat(adjustedLineMap.getPosition(5, 10)).isEqualTo(58);
    assertThat(adjustedLineMap.getPosition(5, 11)).isEqualTo(65);
    assertThat(adjustedLineMap.getPosition(6, 1)).isEqualTo(65);
    assertThat(adjustedLineMap.getPosition(6, 2)).isEqualTo(68);
    assertThat(adjustedLineMap.getPosition(6, 7)).isEqualTo(73);
    assertThat(adjustedLineMap.getPosition(6, 8)).isEqualTo(76);
    assertThat(adjustedLineMap.getPosition(6, 10)).isEqualTo(78);
    assertThat(adjustedLineMap.getPosition(6, 11)).isEqualTo(81);
    assertThat(adjustedLineMap.getPosition(8, 1)).isEqualTo(82);
    assertThat(adjustedLineMap.getPosition(8, 2)).isEqualTo(84);
    assertThat(adjustedLineMap.getPosition(8, 3)).isEqualTo(86);
    assertThat(adjustedLineMap.getPosition(8, 4)).isEqualTo(88);
    assertThat(adjustedLineMap.getPosition(9, 1)).isEqualTo(89);
    assertThat(adjustedLineMap.getPosition(10, 1)).isEqualTo(90);
    assertThat(adjustedLineMap.getPosition(10, 3)).isEqualTo(92);
  }

  @Test
  public void testGetLineNumber() {
    assertThat(adjustedLineMap.getLineNumber(0)).isEqualTo(1);
    assertThat(adjustedLineMap.getLineNumber(4)).isEqualTo(2);
    assertThat(adjustedLineMap.getLineNumber(9)).isEqualTo(2);
    assertThat(adjustedLineMap.getLineNumber(24)).isEqualTo(3);
    assertThat(adjustedLineMap.getLineNumber(30)).isEqualTo(3);
    assertThat(adjustedLineMap.getLineNumber(58)).isEqualTo(5);
    assertThat(adjustedLineMap.getLineNumber(65)).isEqualTo(6);
    assertThat(adjustedLineMap.getLineNumber(68)).isEqualTo(6);
    assertThat(adjustedLineMap.getLineNumber(73)).isEqualTo(6);
    assertThat(adjustedLineMap.getLineNumber(76)).isEqualTo(6);
    assertThat(adjustedLineMap.getLineNumber(78)).isEqualTo(6);
    assertThat(adjustedLineMap.getLineNumber(81)).isEqualTo(7);
    assertThat(adjustedLineMap.getLineNumber(82)).isEqualTo(8);
    assertThat(adjustedLineMap.getLineNumber(84)).isEqualTo(8);
    assertThat(adjustedLineMap.getLineNumber(88)).isEqualTo(8);
    assertThat(adjustedLineMap.getLineNumber(89)).isEqualTo(9);
    assertThat(adjustedLineMap.getLineNumber(90)).isEqualTo(10);
    assertThat(adjustedLineMap.getLineNumber(92)).isEqualTo(10);
  }

  @Test
  public void testGetColumnNumber() {
    assertThat(adjustedLineMap.getColumnNumber(0)).isEqualTo(1);
    assertThat(adjustedLineMap.getColumnNumber(4)).isEqualTo(1);
    assertThat(adjustedLineMap.getColumnNumber(9)).isEqualTo(2);
    assertThat(adjustedLineMap.getColumnNumber(24)).isEqualTo(5);
    assertThat(adjustedLineMap.getColumnNumber(30)).isEqualTo(6);
    assertThat(adjustedLineMap.getColumnNumber(58)).isEqualTo(10);
    assertThat(adjustedLineMap.getColumnNumber(65)).isEqualTo(1);
    assertThat(adjustedLineMap.getColumnNumber(68)).isEqualTo(2);
    assertThat(adjustedLineMap.getColumnNumber(73)).isEqualTo(7);
    assertThat(adjustedLineMap.getColumnNumber(76)).isEqualTo(8);
    assertThat(adjustedLineMap.getColumnNumber(78)).isEqualTo(10);
    assertThat(adjustedLineMap.getColumnNumber(81)).isEqualTo(1);
    assertThat(adjustedLineMap.getColumnNumber(82)).isEqualTo(1);
    assertThat(adjustedLineMap.getColumnNumber(84)).isEqualTo(2);
    assertThat(adjustedLineMap.getColumnNumber(86)).isEqualTo(3);
    assertThat(adjustedLineMap.getColumnNumber(88)).isEqualTo(4);
    assertThat(adjustedLineMap.getColumnNumber(92)).isEqualTo(3);
  }

  private static class SimpleLineMap implements LineMap {
    private static class Line {
      private final long startPos;
      private final long length;

      private Line(long startPos, long length) {
        this.startPos = startPos;
        this.length = length;
      }

      @Override
      public String toString() {
        return String.format("{Start: %s, len: %s}", startPos, length);
      }
    }

    private final List<Line> lines;
    private final String content;

    private SimpleLineMap(String content) {
      this.content = content;
      lines = new ArrayList<>();
      lines.add(new Line(0, 0));
      String[] contentLines = content.split("(?<=\n)");
      long startPos = 0;
      for (String contentLine : contentLines) {
        long length = contentLine.length();
        lines.add(new Line(startPos, length));
        startPos += length;
      }
    }

    @Override
    public long getStartPosition(long line) {
      checkArgument(
          line >= 1 && line < lines.size(), "Line %s exceeds existing lines: %s", line, lines);
      return lines.get((int) line).startPos;
    }

    @Override
    public long getPosition(long line, long column) {
      checkArgument(line >= 1 && line < lines.size());
      Line lineData = lines.get((int) line);
      checkArgument(column >= 1 && column <= lineData.length);
      return lineData.startPos + column - 1;
    }

    @Override
    public long getLineNumber(long pos) {
      checkArgument(pos >= 0);
      for (int i = 0; i < lines.size(); i++) {
        Line line = lines.get(i);
        if (line.startPos <= pos && pos < line.startPos + line.length) {
          return i;
        }
      }
      throw new IndexOutOfBoundsException(
          String.format("Pos %s exceed content length %s", pos, content.length()));
    }

    @Override
    public long getColumnNumber(long pos) {
      Line line = lines.get((int) getLineNumber(pos));
      return pos - line.startPos + 1;
    }
  }
}
