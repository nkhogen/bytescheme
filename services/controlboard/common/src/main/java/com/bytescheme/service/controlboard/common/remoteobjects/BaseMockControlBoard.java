package com.bytescheme.service.controlboard.common.remoteobjects;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.google.api.client.util.Preconditions;

/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class BaseMockControlBoard implements ControlBoard {
  private static Logger LOG = LoggerFactory.getLogger(BaseMockControlBoard.class);
  private static final long serialVersionUID = 1L;
  private final UUID objectId;
  private final Map<Integer, DeviceStatus> devices = new ConcurrentHashMap<>();

  public BaseMockControlBoard(UUID objectId) {
    Preconditions.checkNotNull(objectId, "Invalid object ID");
    this.objectId = objectId;
    for (int i = 0; i < 10; i++) {
      DeviceStatus device = new DeviceStatus();
      device.setPin(i);
      device.setPowerOn(false);
      device.setTag("Device " + i);
      devices.put(i, device);
    }
  }

  @Override
  public UUID getObjectId() {
    return objectId;
  }

  @Override
  public List<DeviceStatus> listDevices() {
    return new LinkedList<DeviceStatus>(devices.values());
  }

  @Override
  public DeviceStatus changePowerStatus(DeviceStatus status) {
    LOG.info("Received request with device status {}", status.toString());
    DeviceStatus device = devices.get(status.getPin());
    if (device == null) {
      throw new IllegalArgumentException("Unknown pin " + status);
    }
    device.setPowerOn(status.isPowerOn());
    return device;
  }

  @Override
  public String getVideoUrl() {
    throw new UnsupportedOperationException();
  }
}
