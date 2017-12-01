package com.bytescheme.rpc.core;

@FunctionalInterface
public interface ClientRequestHandler {
  MethodCallResponse invoke(RemoteCallRequest request, MessageCodec messageCodec);
}
