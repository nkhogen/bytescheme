package com.bytescheme.service.controlboard.common.models;

import java.io.Serializable;

import com.bytescheme.common.utils.JsonUtils;

/**
 * Model for device status.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceStatus implements Serializable {
	private static final long serialVersionUID = 1L;
	private int pin;
	private String tag;
	private boolean powerOn;

	public int getPin() {
		return pin;
	}

	public void setPin(int pin) {
		this.pin = pin;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public boolean isPowerOn() {
		return powerOn;
	}

	public void setPowerOn(boolean powerOn) {
		this.powerOn = powerOn;
	}

	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
}
