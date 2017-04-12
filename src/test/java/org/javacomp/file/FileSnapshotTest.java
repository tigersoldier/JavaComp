package org.javacomp.file;

import static com.google.common.truth.Truth.assertThat;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FileSnapshotTest {
  private static final Optional<Integer> NO_RANGE_LENGTH = Optional.empty();

  @Test
  public void testApplyEdit_removeRange() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 1, 1, 6), NO_RANGE_LENGTH, "");
    assertThat(fileSnapshot.getContent()).isEqualTo("0g");
  }

  @Test
  public void testApplyEdit_replaceRange() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 5, 1, 3), NO_RANGE_LENGTH, "a\n01");
    assertThat(fileSnapshot.getContent()).isEqualTo("01234a\n01defg");
  }

  @Test
  public void testApplyEdit_replaceFromBegin() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 0, 1, 5), NO_RANGE_LENGTH, "ABC");
    assertThat(fileSnapshot.getContent()).isEqualTo("ABCfg");
  }

  @Test
  public void testApplyEdit_replaceUntilEnd() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 5, 1, 8), NO_RANGE_LENGTH, "abc");
    assertThat(fileSnapshot.getContent()).isEqualTo("01234abc");
  }

  @Test
  public void testApplyEdit_replaceAll() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 0, 1, 7), NO_RANGE_LENGTH, "abc");
    assertThat(fileSnapshot.getContent()).isEqualTo("abc");
  }

  @Test
  public void testApplyEdit_clear() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 0, 1, 7), NO_RANGE_LENGTH, "");
    assertThat(fileSnapshot.getContent()).isEqualTo("");
  }

  @Test
  public void testApplyEdit_insert() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(1, 4, 1, 4), NO_RANGE_LENGTH, "$$$");
    assertThat(fileSnapshot.getContent()).isEqualTo("0123456789\nabcd$$$efg");
  }

  @Test
  public void testApplyEditWithSmallerRangeLength_usesRangeLength() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 5, 1, 4), Optional.of(5), "$$$");
    assertThat(fileSnapshot.getContent()).isEqualTo("01234$$$\nabcdefg");
  }

  @Test
  public void testApplyEditWithGreaterRangeLength_usesEditRange() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0123456789\nabcdefg");
    fileSnapshot.applyEdit(createRange(0, 5, 1, 4), Optional.of(500), "$$$");
    assertThat(fileSnapshot.getContent()).isEqualTo("01234$$$efg");
  }

  @Test
  public void testLineTerminators() {
    FileSnapshot fileSnapshot = FileSnapshot.createFromContent("");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "\n\n\r\r\n");
    fileSnapshot.applyEdit(createRange(0, 0, 0, 0), NO_RANGE_LENGTH, "0");
    fileSnapshot.applyEdit(createRange(1, 0, 1, 0), NO_RANGE_LENGTH, "1");
    fileSnapshot.applyEdit(createRange(2, 0, 2, 0), NO_RANGE_LENGTH, "2");
    fileSnapshot.applyEdit(createRange(3, 0, 3, 0), NO_RANGE_LENGTH, "3");
    fileSnapshot.applyEdit(createRange(4, 0, 4, 0), NO_RANGE_LENGTH, "4");
    assertThat(fileSnapshot.getContent()).isEqualTo("0\n1\n2\r3\r\n4");
  }

  private static TextRange createRange(int line1, int char1, int line2, int char2) {
    return TextRange.create(TextPosition.create(line1, char1), TextPosition.create(line2, char2));
  }
}
