package com.bytescheme.service.controlboard;

import java.util.Set;

import com.bytescheme.common.paths.Node;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.service.controlboard.remoteobjects.ObjectEndpoint;
import com.google.common.base.Function;

/**
 * Configuration data provider.
 *
 * @author Naorem Khogendro Singh
 *
 */
public interface ConfigurationProvider {
  Function<String, AuthData> getAuthenticationDataProvider();

  Function<String, Node<String>> getNodeProvider();

  Function<String, Set<ObjectEndpoint>> getObjectEndpointsProvider();
}
