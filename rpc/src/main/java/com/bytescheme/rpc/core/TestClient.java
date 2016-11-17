package com.bytescheme.rpc.core;

import java.net.MalformedURLException;
import java.util.UUID;

public class TestClient {
  public interface HelloProcessor extends RemoteObject {
    HelloProcessor hello();
    String hello(String name);
  }
  
  public static class HelloProcessImpl implements HelloProcessor {
    public UUID objectId = new UUID(0L, 1L);

    @Override
    public UUID getObjectId() {
      return objectId;
    }

    @Override
    public HelloProcessor hello() {
      HelloProcessImpl impl = new HelloProcessImpl();
      impl.objectId = new UUID(0L, 2L);
      return impl;
    }

    @Override
    public String hello(String name) {
      return "Hello "+name+"! How are you?";
    }
  }
  
  public static void main(String[] args) throws MalformedURLException {
    ClientRequestProcessor handler = new ClientRequestProcessor("http://127.0.0.1:8080/rpc");
    TestClient.HelloProcessor client = handler.createRemoteObject(TestClient.HelloProcessor.class, new UUID(0L, 1L));
    System.out.println(client.hello());
    System.out.println(client.hello("Khogen"));
  }
}
