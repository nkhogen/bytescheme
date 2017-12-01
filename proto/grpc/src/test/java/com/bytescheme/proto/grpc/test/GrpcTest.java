package com.bytescheme.proto.grpc.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bytescheme.proto.grpc.GrpcClientRequestHandler;
import com.bytescheme.proto.grpc.GrpcRemoteObjectServer;
import com.bytescheme.rpc.core.RemoteObject;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.rpc.core.RemoteObjectServer;

public class GrpcTest {

  private GrpcRemoteObjectServer server;

  public static interface MyService extends RemoteObject {
    String hello();
  }

  public static class MyServiceImpl implements MyService {
    private static final long serialVersionUID = 1L;
    private UUID objectId;

    public MyServiceImpl(UUID objectId) {
      this.objectId = objectId;
    }

    @Override
    public UUID getObjectId() {
      return objectId;
    }

    @Override
    public String hello() {
      return "Hello!How are you?";
    }

  }

  @Before
  public void setup() throws IOException {
    RemoteObjectServer objectServer = new RemoteObjectServer();
    objectServer.register(new MyServiceImpl(new UUID(0L, 1L)));
    server = new GrpcRemoteObjectServer(objectServer);
    server.start(9990);
  }

  @After
  public void teardown() {
    server.shutdown();
  }

  @Test
  public void test() {
    GrpcClientRequestHandler client = new GrpcClientRequestHandler("localhost", 9990);
    RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder(client);
    RemoteObjectClient objectClient = clientBuilder.login("dummy", "dummy");
    MyService myService = objectClient.createRemoteObject(MyService.class, new UUID(0L, 1L));
    String message = myService.hello();
    assertEquals(message, new MyServiceImpl(new UUID(0L, 1L)).hello());
    System.out.println(message);
  }
}
