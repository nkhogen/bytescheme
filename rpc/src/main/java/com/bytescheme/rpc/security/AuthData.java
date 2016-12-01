package com.bytescheme.rpc.security;

import java.io.Serializable;
import java.util.Set;

/**
 * Auth Data model
 * @author Naorem Khogendro Singh
 *
 */
public class AuthData implements Serializable {
  private static final long serialVersionUID = 1L;
  private String password;
  private Set<String> roles;

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }
}
