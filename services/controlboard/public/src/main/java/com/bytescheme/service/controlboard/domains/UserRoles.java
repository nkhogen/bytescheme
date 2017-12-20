package com.bytescheme.service.controlboard.domains;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "UserRoles")
public class UserRoles {
  private String user;
  private String password;
  private String roles;

  @DynamoDBHashKey(attributeName = Constants.USER_FIELD)
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  @DynamoDBAttribute(attributeName = Constants.PASSWORD_FIELD)
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @DynamoDBAttribute(attributeName = Constants.ROLES_FIELD)
  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }
}
