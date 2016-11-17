package com.bytescheme.rpc.core;

public class MethodCallResponse {
  private String returnValue;
  private RemoteMethodCallException exception;

  public String getReturnValue() {
    return returnValue;
  }

  public void setReturnValue(String returnValue) {
    this.returnValue = returnValue;
  }

  public RemoteMethodCallException getException() {
    return exception;
  }

  public void setException(RemoteMethodCallException exception) {
    this.exception = exception;
  }
}
