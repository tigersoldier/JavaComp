package org.javacomp.server;

import java.net.URI;
import javax.annotation.Nullable;
import org.javacomp.file.FileManager;
import org.javacomp.options.JavaCompOptions;
import org.javacomp.project.Project;
import org.javacomp.protocol.ClientCapabilities;

/** Interface for server functionalities to be accessed by components. */
public interface Server {
  /**
   * Initializes the server. No request is accepted without initialization.
   *
   * @param clientProcessId the process ID of the client. The server can monitor the client process
   *     and exit if the client process dies
   * @param projectRoot the root directory of the project
   * @param clientCapabilities the capabilities of the client
   * @param options the user-provided options
   */
  void initialize(
      int clientProcessId,
      URI projectRoot,
      ClientCapabilities clientCapabilities,
      @Nullable JavaCompOptions options);
  /** Stops accepting any requests other than {@code exit}. */
  void shutdown();
  /** Stops the server and ends the server process. */
  void exit();

  /**
   * Gets the file manager for the server.
   *
   * @throws IllegalStateException if the server is not initialized or shutdown
   */
  FileManager getFileManager();

  Project getProject();

  ClientCapabilities getClientCapabilities();
}
