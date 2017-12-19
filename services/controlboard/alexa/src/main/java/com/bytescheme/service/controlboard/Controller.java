package com.bytescheme.service.controlboard;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

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
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.Constants;
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

  private final RemoteObjectClientBuilder clientBuilder;

  public Controller() {
    try {
      this.clientBuilder = new RemoteObjectClientBuilder(
          new HttpClientRequestHandler(Constants.PUBLIC_ENDPOINT));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SpeechletResponse onIntent(IntentRequest request, Session session)
      throws SpeechletException {
    LOG.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

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
      LOG.info("Exception occurred in key encryption", e);
      return sendSpeechResponse("Key encryption failed ");
    }
    try {
      client = Objects.requireNonNull(clientBuilder.login(userId, password));
    } catch (Exception e) {
      LOG.info("Exception occurred in login", e);
      return sendSpeechResponse("Login failed ");
    }
    try {
      if ("SetStatus".equals(intentName)) {
        Slot deviceSlot = intent.getSlot("DEVICE");
        Slot statusSlot = intent.getSlot("STATUS");
        Root root = client.createRemoteObject(Root.class, Root.OBJECT_ID);
        ControlBoard controlBoard = root.getControlBoard();
        if (controlBoard == null) {
          return sendSpeechResponse("No controlboard found for the user");
        }
        List<DeviceStatus> devices = controlBoard.listDevices();
        DeviceStatus targetDevice = findClosestInOrder(devices, deviceSlot.getValue());
        if (targetDevice == null) {
          return sendSpeechResponse(
              String.format("No target device %s found for the user", deviceSlot.getValue()));
        }
        boolean deviceStatus = "ON".equalsIgnoreCase(statusSlot.getValue());
        if (deviceStatus == targetDevice.isPowerOn()) {
          return sendSpeechResponse(
              String
                  .format("%s is already %s", targetDevice.getTag(), deviceStatus ? "ON" : "OFF"));
        }
        targetDevice.setPowerOn(deviceStatus);
        controlBoard.changePowerStatus(targetDevice);
        return sendSpeechResponse(
            String.format("%s is now %s", targetDevice.getTag(), deviceStatus ? "ON" : "OFF"));
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
  public void onSessionStarted(SessionStartedRequest sessionStartedRequest, Session session)
      throws SpeechletException {
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

  // Finds the closest tag match in order.
  private DeviceStatus findClosestInOrder(List<DeviceStatus> devices, String searchWord) {
    int maxMatchCount = 0;
    DeviceStatus targetDevice = null;
    String[] searchTokens = searchWord.split("[ ]");
    for (DeviceStatus device : devices) {
      String[] tagTokens = device.getTag().split("[ ]");
      int tagTokenPos = 0;
      int matchCount = 0;
      for (String searchToken : searchTokens) {
        if (tagTokenPos >= tagTokens.length) {
          break;
        }
        int index = tagTokenPos;
        while (index < tagTokens.length) {
          if (tagTokens[index++].equalsIgnoreCase(searchToken)) {
            matchCount++;
            tagTokenPos = index;
            break;
          }
        }
      }
      if (maxMatchCount < matchCount) {
        maxMatchCount = matchCount;
        targetDevice = device;
      }
    }
    return targetDevice;
  }
}
