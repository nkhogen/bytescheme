package com.bytescheme.service.controlboard.remoteobjects;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.bytescheme.common.properties.FilePropertyChangePublisher;
import com.bytescheme.common.properties.PropertyChangePublisher;
import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.RemoteAuthenticationException;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.bytescheme.rpc.core.RemoteObject;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.rpc.core.RemoteObjectFactory;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.MockControlBoardImpl;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Root object implementation.
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class RootImpl implements Root, RemoteObjectFactory {
  private static final long serialVersionUID = 1L;
  private static final int CLIENT_RETRY_LIMIT = 3;
  private static final UUID OBJECT_ID = new UUID(0L, 0L);
  private static final String TARGET_USER = "controlboard";
  private final AtomicReference<Map<String, String>> objectIdsRef = new AtomicReference<>(
      Collections.emptyMap());
  private final AtomicReference<Map<String, String>> endpointsRef = new AtomicReference<>(
      Collections.emptyMap());
  private final ConcurrentHashMap<String, RemoteObjectClient> clients = new ConcurrentHashMap<>(
      Collections.emptyMap());
  private final String sshKeysDir;
  private boolean enableMock = false;

  public RootImpl(String objectsJsonFile, String endpointsJsonFile, String sshKeysDir) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(objectsJsonFile),
        "Invalid objects JSON file");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(endpointsJsonFile),
        "Invalid endpoints JSON file");
    PropertyChangePublisher<String> objectsChangePublisher = new FilePropertyChangePublisher<String>(
        objectsJsonFile, String.class);
    objectIdsRef.set(objectsChangePublisher.getCurrentProperties());
    objectsChangePublisher.registerListener((updated, all) -> {
      objectIdsRef.set(all);
    });

    PropertyChangePublisher<String> endpointsChangePublisher = new FilePropertyChangePublisher<String>(
        endpointsJsonFile, String.class);
    endpointsRef.set(endpointsChangePublisher.getCurrentProperties());
    endpointsChangePublisher.registerListener((updated, all) -> {
      endpointsRef.set(all);
    });
    File file = new File(sshKeysDir);
    Preconditions.checkArgument(file.exists() && file.isDirectory() && file.canRead(),
        "Invalid ssh key folder %s", sshKeysDir);
    this.sshKeysDir = sshKeysDir;
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
    Map<String, String> objectIds = objectIdsRef.get();
    Map<String, String> endpoints = endpointsRef.get();
    String objectIdStr = objectIds.get(user);
    if (objectIdStr == null) {
      return null;
    }
    String endpoint = endpoints.get(objectIdStr);
    if (endpoint == null) {
      return null;
    }
    UUID objectId = UUID.fromString(objectIdStr);
    try {
      if (enableMock) {
        return new MockControlBoardImpl(objectId);
      }
      ControlBoard remoteControlBoard = createRemoteObject(ControlBoard.class, objectId);
      return new DelegateControlBoardImpl(objectId, remoteControlBoard);
    } catch (Exception e) {
      throw new RemoteMethodCallException("Failed to create the control board", e);
    }
  }

  @Override
  public <T extends RemoteObject> T createRemoteObject(Class<T> clazz, UUID objectId) {
    return clazz.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[] { clazz }, (proxy, method, args) -> {
          Map<String, String> endpoints = endpointsRef.get();
          if (endpoints == null) {
            throw new IllegalStateException("No points found");
          }
          String endpoint = endpoints.get(objectId.toString());
          if (endpoint == null) {
            throw new IllegalArgumentException(
                String.format("Object ID %s not found", objectId));
          }
          return invokeMethod(clazz, objectId, method, args, endpoint);
        }));
  }

  /*
   * This method takes care of auto-login in case of session expiry under the
   * hood when a method of the remote object is invoked.
   */
  private <T extends RemoteObject> Object invokeMethod(Class<T> clazz, UUID objectId,
      Method method, Object[] args, String endpoint) throws MalformedURLException {
    int retry = 0;
    int parameterCount = (args == null) ? 0 : 1;
    do {
      RemoteObjectClient client = clients.get(endpoint);
      if (client == null) {
        synchronized (clients) {
          client = clients.get(endpoint);
          if (client == null) {
            RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder(
                endpoint);
            client = clientBuilder.login(TARGET_USER,
                CryptoUtils.encrypt(TARGET_USER, CryptoUtils.getPublicKey(
                    sshKeysDir + File.separator + objectId.toString() + ".pub")));
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
        } catch (RemoteAuthenticationException e) {
          clients.remove(endpoint, object);
          retry++;
        } catch (InvocationTargetException e) {
          if (e.getTargetException() instanceof RemoteAuthenticationException) {
            clients.remove(endpoint, object);
            retry++;
          } else {
            throw new RuntimeException("Exception in remote method invocation",
                e.getTargetException());
          }
        } catch (Exception e) {
          throw new RuntimeException("Exception in remote method invocation", e);
        }
      }
    } while (retry < CLIENT_RETRY_LIMIT);
    if (retry == 0) {
      throw new RemoteMethodCallException(
          String.format("Method %s:%d not found in class %s", method.getName(),
              parameterCount, clazz.getName()));
    }
    throw new RemoteMethodCallException("Retry limit exceeded in client call");
  }

}
