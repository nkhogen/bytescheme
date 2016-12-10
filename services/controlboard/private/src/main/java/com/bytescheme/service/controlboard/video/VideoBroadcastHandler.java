package com.bytescheme.service.controlboard.video;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Vector;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utilities.CommandExecutor;
import com.bytescheme.common.utilities.CommandInfoReader;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;

/**
 * Handles video broadcast. Converted to Java from JS
 * {@link https://github.com/phoboslab/jsmpeg}. Also, it handles lifecycle of
 * ffmpeg command. If no client is connected, it is shutdown.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class VideoBroadcastHandler {
  private static final Logger LOG = LoggerFactory.getLogger(VideoBroadcastHandler.class);
  private Vector<Session> sessions = new Vector<Session>();
  private static volatile VideoBroadcastHandler INSTANCE;
  private static final String STREAM_MAGIC_BYTES = "jsmp";
  private short width = 320;
  private short height = 240;
  private final CommandExecutor executor = new CommandExecutor();

  private VideoBroadcastHandler() {
  }

  public static VideoBroadcastHandler getInstance() {
    if (INSTANCE != null) {
      return INSTANCE;
    }
    synchronized (VideoBroadcastHandler.class) {
      if (INSTANCE != null) {
        return INSTANCE;
      }
      INSTANCE = new VideoBroadcastHandler();
    }
    return INSTANCE;
  }

  public void registerConnection(Session session) throws IOException {
    Preconditions.checkNotNull(session, "Invalid websocket session");
    synchronized (sessions) {
      if (sessions.size() == 0 || executor.getProcessCount() == 0) {
        start(false);
      }
      sessions.add(session);
    }
    try {
      Basic basic = session.getBasicRemote();
      ByteBuffer byteBuffer = ByteBuffer.allocate(8);
      byteBuffer.order(ByteOrder.BIG_ENDIAN);
      byteBuffer.put(STREAM_MAGIC_BYTES.getBytes("UTF-8"));
      byteBuffer.position(4);
      byteBuffer.putShort(4, width);
      byteBuffer.position(6);
      byteBuffer.putShort(6, height);
      byteBuffer.position(8);
      byteBuffer.flip();
      basic.sendBinary(byteBuffer);
    } catch (Exception e) {
      LOG.error("Failed to register the client", e);
      unregisterConnection(session);
    }
  }

  public void unregisterConnection(Session session) {
    Preconditions.checkNotNull(session, "Invalid websocket session");
    synchronized (sessions) {
      sessions.remove(session);
      if (sessions.isEmpty()) {
        executor.destroyProcesses();
      }
    }
  }

  public void broadcast(InputStream inputStream, short width, short height)
      throws IOException {
    Preconditions.checkNotNull(inputStream, "Invalid input stream");
    this.width = width;
    this.height = height;
    int readLen = 0;
    byte[] bytes = new byte[4096];
    InputStream stream = new BufferedInputStream(inputStream, 8192);
    while ((readLen = stream.read(bytes)) != -1) {
      Enumeration<Session> enumeration = sessions.elements();
      while (enumeration.hasMoreElements()) {
        Session session = enumeration.nextElement();
        if (session.isOpen()) {
          ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, readLen);
          byteBuffer.position(readLen);
          byteBuffer.flip();
          session.getBasicRemote().sendBinary(byteBuffer);
        }
      }
    }
  }

  private void start(boolean isWait) {
    CommandInfoReader reader;
    int processCount = executor.getProcessCount();
    if (processCount > 0) {
      throw new IllegalStateException(
          String.format("Some processes (%d) are already running", processCount));
    }
    try {
      reader = new CommandInfoReader(
          new FileInputStream("src/main/resources/commands.json"));
      executor.waitExecute(reader, isWait);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to start video streaming", e);
    }
  }
}
