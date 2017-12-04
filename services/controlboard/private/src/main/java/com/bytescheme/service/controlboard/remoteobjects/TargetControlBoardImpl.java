package com.bytescheme.service.controlboard.remoteobjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import com.bytescheme.common.sockets.SimpleEventServer;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.device.DeviceController;
import com.bytescheme.service.controlboard.video.VideoBroadcastHandler;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

/**
 * Actual communication with the Raspberry pi happens here.
 *
 * @see <a href="http://pi4j.com/example/control.html">http://pi4j.com/example/
 *      control.html</a>
 * 
 * @author Naorem Khogendro Singh
 *
 */
@SuppressWarnings("unused")
public class TargetControlBoardImpl implements ControlBoard {
  private static final long serialVersionUID = 1L;
  private final UUID objectId;
  private final String videoUrlFormat;
  private final SimpleEventServer eventServer;
  private final Map<Integer, DeviceController> devicesControllers = new HashMap<>();

  public TargetControlBoardImpl(UUID objectId, Map<String, Map<Integer, String>> devices,
      String videoUrlFormat, SimpleEventServer eventServer) {
    Preconditions.checkNotNull(objectId, "Invalid object ID");
    Preconditions.checkNotNull(MapUtils.isEmpty(devices), "Invalid tags");
    Preconditions.checkNotNull(!Strings.isNullOrEmpty(videoUrlFormat), "Invalid video URL format");
    this.objectId = objectId;
    this.videoUrlFormat = videoUrlFormat;
    this.eventServer = eventServer;
    devices.entrySet().stream().forEach(e -> {
      if (e.getKey().equals("0")) {
        e.getValue().entrySet().forEach(d -> {
          DeviceStatus device = new DeviceStatus();
          device.setPin(d.getKey());
          device.setTag(d.getValue());
          Preconditions.checkArgument(
              devicesControllers.put(d.getKey(), new DeviceController(device)) == null);
        });
      } else {
        UUID controllerNodeId = UUID.fromString(e.getKey());
        e.getValue().entrySet().forEach(d -> {
          DeviceStatus device = new DeviceStatus();
          device.setPin(d.getKey());
          device.setTag(d.getValue());
          Preconditions.checkArgument(devicesControllers.put(d.getKey(),
              new DeviceController(device, controllerNodeId, eventServer)) == null);
        });
      }
    });
  }

  @Override
  public UUID getObjectId() {
    return objectId;
  }

  @Override
  public List<DeviceStatus> listDevices() {
    return devicesControllers.values().stream().map(d -> d.getDeviceStatus(true))
        .collect(Collectors.toList());
  }

  @Override
  public DeviceStatus changePowerStatus(DeviceStatus device) {
    return Preconditions.checkNotNull(devicesControllers.get(device.getPin()),
        "Device %s not found", device.toString())
        .changeDeviceStatus(device.isPowerOn() ? true : false);
  }

  @Override
  public String getVideoUrl() {
    return String.format(videoUrlFormat, VideoBroadcastHandler.getInstance().generateSecret());
  }
}
