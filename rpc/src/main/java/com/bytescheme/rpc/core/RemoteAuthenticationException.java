package com.bytescheme.rpc.core;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Naorem Khogendro Singh
 *
 */
public class RemoteAuthenticationException extends RemoteMethodCallException implements Serializable {
  private static final long serialVersionUID = 1L;

  public RemoteAuthenticationException() {
    super();
  }

  public RemoteAuthenticationException(String message) {
    super(message);
  }

  public RemoteAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }

  /* Make it Jackson friendly */
  @JsonProperty("detailMessage")
  public String getMessage() {
    return super.getMessage();
  }
}
