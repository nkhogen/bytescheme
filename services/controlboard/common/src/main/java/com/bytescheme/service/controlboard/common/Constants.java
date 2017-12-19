package com.bytescheme.service.controlboard.common;

import java.util.UUID;

/**
 * Common constants for Controlboard.
 *
 * @author Naorem Khogendro Singh
 *
 */
public final class Constants {
  public static final UUID ROOT_OBJECT_ID = new UUID(0L, 0L);
  public static final String PUBLIC_ENDPOINT = "https://controller.bytescheme.com/rpc";

  private Constants() {
  }
}
