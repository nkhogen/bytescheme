package com.bytescheme.rpc.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * Login request model.
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class LoginCallRequest implements Serializable {
  private static final long serialVersionUID = 1L;
  private String user;
  private String password;
  private UUID requestId;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public UUID getRequestId() {
    return requestId;
  }

  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }
}
