package com.bytescheme.rpc.core;

/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public final class Constants {
  private Constants() {
  }

  public static final String METHOD_ENTER_LOG_FORMAT = "REQUEST ID: {}, EVENT: ENTER, METHOD: {}, ELAPSED TIME: {}ms";
  public static final String METHOD_EXIT_LOG_FORMAT = "REQUEST ID: {}, EVENT: EXIT, METHOD: {}, ELAPSED TIME: {}ms, METHOD TIME: {}ms";

  /* Exception codes */
  private static final int BASE_ERROR_CODE = 100;
  public static final int CLIENT_ERROR_CODE = BASE_ERROR_CODE + 1;
  public static final int SERVER_ERROR_CODE = BASE_ERROR_CODE + 2;
  public static final int AUTHENTICATION_ERROR_CODE = BASE_ERROR_CODE + 3;
  public static final int AUTHORIZATION_ERROR_CODE = BASE_ERROR_CODE + 4;

}
