package com.bytescheme.service.controlboard;

import java.net.MalformedURLException;
import java.time.Instant;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.Constants;
import com.bytescheme.service.controlboard.common.models.DeviceEventDetails;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;

/**
 * Sample scheduled power on/off test.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class SampleSchedulerTest {
  public static void main(String[] args) throws MalformedURLException {
    RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder(
        new HttpClientRequestHandler(Constants.PUBLIC_ENDPOINT));
    RemoteObjectClient client = null;
    String user = "abc@gmail.com";
    String password = null;
    try {
      password = CryptoUtils.kmsEncrypt(user);
      client = clientBuilder.login(user, password);
      Root root = client.createRemoteObject(Root.class, Root.OBJECT_ID);
      DeviceEventScheduler eventScheduler = root.getDeviceEventScheduler();
      DeviceEventDetails deviceEventDetails = new DeviceEventDetails();
      deviceEventDetails.setUser(user);
      deviceEventDetails.setDeviceId(0);
      deviceEventDetails.setTriggerTime(Instant.now().getEpochSecond() + 30);
      deviceEventDetails.setPowerOn(false);
      eventScheduler.schedule(deviceEventDetails);
      deviceEventDetails.setUser(user);
      deviceEventDetails.setDeviceId(0);
      deviceEventDetails.setTriggerTime(Instant.now().getEpochSecond() + 60);
      deviceEventDetails.setPowerOn(true);
      eventScheduler.schedule(deviceEventDetails);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) {
        client.logout();
      }
    }
  }
}
