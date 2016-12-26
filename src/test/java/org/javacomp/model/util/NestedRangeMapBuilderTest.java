package org.javacomp.model.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NestedRangeMapBuilderTest {
  @Test
  public void noRange() {
    NestedRangeMapBuilder<Integer> builder = new NestedRangeMapBuilder<>();
    ImmutableRangeMap<Integer, Integer> map = builder.build();
    assertThat(map.asMapOfRanges()).isEmpty();
  }

  @Test
  public void oneEmptyRange() {
    NestedRangeMapBuilder<Integer> builder = new NestedRangeMapBuilder<>();
    builder.put(Range.closedOpen(1, 1), 1);
    assertThat(builder.build().asMapOfRanges()).isEmpty();
  }

  @Test
  public void oneSingularRange() {
    NestedRangeMapBuilder<Integer> builder = new NestedRangeMapBuilder<>();
    builder.put(Range.closed(1, 1), 1);
    assertThat(builder.build().asMapOfRanges()).containsExactly(Range.closedOpen(1, 2), 1);
  }

  @Test
  public void oneNonEmptyRange() {
    NestedRangeMapBuilder<Integer> builder = new NestedRangeMapBuilder<>();
    builder.put(Range.closedOpen(15, 20), 2);
    assertThat(builder.build().asMapOfRanges()).containsExactly(Range.closedOpen(15, 20), 2);
  }

  // 0        2        4        6        8
  // <---1---><---2--->         <---3--->
  @Test
  public void disconnectedTopLevelRanges() {
    NestedRangeMapBuilder<Integer> builder = new NestedRangeMapBuilder<>();
    builder.put(Range.closedOpen(2, 4), 2);
    builder.put(Range.closedOpen(6, 8), 3);
    builder.put(Range.closedOpen(0, 2), 1);
    assertThat(builder.build().asMapOfRanges())
        .containsExactly(
            Range.closedOpen(0, 2), 1,
            Range.closedOpen(2, 4), 2,
            Range.closedOpen(6, 8), 3);
  }

  // 0    1    2    3    4    5    6    7    8    9    10   11   12   13   14
  // <------------------------------1------------------------------------->
  // <---------------2-----------------><----3--->     <------4------>
  //      <----5--->     <----6--->          <-7->          <-8->
  @Test
  public void nestedRanges() {
    NestedRangeMapBuilder<Integer> builder = new NestedRangeMapBuilder<>();
    builder.put(Range.closedOpen(0, 14), 1);
    builder.put(Range.closedOpen(0, 7), 2);
    builder.put(Range.closedOpen(7, 9), 3);
    builder.put(Range.closedOpen(10, 13), 4);
    builder.put(Range.closedOpen(1, 3), 5);
    builder.put(Range.closedOpen(4, 6), 6);
    builder.put(Range.closedOpen(8, 9), 7);
    builder.put(Range.closedOpen(11, 12), 8);
    assertThat(builder.build().asMapOfRanges())
        .containsExactly(
            Range.closedOpen(0, 1), 2,
            Range.closedOpen(1, 3), 5,
            Range.closedOpen(3, 4), 2,
            Range.closedOpen(4, 6), 6,
            Range.closedOpen(6, 7), 2,
            Range.closedOpen(7, 8), 3,
            Range.closedOpen(8, 9), 7,
            Range.closedOpen(9, 10), 1,
            Range.closedOpen(10, 11), 4,
            Range.closedOpen(11, 12), 8,
            Range.closedOpen(12, 13), 4,
            Range.closedOpen(13, 14), 1);
  }
}
