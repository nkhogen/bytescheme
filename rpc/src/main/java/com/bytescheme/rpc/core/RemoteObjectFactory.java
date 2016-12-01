package com.bytescheme.rpc.core;

import java.util.UUID;
/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public interface RemoteObjectFactory {
  <T extends RemoteObject> T createRemoteObject (Class<T> clazz, UUID objectId);
}
