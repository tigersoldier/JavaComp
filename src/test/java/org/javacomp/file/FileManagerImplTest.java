package org.javacomp.file;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class FileManagerImplTest {
  private static final int SNAPSHOT_WAIT_TIME_MILLIS = 100;

  @Rule public TestName testName = new TestName();
  @Rule public MockitoRule mrule = MockitoJUnit.rule();

  private Path testDir;
  private FileManagerImpl fileManager;
  private ExecutorService executor;
  private int waitTimeMillis;

  @Mock private FileChangeListener listener;

  @Before
  public void setUpTestDirAndFileManager() throws Exception {
    String testRootDir = System.getenv().get("TEST_TMPDIR");
    assertThat(testRootDir).isNotEmpty();

    testDir = Paths.get(testRootDir, testName.getMethodName());
    Files.deleteIfExists(testDir);
    Files.createDirectory(testDir);

    executor = Executors.newFixedThreadPool(1);

    fileManager = new FileManagerImpl(testDir.toUri(), executor);
    fileManager.setFileChangeListener(listener);
    // Sleep to make sure the WatchService background thread starts.
    Thread.sleep(100);
  }

  @Before
  public void setUpWaitTime() throws Exception {
    String osName = System.getProperty("os.name");
    try (WatchService ws = FileSystems.getDefault().newWatchService()) {
      if (ws.getClass().getSimpleName().startsWith("Poll")) {
        // The WatchService is implemented with polling. Set longer wait time.
        waitTimeMillis = 10_000;
      } else {
        // Using OS native file notification, 100ms should be sufficient.
        waitTimeMillis = 100;
      }
    }
  }

  @After
  public void shutdown() throws Exception {
    fileManager.shutdown();
    executor.shutdown();
  }

  @Test
  public void testCreateModifyAndDeleteFile() throws Exception {
    Path filePath = Paths.get(testDir.toString(), "testFile");
    Files.createFile(filePath);
    waitForWatcher();
    verify(listener).onFileChange(filePath, ENTRY_CREATE);
    verifyNoMoreInteractions(listener);

    setFileContent(filePath, "new file content");
    waitForWatcher();
    verify(listener, atLeastOnce()).onFileChange(filePath, ENTRY_MODIFY);
    verifyNoMoreInteractions(listener);

    Files.delete(filePath);
    waitForWatcher();
    verify(listener).onFileChange(filePath, ENTRY_DELETE);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void testSubDirectory() throws Exception {
    Path subDir = testDir.resolve("subdir");
    Path subSubDir = subDir.resolve("subsubdir");
    Files.createDirectory(subDir);
    Files.createDirectory(subSubDir);

    Path subFilePath = Paths.get(subDir.toString(), "subFile");
    Path subSubFilePath = Paths.get(subSubDir.toString(), "subSubFile");
    Files.createFile(subFilePath);
    Files.createFile(subSubFilePath);
    waitForWatcher();
    // Travis CI recives more than one create event, why?
    verify(listener, atLeastOnce()).onFileChange(subFilePath, ENTRY_CREATE);
    verify(listener, atLeastOnce()).onFileChange(subSubFilePath, ENTRY_CREATE);
    verifyNoMoreInteractions(listener);

    setFileContent(subFilePath, "new file content");
    setFileContent(subSubFilePath, "new sub sub file content");
    waitForWatcher();
    verify(listener, atLeastOnce()).onFileChange(subFilePath, ENTRY_MODIFY);
    verify(listener, atLeastOnce()).onFileChange(subSubFilePath, ENTRY_MODIFY);
    verifyNoMoreInteractions(listener);

    Files.delete(subFilePath);
    Files.delete(subSubFilePath);
    waitForWatcher();
    verify(listener).onFileChange(subFilePath, ENTRY_DELETE);
    verify(listener).onFileChange(subSubFilePath, ENTRY_DELETE);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void testSnapshotExistingFile() throws Exception {
    Path filePath = testDir.resolve("snapshot");
    Files.createFile(filePath);
    waitForWatcher();
    verify(listener).onFileChange(filePath, ENTRY_CREATE);
    verifyNoMoreInteractions(listener);

    fileManager.openFileForSnapshot(filePath.toUri(), "snapshot content");
    waitForSnapshotEvent();
    verify(listener).onFileChange(filePath, ENTRY_MODIFY);
    verifyNoMoreInteractions(listener);

    fileManager.setSnaphotContent(filePath.toUri(), "new content");
    waitForSnapshotEvent();
    verify(listener, times(2)).onFileChange(filePath, ENTRY_MODIFY);
    verifyNoMoreInteractions(listener);

    // Changes to actual file are ignored.
    setFileContent(filePath, "fs content");
    waitForWatcher();
    verifyNoMoreInteractions(listener);

    fileManager.closeFileForSnapshot(filePath.toUri());
    waitForSnapshotEvent();
    verify(listener, times(3)).onFileChange(filePath, ENTRY_MODIFY);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void testSnapshotNonExistingFile() throws Exception {
    Path filePath = testDir.resolve("snapshot");
    fileManager.openFileForSnapshot(filePath.toUri(), "snapshot content");
    waitForSnapshotEvent();
    verify(listener).onFileChange(filePath, ENTRY_CREATE);
    verifyNoMoreInteractions(listener);

    fileManager.setSnaphotContent(filePath.toUri(), "new content");
    waitForSnapshotEvent();
    verify(listener).onFileChange(filePath, ENTRY_MODIFY);
    verifyNoMoreInteractions(listener);

    // Changes to actual file are ignored.
    Files.createFile(filePath);
    waitForWatcher();
    verifyNoMoreInteractions(listener);
    setFileContent(filePath, "fs content");
    waitForWatcher();
    verifyNoMoreInteractions(listener);
    Files.delete(filePath);
    waitForWatcher();
    verifyNoMoreInteractions(listener);

    fileManager.closeFileForSnapshot(filePath.toUri());
    waitForSnapshotEvent();
    verify(listener).onFileChange(filePath, ENTRY_DELETE);
    verifyNoMoreInteractions(listener);
  }

  private void setFileContent(Path filePath, String content) throws Exception {
    try (OutputStream os = Files.newOutputStream(filePath)) {
      os.write(content.getBytes(UTF_8));
    }
  }

  private void waitForWatcher() throws Exception {
    Thread.sleep(waitTimeMillis);
  }

  private void waitForSnapshotEvent() throws Exception {
    Thread.sleep(SNAPSHOT_WAIT_TIME_MILLIS);
  }
}
