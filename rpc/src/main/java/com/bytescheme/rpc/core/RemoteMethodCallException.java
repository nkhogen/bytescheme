package com.bytescheme.rpc.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exception class for all the remote calls.
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class RemoteMethodCallException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final int code;

  public RemoteMethodCallException(int code) {
    super();
    this.code = code;
  }

  public RemoteMethodCallException(int code, String message) {
    super(message);
    this.code = code;
  }

  public RemoteMethodCallException(int code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  /* Make it Jackson friendly */
  @JsonProperty("detailMessage")
  public String getMessage() {
    return super.getMessage();
  }

  public int getCode() {
    return code;
  }
}
