package com.bytescheme.service.controlboard;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.Constants;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;

/**
 * Sample power on/off test.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class SamplePowerTest {

  public static void main(String[] args) throws MalformedURLException {
    RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder(
        new HttpClientRequestHandler(Constants.PUBLIC_ENDPOINT));
    RemoteObjectClient client = null;
    String user = "abc@gmail.com";
    String password = null;
    try {
      password = CryptoUtils.kmsEncrypt(user);
      client = clientBuilder.login(user, password);
      Root root = client.createRemoteObject(Root.class, Constants.ROOT_OBJECT_ID);
      ControlBoard controlBoard = root.getControlBoard();
      DeviceStatus deviceStatus = new DeviceStatus(112, "Dummy");
      for (int i = 0; i < 20; i++) {
        deviceStatus.setPowerOn(!deviceStatus.isPowerOn());
        deviceStatus = controlBoard.changePowerStatus(deviceStatus);
        TimeUnit.SECONDS.sleep(1);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) {
        client.logout();
      }
    }
  }
}
