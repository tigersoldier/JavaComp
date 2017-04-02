package org.javacomp.server;

/** Interface for server functionalities to be accessed by components. */
public interface Server {
  /**
   * Initializes the server. No request is accepted without initialization.
   *
   * @param clientProcessId the process ID of the client. The server can monitor the client process
   *     and exit if the client process dies.
   */
  void initialize(int clientProcessId);
  /** Stops accepting any requests other than {@code exit}. */
  void shutdown();
  /** Stops the server and ends the server process. */
  void exit();
}
