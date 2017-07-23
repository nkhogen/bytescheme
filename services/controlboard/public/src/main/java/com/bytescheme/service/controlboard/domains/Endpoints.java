package com.bytescheme.service.controlboard.domains;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Endpoints")
public class Endpoints {
  private String objectId;
  private String endpoint;
  private String sshKey;

  @DynamoDBHashKey(attributeName = "OBJECT_ID")
  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  @DynamoDBAttribute(attributeName="ENDPOINT")
  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  @DynamoDBAttribute(attributeName="SSH_KEY")
  public String getSshKey() {
    return sshKey;
  }

  public void setSshKey(String sshKey) {
    this.sshKey = sshKey;
  }
}
