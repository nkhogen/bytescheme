package com.bytescheme.proto.grpc;

import java.util.Arrays;

import com.bytescheme.rpc.core.ClientRequestHandler;
import com.bytescheme.rpc.core.Constants;
import com.bytescheme.rpc.core.LoginCallRequest;
import com.bytescheme.rpc.core.LogoutCallRequest;
import com.bytescheme.rpc.core.MessageCodec;
import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.core.MethodCallResponse;
import com.bytescheme.rpc.core.RemoteCallRequest;
import com.bytescheme.rpc.core.RemoteMethodCallException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class GrpcClientRequestHandler implements ClientRequestHandler {
  private final ManagedChannel channel;
  private final ServiceGrpc.ServiceBlockingStub blockingStub;

  public GrpcClientRequestHandler(String host, int port) {
    // Just a demo with plaintext.
    channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
    blockingStub = ServiceGrpc.newBlockingStub(channel);
  }

  @Override
  public MethodCallResponse invoke(RemoteCallRequest request, MessageCodec messageCodec) {
    MethodCallResponse callResponse = new MethodCallResponse();
    try {
      if (request instanceof LoginCallRequest) {
        LoginCallRequest loginCallRequest = (LoginCallRequest) request;
        com.bytescheme.proto.grpc.LoginCallRequest grpcRequest = com.bytescheme.proto.grpc.LoginCallRequest
            .newBuilder().setUser(loginCallRequest.getUser())
            .setPassword(loginCallRequest.getPassword())
            .setRequestId(loginCallRequest.getRequestId().toString()).build();
        com.bytescheme.proto.grpc.MethodCallResponse grpcResponse = blockingStub.login(grpcRequest);
        callResponse.setReturnValue(grpcResponse.getReturnValue());
      } else if (request instanceof LogoutCallRequest) {
        LogoutCallRequest logoutCallRequest = (LogoutCallRequest) request;
        com.bytescheme.proto.grpc.LogoutCallRequest.Builder grpcRequestBuilder = com.bytescheme.proto.grpc.LogoutCallRequest
            .newBuilder().setRequestId(logoutCallRequest.getRequestId().toString());
        if (logoutCallRequest.getSessionId() != null) {
          grpcRequestBuilder.setSessionId(logoutCallRequest.getSessionId());
        }
        com.bytescheme.proto.grpc.MethodCallResponse grpcResponse = blockingStub
            .logout(grpcRequestBuilder.build());
        callResponse.setReturnValue(grpcResponse.getReturnValue());
      } else if (request instanceof MethodCallRequest) {
        MethodCallRequest methodCallRequest = (MethodCallRequest) request;
        com.bytescheme.proto.grpc.MethodCallRequest.Builder grpcRequestBuilder = com.bytescheme.proto.grpc.MethodCallRequest
            .newBuilder().setRequestId(methodCallRequest.getRequestId().toString())
            .setObjectId(methodCallRequest.getObjectId().toString())
            .setName(methodCallRequest.getName());
        if (methodCallRequest.getSessionId() != null) {
          grpcRequestBuilder.setSessionId(methodCallRequest.getSessionId());
        }
        if (methodCallRequest.getParameters() != null) {
          grpcRequestBuilder
              .addAllParameters(Arrays.<String>asList(methodCallRequest.getParameters()));
        }
        com.bytescheme.proto.grpc.MethodCallResponse grpcResponse = blockingStub
            .invoke(grpcRequestBuilder.build());
        callResponse.setReturnValue(grpcResponse.getReturnValue());
      } else {
        throw new RemoteMethodCallException(Constants.CLIENT_ERROR_CODE, "Unknown request object ");
      }
    } catch (StatusRuntimeException e) {
      callResponse.setException(
          new RemoteMethodCallException(e.getStatus().getCode().ordinal(), e.getMessage()));
    }
    return callResponse;
  }

}
