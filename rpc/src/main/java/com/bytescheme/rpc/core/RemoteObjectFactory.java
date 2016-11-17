package com.bytescheme.rpc.core;

import java.util.UUID;

public interface RemoteObjectFactory {
  <T extends RemoteObject> T createRemoteObject (Class<T> clazz, UUID objectId);
}
