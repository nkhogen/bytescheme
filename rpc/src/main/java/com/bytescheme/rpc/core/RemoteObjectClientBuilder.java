package com.bytescheme.rpc.core;

import java.lang.reflect.Proxy;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class is used to create remote proxy object for a remote object
 * interface and an object ID. Authentication is supported and the session
 * management is done under the hood.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class RemoteObjectClientBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteObjectClientBuilder.class);
  private static final MessageCodec DEFAULT_MESSAGE_CODEC = new MessageCodec(null, null);

  private final ClientRequestHandler clientRequestHandler;

  /**
   * Constructs an instance with the request handler.
   *
   * @param clientRequestHandler
   */
  public RemoteObjectClientBuilder(ClientRequestHandler clientRequestHandler) {
    this.clientRequestHandler = Preconditions.checkNotNull(clientRequestHandler);
  }

  public RemoteObjectClient login(String user, String password) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "Invalid user");
    LoginCallRequest request = new LoginCallRequest();
    request.setUser(user);
    request.setPassword(password);
    request.setRequestId(UUID.randomUUID());
    LOG.info("Invoking login method {} with user {} and request ID {}", request.getUser(),
        request.getRequestId());
    String sessionId = invokeRemoteMethod(request, String.class, DEFAULT_MESSAGE_CODEC);
    return new ClientRemoteObjectFactory(sessionId);
  }

  private <T, R extends RemoteCallRequest> T invokeRemoteMethod(R request, Class<T> responseClass,
      MessageCodec messageCodec) {
    try {
      MethodCallResponse response = clientRequestHandler.invoke(request, messageCodec);
      if (response.getException() != null) {
        LOG.error("Error occurred in method call", response.getException());
        throw response.getException();
      }
      return messageCodec.getObject(response.getReturnValue(), responseClass);
    } catch (RemoteMethodCallException e) {
      throw e;
    } catch (Exception e) {
      throw new RemoteMethodCallException(Constants.CLIENT_ERROR_CODE, "Error occurred in client",
          e);
    }
  }

  private class ClientRemoteObjectFactory implements RemoteObjectClient {
    private final String sessionId;
    private final MessageCodec messageCodec = new MessageCodec(this, null);

    ClientRemoteObjectFactory(String sessionId) {
      this.sessionId = sessionId;
    }

    @Override
    public <T extends RemoteObject> T createRemoteObject(Class<T> clazz, UUID objectId) {
      return clazz.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
          new Class<?>[] { clazz }, (proxy, method, args) -> {
            if (method.getName().equals("getObjectId") && method.getParameterCount() == 0) {
              return objectId;
            }
            if (method.getName().equals("equals") && method.getParameterCount() == 1) {
              if (args[0] == null) {
                return false;
              }
              return System.identityHashCode(this) == System.identityHashCode(args[0]);
            }
            if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
              return System.identityHashCode(this);
            }
            String[] jsonParameters = null;
            int parameterCount = args == null ? 0 : args.length;
            MethodCallRequest request = new MethodCallRequest();
            request.setName(method.getName());
            request.setObjectId(objectId);
            request.setRequestId(UUID.randomUUID());
            request.setSessionId(sessionId);
            if (args != null) {
              int index = 0;
              jsonParameters = new String[args.length];
              for (Object arg : args) {
                jsonParameters[index++] = messageCodec.getJson(arg);
              }
            }
            request.setParameters(jsonParameters);
            LOG.info(
                "Invoking remote method {}, parameter count {} on object ID {} with request ID {}",
                request.getName(), parameterCount, objectId, request.getRequestId());
            return invokeRemoteMethod(request, method.getReturnType(), messageCodec);
          }));
    }

    @Override
    public void logout() {
      LogoutCallRequest request = new LogoutCallRequest();
      request.setSessionId(sessionId);
      request.setRequestId(UUID.randomUUID());
      LOG.info("Invoking logout method {} with session ID %s and request ID {}",
          request.getSessionId(), request.getRequestId());
      invokeRemoteMethod(request, String.class, DEFAULT_MESSAGE_CODEC);
    }
  }
}
