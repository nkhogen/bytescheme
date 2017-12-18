package com.bytescheme.service.controlboard.remoteobjects;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.service.controlboard.common.models.DeviceEventDetails;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.bytescheme.service.eventscheduler.domains.Event;

public class DeviceEventConsumer implements Consumer<Event> {
  private static final Logger LOG = LoggerFactory.getLogger(DeviceEventConsumer.class);

  private final Root root;

  public DeviceEventConsumer(Root root) {
    this.root = Objects.requireNonNull(root);
  }

  @Override
  public void accept(Event event) {
    try {
      DeviceEventDetails eventDetails = getEventDetails(event);
      ControlBoard controlBoard = root.getControlBoard(eventDetails.getUser());
      if (controlBoard == null) {
        LOG.error("No control board found for user {}", eventDetails.getUser());
        return;
      }
      List<DeviceStatus> devices = controlBoard.listDevices();
      DeviceStatus targetDevice = null;
      for (DeviceStatus device : devices) {
        if (device.getDeviceId() == eventDetails.getDeviceId()) {
          targetDevice = device;
          break;
        }
      }
      if (targetDevice == null) {
        LOG.error("Target device {} is not found", eventDetails);
        return;
      }
      if (targetDevice.isPowerOn() != eventDetails.isPowerOn()) {
        targetDevice.setPowerOn(eventDetails.isPowerOn());
        controlBoard.changePowerStatus(targetDevice);
      }
    } catch (Exception e) {
      LOG.info(String.format("Exception occurred in processing the event %s", event), e);
    }
  }

  private DeviceEventDetails getEventDetails(Event event) {
    return JsonUtils.fromJson(event.getDetails(), DeviceEventDetails.class);
  }
}
