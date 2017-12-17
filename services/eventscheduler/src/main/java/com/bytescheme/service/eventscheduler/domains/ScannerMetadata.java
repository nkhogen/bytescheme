package com.bytescheme.service.eventscheduler.domains;

import java.util.UUID;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.bytescheme.common.utils.JsonUtils;

/**
 * @author Naorem Khogendro Singh
 *
 */
@DynamoDBTable(tableName = "Scanners")
public class ScannerMetadata {
  private UUID id;
  private Long scanTime;
  private Long version;

  @DynamoDBHashKey(attributeName = "ID")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @DynamoDBAttribute(attributeName = "SCAN_TIME")
  public Long getScanTime() {
    return scanTime;
  }

  public void setScanTime(Long scanTime) {
    this.scanTime = scanTime;
  }

  @DynamoDBVersionAttribute(attributeName = "VERSION")
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return JsonUtils.toJson(this);
  }
}
