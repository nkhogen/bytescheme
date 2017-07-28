package com.bytescheme.service.controlboard;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.User;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SimpleCard;
import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;

/**
 * Alexa main controller.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class Controller implements Speechlet {
  private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
  private static final String ENDPOINT = "https://controller.bytescheme.com/rpc";
  private static final UUID OBJECT_ID = new UUID(0L, 0L);

  private final RemoteObjectClientBuilder clientBuilder;

  public Controller() {
    try {
      this.clientBuilder = new RemoteObjectClientBuilder(ENDPOINT);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SpeechletResponse onIntent(IntentRequest request, Session session)
      throws SpeechletException {
    LOG.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
        session.getSessionId());

    Intent intent = request.getIntent();
    String intentName = (intent != null) ? intent.getName() : null;
    User user = session.getUser();
    String userId = user.getUserId();
    LOG.info("User ID {}", userId);
    RemoteObjectClient client = null;
    String password = null;
    try {
      password = CryptoUtils.kmsEncrypt(userId);
    } catch (Exception e) {
      return sendSpeechResponse("Key encryption failed " + e.getMessage());
    }
    try {
      client = clientBuilder.login(userId, password);
    } catch (Exception e) {
      LOG.info("Exception occurred in login", e);
      return sendSpeechResponse("Login failed " + e.getMessage());
    }
    if (client == null) {
      return sendSpeechResponse("Login failed");
    }
    try {
      if ("SetStatus".equals(intentName)) {
        Slot deviceSlot = intent.getSlot("DEVICE");
        Slot statusSlot = intent.getSlot("STATUS");
        Root root = client.createRemoteObject(Root.class, OBJECT_ID);
        ControlBoard controlBoard = root.getControlBoard(userId);
        if (controlBoard == null) {
          return sendSpeechResponse("No controlboard found for the user");
        }
        List<DeviceStatus> devices = controlBoard.listDevices();
        DeviceStatus targetDevice = null;
        for (DeviceStatus deviceStatus : devices) {
          if (deviceStatus.getTag().equalsIgnoreCase(deviceSlot.getValue())) {
            targetDevice = deviceStatus;
            break;
          }
        }
        if (targetDevice == null) {
          return sendSpeechResponse("No target device found for the user");
        }
        boolean deviceStatus = "ON".equalsIgnoreCase(statusSlot.getValue());
        if (deviceStatus != targetDevice.isPowerOn()) {
          targetDevice.setPowerOn(deviceStatus);
          controlBoard.changePowerStatus(targetDevice);
        }
        return sendSpeechResponse("Device status is now " + deviceStatus);
      } else {
        throw new SpeechletException("Invalid Intent");
      }
    } finally {
      client.logout();
    }
  }

  @Override
  public SpeechletResponse onLaunch(LaunchRequest request, Session session)
      throws SpeechletException {
    return sendSpeechResponse("Logged in successfully");
  }

  @Override
  public void onSessionEnded(SessionEndedRequest sessionEndedRequest, Session session)
      throws SpeechletException {
  }

  @Override
  public void onSessionStarted(SessionStartedRequest sessionStartedRequest,
      Session session) throws SpeechletException {
  }

  private SpeechletResponse sendSpeechResponse(String text) {
    // Create the Simple card content.
    SimpleCard card = new SimpleCard();
    card.setTitle(text);
    card.setContent(text);
    // Create the plain text output.
    PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
    speech.setText(text);
    return SpeechletResponse.newTellResponse(speech, card);
  }
}
