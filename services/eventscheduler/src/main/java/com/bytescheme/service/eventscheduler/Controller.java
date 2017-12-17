package com.bytescheme.service.eventscheduler;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.bytescheme.service.eventscheduler.domains.Event;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

/**
 * This consumes the event and calls the remote method to turn on/off devices.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class Controller implements Consumer<Event> {
  private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
  private static final String ENDPOINT = "https://controller.bytescheme.com/rpc";
  private final RemoteObjectClientBuilder clientBuilder;

  public static class EventDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String user;
    private int deviceId;
    private boolean powerOn;

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
      Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "Invalid user");
    }
  }

  public Controller() {
    try {
      this.clientBuilder = new RemoteObjectClientBuilder(
          new HttpClientRequestHandler(ENDPOINT));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static Event createEvent(EventDetails eventDetails, long triggerTime) {
    Objects.requireNonNull(eventDetails, "Invalid event details").validate();
    Preconditions.checkArgument(
        triggerTime > Instant.now().getEpochSecond(),
        "Invalid trigger time");
    Event event = new Event();
    event.setTriggerTime(triggerTime);
    event.setDetails(JsonUtils.toJson(eventDetails));
    return event;
  }

  @Override
  public void accept(Event event) {
    EventDetails eventDetails = getEventDetails(event);
    String password = null;
    try {
      password = CryptoUtils.kmsEncrypt(eventDetails.getUser());
      RemoteObjectClient client = clientBuilder.login(eventDetails.getUser(), password);
      Root root = client.createRemoteObject(Root.class, Root.OBJECT_ID);
      ControlBoard controlBoard = root.getControlBoard(eventDetails.getUser());
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

  private EventDetails getEventDetails(Event event) {
    return JsonUtils.fromJson(event.getDetails(), EventDetails.class);
  }
}
