package com.bytescheme.rpc.core;

public class RemoteMethodCallException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RemoteMethodCallException() {
    super();
  }

  public RemoteMethodCallException(String message) {
    super(message);
  }

  public RemoteMethodCallException(String message, Throwable cause) {
    super(message, cause);
  }
}
