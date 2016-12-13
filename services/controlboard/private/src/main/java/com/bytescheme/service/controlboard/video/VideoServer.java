package com.bytescheme.service.controlboard.video;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Websocket server to accept client connections.
 *
 * @author Naorem Khogendro Singh
 *
 */
@ServerEndpoint(value = "/video/{secret}", subprotocols = { "stream", "video" })
public class VideoServer {
  private static final Logger LOG = LoggerFactory.getLogger(VideoServer.class);

  public VideoServer() {
  }

  @OnOpen
  public void handleConnection(EndpointConfig config, Session session,
      @PathParam("secret") String secret) throws IOException {
    LOG.info("Connected client {}", session.toString());
    if (VideoBroadcastHandler.getInstance().isValidSecret(secret, session)) {
      LOG.info("Secret recognized");
      VideoBroadcastHandler.getInstance().registerConnection(secret, session);
    } else {
      LOG.info("Unrecognized secret");
      session.close(new CloseReason(CloseCodes.VIOLATED_POLICY, "Invalid secret"));
    }
  }

  @OnMessage
  public void processBinary(String message, Session session,
      @PathParam("secretId") String secretId) {
    LOG.info("Message received from client {}", session.toString());
  }

  @OnClose
  public void handleClose(CloseReason reason, Session session) {
    VideoBroadcastHandler.getInstance().unregisterConnection(session);
    LOG.info("Disconnected client {}", session.toString());
  }
}
