package com.bytescheme.rpc.security;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.bytescheme.common.properties.PropertyChangeListener;
import com.google.common.base.Function;

/**
 * File based authentication data provider. This class is meant for
 * FilePropertyChangePublisher.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DefaultAuthenticationDataProvider
    implements Function<String, AuthData>, PropertyChangeListener<AuthData> {
  protected final AtomicReference<Map<String, AuthData>> authDataMapRef = new AtomicReference<>(
      Collections.emptyMap());

  @Override
  public void onPropertyChange(Map<String, AuthData> changedProperties,
      Map<String, AuthData> allProperties) {
    authDataMapRef.set(allProperties);
  }

  @Override
  public AuthData apply(String input) {
    return authDataMapRef.get().get(input);
  }
}
