package com.bytescheme.service.controlboard.video;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.bytescheme.common.utilities.CommandExecutor;
import com.bytescheme.common.utilities.CommandInfoReader;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

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
  private static final long UNCLAIMED_SECRET_LIFE = 60000L;
  private BiMap<String, Session> sessions = HashBiMap.create();
  private DelayQueue<Secret> secrets = new DelayQueue<>();
  private static volatile VideoBroadcastHandler INSTANCE;
  private static final String STREAM_MAGIC_BYTES = "jsmp";
  private String commandFile;
  private short width = 320;
  private short height = 240;
  private final CommandExecutor executor = new CommandExecutor();
  private final ExecutorService executorService;

  private VideoBroadcastHandler() {
    this.executorService = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      return thread;
    });
    this.executorService.submit(() -> {
      while (true) {
        secrets.take();
        LOG.info("Expired unclaimed secret");
      }
    });
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

  public String getCommandFile() {
    return commandFile;
  }

  public void setCommandFile(String commandFile) {
    this.commandFile = commandFile;
  }

  public void registerConnection(String secret, Session session) throws IOException {
    Preconditions.checkArgument(!StringUtils.isEmpty(secret), "Invalid secret ID");
    Preconditions.checkNotNull(session, "Invalid websocket session");
    synchronized (sessions) {
      if (sessions.size() == 0 || executor.getProcessCount() == 0) {
        start(false);
      }
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
      sessions.put(secret, session);
    }
  }

  public void unregisterConnection(Session session) {
    Preconditions.checkNotNull(session, "Invalid websocket session");
    synchronized (sessions) {
      String secret = sessions.inverse().remove(session);
      if (secret != null) {
        // There could be connection breaks. Reclaim the secret and keep it for
        // some time.
        secrets
            .add(new Secret(secret, System.currentTimeMillis() + UNCLAIMED_SECRET_LIFE));
      }
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
      Session[] sessionArray = null;
      synchronized (sessions) {
        sessionArray = sessions.values().toArray(new Session[0]);
      }
      for (Session session : sessionArray) {
        if (session.isOpen()) {
          ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, readLen);
          byteBuffer.position(readLen);
          byteBuffer.flip();
          session.getBasicRemote().sendBinary(byteBuffer);
        }
      }
    }
  }

  public String generateSecret() {
    String secret = createSecret();
    secrets.add(new Secret(secret, System.currentTimeMillis() + UNCLAIMED_SECRET_LIFE));
    return secret;

  }

  public boolean isValidSecret(String secret, Session session) {
    Session currSession = sessions.get(secret);
    if (currSession != null) {
      return (session == currSession);
    }
    if (secrets.contains(new Secret(secret, 0L))) {
      return true;
    }
    return false;
  }

  protected String createSecret() {
    return UUID.randomUUID().toString();
  }

  private void start(boolean isWait) {
    int processCount = executor.getProcessCount();
    if (processCount > 0) {
      throw new IllegalStateException(
          String.format("Some processes (%d) are already running", processCount));
    }
    CommandInfoReader reader = null;
    try {
      reader = new CommandInfoReader(new FileInputStream(commandFile));
      executor.waitExecute(reader, isWait);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to start video streaming", e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
