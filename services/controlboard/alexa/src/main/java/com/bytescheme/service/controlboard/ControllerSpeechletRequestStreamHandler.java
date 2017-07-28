package com.bytescheme.service.controlboard;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

/**
 * Lambda entry class.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class ControllerSpeechletRequestStreamHandler
    extends SpeechletRequestStreamHandler {
  private static final Set<String> SUPPORTED_APP_IDS = new HashSet<>();
  private static final String APP_ID_ENV = "APP_IDS";
  static {
    String value = System.getenv(APP_ID_ENV);
    if (value != null) {
      String[] tokens = value.split(",");
      for (String token : tokens) {
        SUPPORTED_APP_IDS.add(token.trim());
      }
    }
  }

  public ControllerSpeechletRequestStreamHandler() {
    super(new Controller(), SUPPORTED_APP_IDS);
  }
}
