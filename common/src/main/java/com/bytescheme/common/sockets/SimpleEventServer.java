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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Simple event server for small number of clients.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class SimpleEventServer {
  private static final int SEND_EVENT_RETRY_LIMIT = 3;
  private static final int LOOP_SLEEP_TIME_MS = 400;
  private static final int SEND_EVENT_SLEEP_TIME_MS = 100;
  private final Logger LOG = LoggerFactory.getLogger(SimpleEventServer.class);
  private final ExecutorService executor = Executors
      .newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true)
          .setNameFormat("SimpleEventServer-thread-%d").build());
  private final Map<Integer, SocketInfo> socketInfoMap = Collections
      .synchronizedMap(new HashMap<>());
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

    public boolean isDisconnected() {
      return !socket.isConnected();
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
          Integer[] keys = socketInfoMap.keySet().toArray(new Integer[0]);
          for (int key : keys) {
            SocketInfo socketInfo = socketInfoMap.get(key);
            if (socketInfo == null || socketInfo.isClosed()
                || socketInfo.isDisconnected()) {
              LOG.info("Client dropped connection");
              socketInfoMap.remove(key, socketInfo);
              IOUtils.closeQuietly(socketInfo);
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
          sleep(TimeUnit.MILLISECONDS, LOOP_SLEEP_TIME_MS);
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
              // nextInt does not consume \n
              int clientId = Integer.parseInt(socketInfo.scanner.nextLine());
              LOG.info("Read client ID {}", clientId);
              socketInfoMap.put(clientId, socketInfo);
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

  public String sendEvent(int id, String data) {
    for (int retry = 0; retry < SEND_EVENT_RETRY_LIMIT; retry++) {
      SocketInfo socketInfo = socketInfoMap.get(id);
      if (socketInfo == null || socketInfo.isClosed()) {
        LOG.info("Removing socket for client {}", id);
        socketInfoMap.remove(id, socketInfo);
        IOUtils.closeQuietly(socketInfo);
        sleep(TimeUnit.MILLISECONDS, LOOP_SLEEP_TIME_MS);
        continue;
      }
      try {
        socketInfo.writer.println(data);
        sleep(TimeUnit.MILLISECONDS, SEND_EVENT_SLEEP_TIME_MS);
        // Warning: blocking call
        return socketInfo.scanner.hasNextLine() ? socketInfo.scanner.nextLine() : null;
      } catch (Exception e) {
        LOG.error("Error sending data to client ", e);
        socketInfoMap.remove(id, socketInfo);
        IOUtils.closeQuietly(socketInfo);
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Rety limit exceeded");
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

  private void sleep(TimeUnit timeUnit, int duration) {
    try {
      Objects.requireNonNull(timeUnit).sleep(duration);
    } catch (InterruptedException e) {
    }
  }
}
