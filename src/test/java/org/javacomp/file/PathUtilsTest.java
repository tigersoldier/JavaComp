package org.javacomp.file;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PathUtilsTest {
  @Test
  public void testShouldIgnoreFile() {
    assertThat(PathUtils.shouldIgnoreFile(Paths.get("/path/to/.file.java"))).isTrue();
    assertThat(PathUtils.shouldIgnoreFile(Paths.get("/path/to/file.java.bak"))).isTrue();
    assertThat(PathUtils.shouldIgnoreFile(Paths.get("/path/to/file.java~"))).isTrue();
    assertThat(PathUtils.shouldIgnoreFile(Paths.get("/path/to/file.java"))).isFalse();
  }
}
