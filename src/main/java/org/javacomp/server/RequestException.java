package org.javacomp.server;

/** Exception thrown during handling requests. */
public class RequestException extends Exception {
  private final ErrorCode errorCode;

  public RequestException(ErrorCode errorCode, String fmt, Object... args) {
    super(String.format(fmt, args));
    this.errorCode = errorCode;
  }

  public RequestException(ErrorCode errorCode, Throwable throwable, String fmt, Object... args) {
    super(String.format(fmt, args), throwable);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  @Override
  public String toString() {
    return errorCode.name() + ": " + super.toString();
  }
}
