package com.bytescheme.rpc.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * Remote method call.
 *
 * @author Naorem Khogendro Singh
 *
 */
public interface RemoteCallRequest extends Serializable {
  UUID getRequestId();
}
