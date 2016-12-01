package com.bytescheme.service.controlboard.remoteobjects;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.gpio.GpioUtils;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

/**
 * Actual communication with the Raspberry pi happens here.
 * @see <a href="http://pi4j.com/example/control.html">http://pi4j.com/example/control.html</a>
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class TargetControlBoardImpl implements ControlBoard {
  private static final long serialVersionUID = 1L;
  private final UUID objectId;
  private final GpioController gpio;
  private final Map<Integer, SimpleEntry<DeviceStatus, GpioPinDigitalOutput>> devicesMap = new HashMap<>();

  public TargetControlBoardImpl(UUID objectId, Map<Integer, String> tags) {
    this.objectId = objectId;
    this.gpio = GpioFactory.getInstance();
    for (Map.Entry<Integer, String> tagEntry : tags.entrySet()) {
      DeviceStatus device = new DeviceStatus();
      device.setPin(tagEntry.getKey());
      device.setTag(tagEntry.getValue());
      GpioPinDigitalOutput pin = gpio
          .provisionDigitalOutputPin(GpioUtils.getRaspiPin(tagEntry.getKey()));
      devicesMap.put(tagEntry.getKey(), new SimpleEntry<>(device, pin));
    }
  }

  @Override
  public UUID getObjectId() {
    return objectId;
  }

  @Override
  public List<DeviceStatus> listDevices() {
    List<DeviceStatus> devices = new LinkedList<DeviceStatus>();
    for (Map.Entry<Integer, SimpleEntry<DeviceStatus, GpioPinDigitalOutput>> deviceEntry : devicesMap
        .entrySet()) {
      DeviceStatus device = deviceEntry.getValue().getKey();
      GpioPinDigitalOutput pin = deviceEntry.getValue().getValue();
      device.setPowerOn(pin.getState() == PinState.HIGH);
      devices.add(device);
    }
    return devices;
  }

  @Override
  public DeviceStatus changePowerStatus(DeviceStatus device) {
    Preconditions.checkNotNull(device, "Invalid device");
    SimpleEntry<DeviceStatus, GpioPinDigitalOutput> deviceEntry = devicesMap
        .get(device.getPin());
    Preconditions.checkNotNull(deviceEntry, "Device %s not found", device.toString());
    deviceEntry.getValue().setState(device.isPowerOn() ? PinState.HIGH : PinState.LOW);
    deviceEntry.getKey().setPowerOn(deviceEntry.getValue().getState() == PinState.HIGH);
    return deviceEntry.getKey();
  }
}
