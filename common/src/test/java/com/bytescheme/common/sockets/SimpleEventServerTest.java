package com.bytescheme.common.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleEventServerTest {

  private static final int PORT = 9999;
  private static final UUID OBJECT_ID = UUID.randomUUID();

  public static void sendReceiveRequest() throws UnknownHostException, IOException {

    while (true) {
      System.out.println("Connecting to server ...");
      try (Socket socket = new Socket("127.0.0.1", PORT);
          PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
          BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
        System.out.println("Sending object ID to server: " + OBJECT_ID);
        out.println(OBJECT_ID.getMostSignificantBits());
        out.println(OBJECT_ID.getLeastSignificantBits());
        while (!socket.isClosed()) {
          String line = in.readLine();
          if (line == null) {
            System.out.println("Waiting for events");
            try {
              TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
            }
            continue;
          }
          System.out.println("Received events from server: " + line);
          out.println(2);
          out.println("Hello from client");
          out.println("Good morning! " + line);
          out.close();
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    SimpleEventServer server = new SimpleEventServer(PORT);
    server.start();
    Thread.sleep(1000L);
    Thread clientThread = new Thread(() -> {
      try {
        sendReceiveRequest();
      } catch (UnknownHostException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    clientThread.start();
    for (int i = 0; i < 100; i++) {
      String reply = server.sendEvent(OBJECT_ID, "Hello " + i);
      System.out.println("Received from client: " + reply);
      TimeUnit.SECONDS.sleep(10);
    }
    clientThread.join();
  }
}
