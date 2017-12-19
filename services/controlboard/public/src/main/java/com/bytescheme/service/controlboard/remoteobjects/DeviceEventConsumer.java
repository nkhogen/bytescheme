package com.bytescheme.service.controlboard.remoteobjects;

import java.net.MalformedURLException;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.Constants;
import com.bytescheme.service.controlboard.common.models.DeviceEventDetails;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.bytescheme.service.eventscheduler.domains.Event;

/**
 * This consumes the events and calls the remote method to power on/off.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceEventConsumer implements Consumer<Event> {
  private static final Logger LOG = LoggerFactory.getLogger(DeviceEventConsumer.class);

  private final RemoteObjectClientBuilder clientBuilder;

  public DeviceEventConsumer() throws MalformedURLException {
    this.clientBuilder = new RemoteObjectClientBuilder(
        new HttpClientRequestHandler(Constants.PUBLIC_ENDPOINT));
  }

  @Override
  public void accept(Event event) {
    RemoteObjectClient client = null;
    try {
      DeviceEventDetails eventDetails = getEventDetails(event);
      String user = eventDetails.getUser();
      String password = CryptoUtils.kmsEncrypt(user);
      client = clientBuilder.login(user, password);
      Root root = client.createRemoteObject(Root.class, Root.OBJECT_ID);
      ControlBoard controlBoard = root.getControlBoard();
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
    } finally {
      if (client != null) {
        client.logout();
      }
    }
  }

  private DeviceEventDetails getEventDetails(Event event) {
    return JsonUtils.fromJson(event.getDetails(), DeviceEventDetails.class);
  }
}
