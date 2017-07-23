package com.bytescheme.service.controlboard.remoteobjects;

import java.io.File;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.collections4.MapUtils;

import com.bytescheme.common.utils.CryptoUtils;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * Default object endpoints provider. This class is meant for
 * FilePropertyChangePublisher.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DefaultObjectEndpointsProvider
    implements Function<String, Set<ObjectEndpoint>> {
  private final AtomicReference<Map<String, String>> objectIdsRef = new AtomicReference<>(
      Collections.emptyMap());
  private final AtomicReference<Map<String, String>> endpointsRef = new AtomicReference<>(
      Collections.emptyMap());
  private final String sshKeysDir;

  public DefaultObjectEndpointsProvider(String sshKeysDir) {
    File file = new File(sshKeysDir);
    Preconditions.checkArgument(file.exists() && file.isDirectory() && file.canRead(),
        "Invalid ssh key folder %s", sshKeysDir);
    this.sshKeysDir = sshKeysDir;
  }

  public void updateObjectIds(Map<String, String> properties) {
    Preconditions.checkNotNull(properties, "Invalid object IDs");
    objectIdsRef.set(properties);
  }

  public void updateEndpoints(Map<String, String> properties) {
    Preconditions.checkNotNull(properties, "Invalid endpoints");
    endpointsRef.set(properties);
  }

  @Override
  public Set<ObjectEndpoint> apply(String user) {
    Preconditions.checkNotNull(user, "Invalid user");
    Map<String, String> objectIds = objectIdsRef.get();
    if (MapUtils.isEmpty(objectIds)) {
      return null;
    }
    String objectId = objectIds.get(user);
    if (objectId == null) {
      return null;
    }
    Map<String, String> endpoints = endpointsRef.get();
    if (MapUtils.isEmpty(endpoints)) {
      return null;
    }
    String endpoint = endpoints.get(objectId);
    if (endpoint == null) {
      return null;
    }
    PublicKey publicKey = CryptoUtils
        .getPublicKey(sshKeysDir + File.separator + objectId.toString() + ".pub");
    return ImmutableSet.of(new ObjectEndpoint(objectId, endpoint, publicKey));
  }

}
