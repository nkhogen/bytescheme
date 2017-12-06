package com.bytescheme.service.controlboard.device;

import java.util.Objects;

import com.bytescheme.common.sockets.SimpleEventServer;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

/**
 * Controls a device which can be directly attached to the hub or connected via
 * a controller node.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceController {
  public static final String SET_POWER_EVENT_FORMAT = "SET %d %d";
  public static final String GET_POWER_EVENT_FORMAT = "GET %d";

  private final DeviceStatus deviceStatus;
  private final GpioPinDigitalOutput digitalOutput;
  private final SimpleEventServer eventServer;

  public DeviceController(DeviceStatus deviceStatus, SimpleEventServer eventServer) {
    this.deviceStatus = Objects.requireNonNull(deviceStatus);
    this.eventServer = Objects.requireNonNull(eventServer);
    if (deviceStatus.getControllerId() == 0) {
      this.digitalOutput = GpioFactory.getInstance()
          .provisionDigitalOutputPin(GpioUtils.getRaspiPin(deviceStatus.getPin()));
    } else {
      Objects.requireNonNull(eventServer);
      this.digitalOutput = null;
    }
  }

  public DeviceStatus changeDeviceStatus(boolean status) {
    if (digitalOutput == null) {
      deviceStatus
          .setPowerOn(Boolean.valueOf(eventServer
              .sendEvent(deviceStatus.getControllerId(), String
                  .format(SET_POWER_EVENT_FORMAT, deviceStatus.getPin(), status ? 1 : 0))
              .trim()));
    } else {
      digitalOutput.setState(status ? PinState.HIGH : PinState.LOW);
      deviceStatus.setPowerOn(digitalOutput.isHigh());
    }
    return deviceStatus;
  }

  public DeviceStatus getDeviceStatus(boolean load) {
    if (load) {
      if (digitalOutput == null) {
        deviceStatus
            .setPowerOn(Boolean.valueOf(eventServer
                .sendEvent(deviceStatus.getControllerId(),
                    String.format(GET_POWER_EVENT_FORMAT, deviceStatus.getPin()))
                .trim()));
      } else {
        deviceStatus.setPowerOn(digitalOutput.isHigh());
      }
    }
    return deviceStatus;
  }
}
