package com.bytescheme.service.controlboard.common.models;

import java.util.UUID;

import com.bytescheme.rpc.core.RemoteObject;

public interface DeviceEventScheduler extends RemoteObject {
  public boolean schedule(DeviceEventDetails eventDetails);

  public boolean cancel(UUID eventId);
}
