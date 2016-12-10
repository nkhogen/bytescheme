package com.bytescheme.service.controlboard.remoteobjects;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;

import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;

/**
 * This delegates the call from client to the target (Raspberry pi) server.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DelegateControlBoardImpl implements ControlBoard {
  private static final long serialVersionUID = 1L;
  private final UUID objectId;
  private final ControlBoard remoteControlBoard;

  public DelegateControlBoardImpl(UUID objectId, ControlBoard remoteControlBoard)
      throws MalformedURLException {
    this.objectId = objectId;
    this.remoteControlBoard = remoteControlBoard;
  }

  @Override
  public UUID getObjectId() {
    return objectId;
  }

  @Override
  public List<DeviceStatus> listDevices() {
    return remoteControlBoard.listDevices();
  }

  @Override
  public DeviceStatus changePowerStatus(DeviceStatus status) {
    return remoteControlBoard.changePowerStatus(status);
  }

  @Override
  public String getVideoUrl() {
    return remoteControlBoard.getVideoUrl();
  }

}
