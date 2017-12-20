package com.bytescheme.service.controlboard.domains;

import java.security.PublicKey;
import java.util.UUID;

import com.google.common.base.Preconditions;

/**
 * Model class to store object ID and its endpoint.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class ObjectEndpoint {
  private final UUID objectId;
  private final String endpoint;
  private final PublicKey publicKey;

  public ObjectEndpoint(UUID objectId, String endpoint, PublicKey publicKey) {
    this.objectId = Preconditions.checkNotNull(objectId, "Invalid object ID");
    this.endpoint = Preconditions.checkNotNull(endpoint, "Invalid endpoint");
    this.publicKey = Preconditions.checkNotNull(publicKey, "Invalid public key");
  }

  public UUID getObjectId() {
    return objectId;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }
}
