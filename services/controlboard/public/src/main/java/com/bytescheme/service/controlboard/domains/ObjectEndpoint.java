package com.bytescheme.service.controlboard.domains;

import java.security.PublicKey;

import com.google.common.base.Preconditions;

/**
 * Model class to store object ID and its endpoint.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class ObjectEndpoint {
  private final String objectId;
  private final String endpoint;
  private final PublicKey publicKey;

  public ObjectEndpoint(String objectId, String endpoint, PublicKey publicKey) {
    this.objectId = Preconditions.checkNotNull(objectId, "Invalid object ID");
    this.endpoint = Preconditions.checkNotNull(endpoint, "Invalid endpoint");
    this.publicKey = Preconditions.checkNotNull(publicKey, "Invalid public key");
  }

  public String getObjectId() {
    return objectId;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }
}
