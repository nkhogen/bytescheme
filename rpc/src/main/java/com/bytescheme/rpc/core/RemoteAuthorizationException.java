package com.bytescheme.rpc.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public class RemoteAuthorizationException extends RemoteMethodCallException {
  private static final long serialVersionUID = 1L;

  public RemoteAuthorizationException() {
    super();
  }

  public RemoteAuthorizationException(String message) {
    super(message);
  }

  public RemoteAuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }

  /* Make it Jackson friendly */
  @JsonProperty("detailMessage")
  public String getMessage() {
    return super.getMessage();
  }
}
