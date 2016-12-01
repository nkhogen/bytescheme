package com.bytescheme.rpc.core;

import java.io.Serializable;

/**
 * Method response model.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class MethodCallResponse implements Serializable {
  private static final long serialVersionUID = 1L;
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
