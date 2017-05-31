package org.javacomp.file;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PathUtilsTest {
  @Test
  public void testDefaultIgnorePathMatchers() {
    Path rootPath = Paths.get("/");
    assertThat(
            PathUtils.shouldIgnorePath(
                Paths.get("/path/to/.file.java"), rootPath, PathUtils.DEFAULT_IGNORE_MATCHERS))
        .isTrue();
    assertThat(
            PathUtils.shouldIgnorePath(
                Paths.get("/path/to/file.java.bak"), rootPath, PathUtils.DEFAULT_IGNORE_MATCHERS))
        .isTrue();
    assertThat(
            PathUtils.shouldIgnorePath(
                Paths.get("/path/to/file.java~"), rootPath, PathUtils.DEFAULT_IGNORE_MATCHERS))
        .isTrue();
    assertThat(
            PathUtils.shouldIgnorePath(
                Paths.get("/path/to/file.java"), rootPath, PathUtils.DEFAULT_IGNORE_MATCHERS))
        .isFalse();
  }

  @Test
  public void testIgnoreMatchedPsuedoAbsolutePaths() {
    FileSystem fs = FileSystems.getDefault();
    ImmutableList<PathMatcher> matchers = ImmutableList.of(fs.getPathMatcher("glob:/bar"));
    Path rootPath = Paths.get("/root/path");
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo"), rootPath, matchers))
        .isFalse();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo/bar"), rootPath, matchers))
        .isFalse();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/bar"), rootPath, matchers))
        .isTrue();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/bar/"), rootPath, matchers))
        .isTrue();
  }

  @Test
  public void testIgnoreMatchedFileName() {
    FileSystem fs = FileSystems.getDefault();
    ImmutableList<PathMatcher> matchers = ImmutableList.of(fs.getPathMatcher("glob:foo"));
    Path rootPath = Paths.get("/root/path");
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo/"), rootPath, matchers))
        .isTrue();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo"), rootPath, matchers))
        .isTrue();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo/bar"), rootPath, matchers))
        .isFalse();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/bar/"), rootPath, matchers))
        .isFalse();
  }

  @Test
  public void testIgnoreMatchedPathSegment() {
    FileSystem fs = FileSystems.getDefault();
    ImmutableList<PathMatcher> matchers = ImmutableList.of(fs.getPathMatcher("glob:**/foo/**"));
    Path rootPath = Paths.get("/root/path");
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo/"), rootPath, matchers))
        .isFalse();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo"), rootPath, matchers))
        .isFalse();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/foo/bar"), rootPath, matchers))
        .isTrue();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/bar/foo/bar"), rootPath, matchers))
        .isTrue();
    assertThat(PathUtils.shouldIgnorePath(Paths.get("/root/path/bar/bar/bar"), rootPath, matchers))
        .isFalse();
  }
}
