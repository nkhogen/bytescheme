package com.bytescheme.rpc.core;

import java.io.Serializable;
import java.util.UUID;

public interface RemoteCallRequest extends Serializable {
  UUID getRequestId();
}
