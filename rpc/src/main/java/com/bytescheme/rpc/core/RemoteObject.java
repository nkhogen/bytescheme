package com.bytescheme.rpc.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * An interface identifying a remote object.
 *
 * @author Naorem Khogendro Singh
 *
 */
public interface RemoteObject extends Serializable {
  UUID getObjectId();
}
