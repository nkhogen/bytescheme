package com.bytescheme.service.controlboard.common.remoteobjects;

import java.util.List;

import com.bytescheme.rpc.core.RemoteObject;
import com.bytescheme.service.controlboard.common.models.DeviceStatus;

/**
 * @author Naorem Khogendro Singh
 *
 */
public interface ControlBoard extends RemoteObject {
	List<DeviceStatus> listDevices();

	DeviceStatus changePowerStatus(DeviceStatus status);
}
