package com.bytescheme.service.controlboard.domains;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Endpoints")
public class Endpoints {
  private UUID objectId;
  private String endpoint;
  private String sshKey;

  @DynamoDBHashKey(attributeName = Constants.OBJECT_ID_FIELD)
  public UUID getObjectId() {
    return objectId;
  }

  public void setObjectId(UUID objectId) {
    this.objectId = objectId;
  }

  @DynamoDBAttribute(attributeName = Constants.ENDPOINT_FIELD)
  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  @DynamoDBAttribute(attributeName = Constants.SSH_KEY_FIELD)
  public String getSshKey() {
    return sshKey;
  }

  public void setSshKey(String sshKey) {
    this.sshKey = sshKey;
  }
}
