package org.javacomp.server.protocol;

import java.net.URI;

/**
 * Parameters for "initialize" method.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#initialize-request
 */
public class InitializeParams implements RequestParams {
  public int processId;
  public URI rootUri;
  public ClientCapabilities capabilities;
  public String trace;
  // initializationOptions is not supported yet.
  // rootPath is deprecated and not supported.
}
