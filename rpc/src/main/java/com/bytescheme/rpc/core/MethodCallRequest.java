package com.bytescheme.rpc.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * Method call model.
 * @author Naorem Khogendro Singh
 *
 */
public class MethodCallRequest implements Serializable {
  private static final long serialVersionUID = 1L;
  private UUID objectId;
  private String name;
  private String[] parameters;
  private UUID requestId;
  private String sessionId;

  public UUID getObjectId() {
    return objectId;
  }

  public void setObjectId(UUID objectId) {
    this.objectId = objectId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getParameters() {
    return parameters;
  }

  public void setParameters(String[] parameters) {
    this.parameters = parameters;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
}
