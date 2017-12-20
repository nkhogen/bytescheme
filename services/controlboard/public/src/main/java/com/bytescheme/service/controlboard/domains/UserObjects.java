package com.bytescheme.service.controlboard.domains;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "UserObjects")
public class UserObjects {
  private String user;
  private UUID objectId;

  @DynamoDBHashKey(attributeName = Constants.USER_FIELD)
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  @DynamoDBAttribute(attributeName = Constants.OBJECT_ID_FIELD)
  public UUID getObjectId() {
    return objectId;
  }

  public void setObjectId(UUID objectId) {
    this.objectId = objectId;
  }
}
