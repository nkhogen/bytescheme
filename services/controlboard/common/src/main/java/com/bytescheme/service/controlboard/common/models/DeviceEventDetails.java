package com.bytescheme.service.controlboard.common.models;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nullable;

import com.bytescheme.common.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

/**
 * Details of a device event.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceEventDetails implements Serializable {
  private static final long serialVersionUID = 1L;

  private @Nullable UUID id;
  private @Nullable UUID schedulerId;

  private String user;
  private int deviceId;
  private long triggerTime;
  private boolean powerOn;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getSchedulerId() {
    return schedulerId;
  }

  public void setSchedulerId(UUID schedulerId) {
    this.schedulerId = schedulerId;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public int getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(int deviceId) {
    this.deviceId = deviceId;
  }

  public long getTriggerTime() {
    return triggerTime;
  }

  public void setTriggerTime(long triggerTime) {
    this.triggerTime = triggerTime;
  }

  public boolean isPowerOn() {
    return powerOn;
  }

  public void setPowerOn(boolean powerOn) {
    this.powerOn = powerOn;
  }

  @Override
  public String toString() {
    return JsonUtils.toJson(this);
  }

  @JsonIgnore
  public void validate() {
    Preconditions.checkArgument(deviceId >= 0, "Invalid device ID");
    Preconditions.checkArgument(
        triggerTime > Instant.now().getEpochSecond(),
        "Trigger time is invalid");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "Invalid user");
  }
}
