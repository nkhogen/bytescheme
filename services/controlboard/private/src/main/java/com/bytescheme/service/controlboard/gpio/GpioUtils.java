package com.bytescheme.service.controlboard.gpio;

import java.lang.reflect.Field;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Util class to get the Raspberry pi Pin field using the pin number.
 * @see <a href="http://pi4j.com/example/control.html">http://pi4j.com/example/control.html</a>
 * @author Naorem Khogendro Singh
 *
 */
public final class GpioUtils {
  private static final String PIN_FORMAT = "GPIO_%02d";

  private GpioUtils() {
  }

  public static Pin getRaspiPin(int num) {
    try {
      Field field = RaspiPin.class.getField(String.format(PIN_FORMAT, num));
      return (Pin) field.get(null);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to get raspberry pin " + num);
    }
  }
}
