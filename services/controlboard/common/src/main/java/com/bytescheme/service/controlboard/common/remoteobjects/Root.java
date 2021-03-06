package com.bytescheme.service.controlboard.common.remoteobjects;

import com.bytescheme.rpc.core.RemoteObject;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;

/**
 * Root class is the entry for the client calls. It has a well-known object ID
 * that is known to all the clients.
 *
 * @author Naorem Khogendro Singh
 *
 */
public interface Root extends RemoteObject {

	ControlBoard getControlBoard();

	DeviceEventScheduler getDeviceEventScheduler();
}
