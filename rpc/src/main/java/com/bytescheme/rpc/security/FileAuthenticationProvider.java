package com.bytescheme.rpc.security;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.bytescheme.common.properties.PropertyChangeListener;
import com.google.common.base.Preconditions;

/**
 * File based authentication provider class.
 * E.g { "myname@gmail.com": {"password": "**", "roles": ["role1", "role2"] } }
 *
 * @author Naorem Khogendro Singh
 *
 */
public class FileAuthenticationProvider
    implements AuthenticationProvider, PropertyChangeListener<AuthData> {
  protected final AtomicReference<Map<String, AuthData>> authDataMapRef = new AtomicReference<>(
      Collections.emptyMap());

  public FileAuthenticationProvider(Map<String, AuthData> authDataMap) {
    Preconditions.checkNotNull(authDataMap, "Invalid auth data map");
    this.authDataMapRef.set(authDataMap);
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    Preconditions.checkNotNull(authentication, "Invalid authentication object");
    Preconditions.checkNotNull(authentication.getUser(), "Invalid user");
    Map<String, AuthData> authDataMap = authDataMapRef.get();
    AuthData authData = authDataMap.get(authentication.getUser());
    if (authData == null) {
      return null;
    }
    if (authData.getPassword().equals(authentication.getPassword())) {
      return null;
    }
    return new Authentication(authentication.getUser(), authData.getPassword(),
        authData.getRoles());
  }

  @Override
  public void onPropertyChange(Map<String, AuthData> changedProperties,
      Map<String, AuthData> allProperties) {
    authDataMapRef.set(allProperties);
  }
}
