package com.bytescheme.rpc.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * Logout call model.
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class LogoutCallRequest implements Serializable {
  private static final long serialVersionUID = 1L;
  private String sessionId;
  private UUID requestId;

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }
}
