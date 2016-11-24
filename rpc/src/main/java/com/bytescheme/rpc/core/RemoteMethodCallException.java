package com.bytescheme.rpc.core;

import com.fasterxml.jackson.annotation.JsonProperty;

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

  /* Make it Jackson friendly */
  @JsonProperty("detailMessage")
  public String getMessage() {
    return super.getMessage();
  }
}
