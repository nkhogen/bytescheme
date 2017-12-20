package com.bytescheme.service.controlboard.common.models;

import java.util.List;

import com.bytescheme.rpc.core.RemoteObject;

public interface DeviceEventScheduler extends RemoteObject {
  boolean schedule(DeviceEventDetails eventDetails);

  boolean cancel(DeviceEventDetails eventDetails);

  List<DeviceEventDetails> list();
}
