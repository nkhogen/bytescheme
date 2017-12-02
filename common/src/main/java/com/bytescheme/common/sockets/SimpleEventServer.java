package com.bytescheme.common.sockets;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple event server for small number of clients.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class SimpleEventServer {
  private static final int SEND_EVENT_RETRY_LIMIT = 3;
  private static final int LOOP_SLEEP_TIME_SEC = 1;
  private final Logger LOG = LoggerFactory.getLogger(SimpleEventServer.class);
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Map<UUID, SocketInfo> socketInfoMap = Collections.synchronizedMap(new HashMap<>());
  private final int port;

  private ServerSocket serverSocket;

  private volatile boolean isRunning = true;

  // Immutable socket information holder
  static class SocketInfo implements Closeable {
    final Socket socket;
    final PrintWriter writer;
    final Scanner scanner;

    public SocketInfo(Socket socket) throws IOException {
      this.socket = Objects.requireNonNull(socket);
      this.writer = new PrintWriter(socket.getOutputStream(), true);
      this.scanner = new Scanner(socket.getInputStream());
    }

    public boolean isClosed() {
      return socket.isClosed();
    }

    public synchronized void close() {
      IOUtils.closeQuietly(socket);
      IOUtils.closeQuietly(writer);
      IOUtils.closeQuietly(scanner);
    }
  }

  public SimpleEventServer(int port) throws IOException {
    this.port = port;
  }

  public void start() throws IOException {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
    serverSocket = new ServerSocket(port);
    executor.submit(() -> {
      while (isRunning) {
        try {
          UUID[] keys = socketInfoMap.keySet().toArray(new UUID[0]);
          for (UUID key : keys) {
            SocketInfo socketInfo = socketInfoMap.get(key);
            if (socketInfo == null || socketInfo.isClosed()) {
              LOG.info("Client dropped connection");
              socketInfoMap.remove(key, socketInfo);
            }
          }
          while (socketInfoMap.isEmpty()) {
            synchronized (socketInfoMap) {
              try {
                socketInfoMap.wait();
              } catch (InterruptedException e) {
              }
            }
          }
        } catch (Exception e) {
          LOG.error("Exception in socket cleaner thread", e);
        } finally {
          try {
            TimeUnit.SECONDS.sleep(LOOP_SLEEP_TIME_SEC);
          } catch (InterruptedException e) {
          }
        }
      }
    });
    executor.submit(() -> {
      while (isRunning) {
        try {
          Socket clientSocket = serverSocket.accept();
          executor.submit(() -> {
            SocketInfo socketInfo = null;
            try {
              socketInfo = new SocketInfo(clientSocket);
              UUID objectId = new UUID(socketInfo.scanner.nextLong(),
                  socketInfo.scanner.nextLong());
              LOG.info("Read object ID {}", objectId);
              socketInfoMap.put(objectId, socketInfo);
              synchronized (socketInfoMap) {
                socketInfoMap.notify();
              }
            } catch (Exception e) {
              LOG.error("Exception while receiving client data", e);
              IOUtils.closeQuietly(socketInfo);
            }
          });
        } catch (Exception e) {
          LOG.error("Exception in server", e);
        }
      }
    });
  }

  public void sendEvent(UUID id, String data) {
    for (int retry = 0; retry < SEND_EVENT_RETRY_LIMIT; retry++) {
      SocketInfo socketInfo = socketInfoMap.get(id);
      if (socketInfo == null || socketInfo.isClosed()) {
        socketInfoMap.remove(id, socketInfo);
        try {
          TimeUnit.SECONDS.sleep(LOOP_SLEEP_TIME_SEC);
        } catch (InterruptedException e) {
        }
        continue;
      }
      try {
        socketInfo.writer.println(data);
        break;
      } catch (Exception e) {
        LOG.error("Error sending data to client ", e);
        socketInfoMap.remove(id, socketInfo);
      }
    }
  }

  public void shutdown() {
    isRunning = false;
    socketInfoMap.clear();
    IOUtils.closeQuietly(serverSocket);
    executor.shutdown();
    try {
      executor.awaitTermination(1, TimeUnit.HOURS);
    } catch (InterruptedException e) {
    }
  }
}
