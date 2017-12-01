package com.bytescheme.service.controlboard.video;

import java.util.UUID;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;

public class VideoUrlTest {
  // @Test
  public void testGetVideoUrl() throws Exception {
    RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder(
        new HttpClientRequestHandler("https://localhost:8443/rpc"));
    RemoteObjectClient client = clientBuilder.login("controlboard", CryptoUtils.encrypt(
        "controlboard",
        CryptoUtils.getPublicKey("src/test/resources/bfd8dd0a-10db-4782-86ec-b27f52d6362c.pub")));
    ControlBoard board = client.createRemoteObject(ControlBoard.class,
        UUID.fromString("bfd8dd0a-10db-4782-86ec-b27f52d6362c"));
    System.out.println(board.getVideoUrl());
  }
}
