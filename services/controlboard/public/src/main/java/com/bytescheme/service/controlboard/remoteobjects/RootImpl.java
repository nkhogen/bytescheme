package com.bytescheme.service.controlboard.remoteobjects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.Constants;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.bytescheme.rpc.core.RemoteObject;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;
import com.bytescheme.service.controlboard.common.remoteobjects.BaseMockControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.bytescheme.service.controlboard.domains.ObjectEndpoint;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

/**
 * Root object implementation.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class RootImpl implements Root {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RootImpl.class);
  private static final int CLIENT_RETRY_LIMIT = 3;
  private static final String TARGET_USER = "controlboard";
  private final Function<String, Set<ObjectEndpoint>> objectEndpointProvider;
  private DeviceEventScheduler deviceEventScheduler;

  private final ConcurrentHashMap<String, RemoteObjectClient> clients = new ConcurrentHashMap<>(
      Collections.emptyMap());
  private boolean enableMock = false;

  public RootImpl(Function<String, Set<ObjectEndpoint>> objectEndpointProvider) {
    this.objectEndpointProvider = Preconditions.checkNotNull(objectEndpointProvider,
        "Invalid object endpoint provider");
  }

  public boolean isEnableMock() {
    return enableMock;
  }

  public void setEnableMock(boolean enableMock) {
    this.enableMock = enableMock;
  }

  @Override
  public UUID getObjectId() {
    return OBJECT_ID;
  }

  @Override
  public ControlBoard getControlBoard(String user) {
    ObjectEndpoint objectEndpoint = getObjectEndPoint(user);
    UUID objectId = UUID.fromString(objectEndpoint.getObjectId());
    try {
      if (enableMock) {
        return new BaseMockControlBoard(objectId);
      }
      ControlBoard remoteControlBoard = createRemoteObject(ControlBoard.class, objectId,
          objectEndpoint.getEndpoint(), user, objectEndpoint.getPublicKey());
      return new DelegateControlBoardImpl(objectId, remoteControlBoard);
    } catch (Exception e) {
      throw new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
          "Failed to create the control board", e);
    }
  }

  @Override
  public DeviceEventScheduler getDeviceEventScheduler() {
    return deviceEventScheduler;
  }

  public void setDeviceEventScheduler(DeviceEventScheduler deviceEventScheduler) {
    this.deviceEventScheduler = deviceEventScheduler;
  }

  private <T extends RemoteObject> T createRemoteObject(Class<T> clazz, UUID objectId,
      String endpoint, String user, PublicKey publicKey) {
    return clazz.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { clazz },
        (proxy, method, args) -> {
          return invokeMethod(clazz, objectId, method, args, endpoint, user, publicKey);
        }));
  }

  /*
   * This method takes care of auto-login in case of session expiry under the
   * hood when a method of the remote object is invoked.
   */
  private <T extends RemoteObject> Object invokeMethod(Class<T> clazz, UUID objectId, Method method,
      Object[] args, String endpoint, String user, PublicKey publicKey) throws MalformedURLException {
    ObjectEndpoint objectEndpoint = getObjectEndPoint(user);
    // Do not allow authenticated users to replace the UUID.
    if (!objectId.toString().equals(objectEndpoint.getObjectId())) {
      throw new RemoteMethodCallException(Constants.AUTHORIZATION_ERROR_CODE, "User is not authorized");
    }
    int retry = 0;
    int parameterCount = (args == null) ? 0 : 1;
    do {
      if (retry > 0) {
        LOG.info("Retrying delegated method call {}", retry);
      }
      RemoteObjectClient client = clients.get(endpoint);
      if (client == null) {
        synchronized (clients) {
          client = clients.get(endpoint);
          if (client == null) {
            RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder(
                new HttpClientRequestHandler(endpoint));
            client = clientBuilder.login(TARGET_USER, CryptoUtils.encrypt(TARGET_USER, publicKey));
            clients.put(endpoint, client);
          }
        }
      }
      T object = client.createRemoteObject(clazz, objectId);
      Method[] methods = object.getClass().getMethods();
      for (Method m : methods) {
        if (!m.getName().equals(method.getName())) {
          continue;
        }
        if (parameterCount != m.getParameterCount()) {
          continue;
        }
        try {
          return m.invoke(object, args);
        } catch (RemoteMethodCallException e) {
          LOG.error("Error occurred in delegated method call", e);
          if (e.getCode() == Constants.AUTHENTICATION_ERROR_CODE) {
            clients.remove(endpoint, object);
            retry++;
            break;
          } else {
            throw e;
          }
        } catch (InvocationTargetException e) {
          LOG.error("Error occurred in delegated method call", e.getTargetException());
          if (e.getTargetException() instanceof RemoteMethodCallException) {
            RemoteMethodCallException exception = (RemoteMethodCallException) e
                .getTargetException();
            if (exception.getCode() == Constants.AUTHENTICATION_ERROR_CODE) {
              clients.remove(endpoint, object);
              retry++;
              break;
            } else {
              throw exception;
            }
          } else {
            throw new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
                "Exception in remote method invocation", e.getTargetException());
          }
        } catch (Exception e) {
          LOG.error("Error occurred in delegated method call", e);
          throw new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
              "Exception in remote method invocation", e);
        }
      }
    } while (retry < CLIENT_RETRY_LIMIT);
    if (retry == 0) {
      throw new RemoteMethodCallException(Constants.SERVER_ERROR_CODE, String.format(
          "Method %s:%d not found in class %s", method.getName(), parameterCount, clazz.getName()));
    }
    throw new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
        "Retry limit exceeded in client call");
  }

  private ObjectEndpoint getObjectEndPoint(String user) {
    Set<ObjectEndpoint> objectEndpoints = objectEndpointProvider.apply(Objects.requireNonNull(user));
    // Support for only one
    ObjectEndpoint objectEndpoint = Iterables.getFirst(objectEndpoints, null);
    if (objectEndpoint == null) {
      return null;
    }
    Preconditions.checkNotNull(objectEndpoint.getObjectId());
    Preconditions.checkNotNull(objectEndpoint.getEndpoint());
    return objectEndpoint;
  }
}
