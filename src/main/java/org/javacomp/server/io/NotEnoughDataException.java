package org.javacomp.server.io;

public class NotEnoughDataException extends Exception {
  public NotEnoughDataException(String msg) {
    super(msg);
  }
}
