package com.bytescheme.service.eventscheduler.domains;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;

@DynamoDBTable(tableName = "Events")
public class Event {
  private UUID id;
  private UUID schedulerId;
  private Long triggerTime;
  private Long createTime;
  private Long modifyTime;
  private String details;
  private EventStatus status;
  private Long version;

  public static enum EventStatus {
    SCHEDULED, STARTED, ENDED, CANCELLED
  }

  @DynamoDBHashKey(attributeName = Constants.ID_FIELD)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @DynamoDBRangeKey(attributeName = Constants.SCHEDULER_ID)
  public UUID getSchedulerId() {
    return schedulerId;
  }

  public void setSchedulerId(UUID schedulerId) {
    this.schedulerId = schedulerId;
  }

  // TODO index on this
  @DynamoDBAttribute(attributeName = Constants.TRIGGER_TIME_FIELD)
  public Long getTriggerTime() {
    return triggerTime;
  }

  public void setTriggerTime(Long triggerTime) {
    this.triggerTime = triggerTime;
  }

  @DynamoDBAttribute(attributeName = Constants.CREATE_TIME_FIELD)
  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  @DynamoDBAttribute(attributeName = Constants.MODIFY_TIME_FIELD)
  public Long getModifyTime() {
    return modifyTime;
  }

  public void setModifyTime(Long modifyTime) {
    this.modifyTime = modifyTime;
  }

  @DynamoDBAttribute(attributeName = Constants.DETAILS_FIELD)
  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  @DynamoDBTyped(DynamoDBAttributeType.S)
  @DynamoDBAttribute(attributeName = Constants.STATUS_FIELD)
  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  @DynamoDBVersionAttribute(attributeName = Constants.VERSION_FIELD)
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
