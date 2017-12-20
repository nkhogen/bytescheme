package com.bytescheme.service.controlboard.domains;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "ObjectRoles")
public class ObjectRoles {
  private UUID objectId;
  private String method;
  private String roles;

  @DynamoDBHashKey(attributeName = Constants.OBJECT_ID_FIELD)
  public UUID getObjectId() {
    return objectId;
  }

  public void setObjectId(UUID objectId) {
    this.objectId = objectId;
  }

  @DynamoDBRangeKey(attributeName = Constants.METHOD_FIELD)
  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  @DynamoDBAttribute(attributeName = Constants.ROLES_FIELD)
  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }
}
