package org.javacomp.server;

/**
 * Error codes defined by Language Server Protocol.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#response-message
 */
public enum ErrorCode {
  // JSON RPC defined errors.

  /**
   * Invalid JSON was received by the server.
   *
   * <p>An error occurred on the server while parsing the JSON text.
   */
  PARSE_ERROR(-32700),
  /** The JSON sent is not a valid Request object. */
  INVALID_REQUEST(-32600),
  /** The method does not exist / is not available. */
  METHOD_NOT_FOUND(-32601),
  /** Invalid method parameter(s). */
  INVALID_PARAMS(-32602),
  /** Internal JSON-RPC error. */
  INTERNAL_ERROR(-32603),

  // Language Server Protocol defined errors.
  SERVER_NOT_INITIALIZED(-32002),
  UNKNOWN_ERROR_CODE(-32001),
  REQUEST_CANCELLED(-32800),
  ;

  private final int code;

  private ErrorCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
