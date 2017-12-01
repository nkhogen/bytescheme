package com.bytescheme.proto.grpc;

import java.io.IOException;
import java.util.UUID;

import com.bytescheme.rpc.core.LoginCallRequest;
import com.bytescheme.rpc.core.LogoutCallRequest;
import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.core.MethodCallResponse;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.google.common.base.Preconditions;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;

public class GrpcRemoteObjectServer extends ServiceGrpc.ServiceImplBase {

  private final RemoteObjectServer remoteObjectServer;
  private Server server;

  public GrpcRemoteObjectServer(RemoteObjectServer remoteObjectServer) {
    Preconditions.checkNotNull(remoteObjectServer);
    this.remoteObjectServer = remoteObjectServer;
  }

  @Override
  public void login(com.bytescheme.proto.grpc.LoginCallRequest request,
      io.grpc.stub.StreamObserver<com.bytescheme.proto.grpc.MethodCallResponse> responseObserver) {
    LoginCallRequest loginCallRequest = new LoginCallRequest();
    loginCallRequest.setUser(request.getUser());
    loginCallRequest.setPassword(request.getPassword());
    loginCallRequest.setRequestId(UUID.fromString(request.getRequestId()));
    MethodCallResponse methodCallResponse = remoteObjectServer.login(loginCallRequest);
    RemoteMethodCallException exception = methodCallResponse.getException();
    if (exception != null) {
      Status.INTERNAL.withDescription(exception.getMessage()).withCause(exception)
          .asRuntimeException();
    }
    com.bytescheme.proto.grpc.MethodCallResponse.Builder responseBuilder = com.bytescheme.proto.grpc.MethodCallResponse
        .newBuilder();
    if (methodCallResponse.getReturnValue() != null) {
      responseBuilder.setReturnValue(methodCallResponse.getReturnValue());
    }
    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void logout(com.bytescheme.proto.grpc.LogoutCallRequest request,
      io.grpc.stub.StreamObserver<com.bytescheme.proto.grpc.MethodCallResponse> responseObserver) {
    LogoutCallRequest logoutCallRequest = new LogoutCallRequest();
    logoutCallRequest.setSessionId(request.getSessionId());
    logoutCallRequest.setRequestId(UUID.fromString(request.getRequestId()));
    MethodCallResponse methodCallResponse = remoteObjectServer.logout(logoutCallRequest);
    RemoteMethodCallException exception = methodCallResponse.getException();
    if (exception != null) {
      Status.INTERNAL.withDescription(exception.getMessage()).withCause(exception)
          .asRuntimeException();
    }
    com.bytescheme.proto.grpc.MethodCallResponse.Builder responseBuilder = com.bytescheme.proto.grpc.MethodCallResponse
        .newBuilder();
    if (methodCallResponse.getReturnValue() != null) {
      responseBuilder.setReturnValue(methodCallResponse.getReturnValue());
    }
    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void invoke(com.bytescheme.proto.grpc.MethodCallRequest request,
      io.grpc.stub.StreamObserver<com.bytescheme.proto.grpc.MethodCallResponse> responseObserver) {
    MethodCallRequest methodCallRequest = new MethodCallRequest();
    methodCallRequest.setObjectId(UUID.fromString(request.getObjectId()));
    methodCallRequest.setName(request.getName());
    methodCallRequest.setParameters(request.getParametersList().toArray(new String[0]));
    methodCallRequest.setSessionId(request.getSessionId());
    methodCallRequest.setRequestId(UUID.fromString(request.getRequestId()));
    MethodCallResponse methodCallResponse = remoteObjectServer.process(methodCallRequest);
    RemoteMethodCallException exception = methodCallResponse.getException();
    if (exception != null) {
      throw Status.INTERNAL.withDescription(exception.getMessage()).withCause(exception)
          .asRuntimeException();
    }
    com.bytescheme.proto.grpc.MethodCallResponse.Builder responseBuilder = com.bytescheme.proto.grpc.MethodCallResponse
        .newBuilder();
    if (methodCallResponse.getReturnValue() != null) {
      responseBuilder.setReturnValue(methodCallResponse.getReturnValue());
    }
    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  public void start(int port) throws IOException {
    server = ServerBuilder.forPort(port).addService(this).build().start();
  }

  public void shutdown() {
    if (server != null) {
      server.shutdown();
      try {
        server.awaitTermination();
      } catch (InterruptedException e) {
      }
    }
  }

}
