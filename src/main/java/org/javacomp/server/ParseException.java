package org.javacomp.server;

/** Exception thrown during parsing requests. */
public class ParseException extends Exception {
  private final ErrorCode errorCode;

  public ParseException(ErrorCode errorCode, String fmt, Object... args) {
    super(String.format(fmt, args));
    this.errorCode = errorCode;
  }

  public ParseException(ErrorCode errorCode, Throwable throwable, String fmt, Object... args) {
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
