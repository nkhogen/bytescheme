package com.bytescheme.service.controlboard;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.bytescheme.common.paths.Node;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.service.controlboard.domains.ObjectEndpoint;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Configuration data provider.
 *
 * @author Naorem Khogendro Singh
 *
 */
public interface ConfigurationProvider {
  // Provides auth data for an user.
  Function<String, AuthData> getAuthenticationDataProvider();

  // Provides Node data for an object ID.
  Function<UUID, Node<String>> getNodeProvider();

  // Provides endpoints for an user.
  Function<String, Set<ObjectEndpoint>> getObjectEndpointsProvider();

  // Helper
  default ObjectEndpoint getObjectEndPoint(String user) {
    Set<ObjectEndpoint> objectEndpoints = getObjectEndpointsProvider()
        .apply(Objects.requireNonNull(user));
    // Support for only one
    ObjectEndpoint objectEndpoint = Objects
        .requireNonNull(Iterables.getFirst(objectEndpoints, null));
    Objects.requireNonNull(objectEndpoint.getObjectId());
    Objects.requireNonNull(objectEndpoint.getEndpoint());
    return objectEndpoint;
  }
}
