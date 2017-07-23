package com.bytescheme.rpc.security;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * File based authentication provider class.
 * E.g { "myname@gmail.com": {"password": "**", "roles": ["role1", "role2"] } }
 *
 * @author Naorem Khogendro Singh
 *
 */
public class BasicAuthenticationProvider
    implements AuthenticationProvider {
  protected final Function<String, AuthData> authDataProvider;

  public BasicAuthenticationProvider(Function<String, AuthData> authDataProvider) {
    this.authDataProvider = Preconditions.checkNotNull(authDataProvider, "Invalid auth data provider");
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    Preconditions.checkNotNull(authentication, "Invalid authentication object");
    Preconditions.checkNotNull(authentication.getUser(), "Invalid user");
    AuthData authData = authDataProvider.apply(authentication.getUser());
    if (authData == null) {
      return null;
    }
    if (authData.getPassword().equals(authentication.getPassword())) {
      return null;
    }
    return new Authentication(authentication.getUser(), authData.getPassword(),
        authData.getRoles());
  }
}
