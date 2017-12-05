package com.bytescheme.service.controlboard.common.models;

import java.io.Serializable;
import java.util.Objects;

import com.bytescheme.common.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for device status.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceStatus implements Serializable {
  public static final int ID_MULTIPLIER = 100;
  private static final long serialVersionUID = 1L;
  private final int deviceId;
  private final String tag;
  private boolean powerOn;

  @JsonCreator
  public DeviceStatus(@JsonProperty("deviceId") int deviceId, @JsonProperty("tag") String tag) {
    this.deviceId = deviceId;
    this.tag = Objects.requireNonNull(tag);
  }

  public int getDeviceId() {
    return deviceId;
  }

  public String getTag() {
    return tag;
  }

  public boolean isPowerOn() {
    return powerOn;
  }

  public void setPowerOn(boolean powerOn) {
    this.powerOn = powerOn;
  }

  @JsonIgnore
  public int getControllerId() {
    return getDeviceId() / ID_MULTIPLIER;
  }

  @JsonIgnore
  public int getPin() {
    return getDeviceId() % ID_MULTIPLIER;
  }

  @Override
  public String toString() {
    return JsonUtils.toJson(this);
  }
}
