package com.bytescheme.service.controlboard.remoteobjects;

import static com.bytescheme.service.controlboard.common.Constants.ROOT_OBJECT_ID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.Constants;
import com.bytescheme.rpc.core.HttpClientRequestHandler;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.bytescheme.rpc.core.RemoteObject;
import com.bytescheme.rpc.core.RemoteObjectClient;
import com.bytescheme.rpc.core.RemoteObjectClientBuilder;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.ConfigurationProvider;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;
import com.bytescheme.service.controlboard.common.remoteobjects.BaseMockControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.ControlBoard;
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.bytescheme.service.controlboard.domains.ObjectEndpoint;

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
  private static final int MAX_CLIENT_CACHE_SIZE = 50;
  private static final String TARGET_USER = "controlboard";

  private final Map<String, RemoteObjectClient> clients = Collections.synchronizedMap(new LinkedHashMap<String, RemoteObjectClient>() {
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, RemoteObjectClient> entry) {
      return size() > MAX_CLIENT_CACHE_SIZE;
    }
  });

  private final boolean enableMock;

  @Autowired
  private ConfigurationProvider configurationProvider;

  @Autowired
  private SecurityProvider securityProvider;

  @Autowired
  private DeviceEventScheduler deviceEventScheduler;

  public RootImpl(boolean enableMock) {
    this.enableMock = enableMock;
  }

  public boolean isEnableMock() {
    return enableMock;
  }

  @PostConstruct
  public void validate() {
    Objects.requireNonNull(configurationProvider.getObjectEndpointsProvider());
  }

  @Override
  public UUID getObjectId() {
    return ROOT_OBJECT_ID;
  }

  @Override
  public ControlBoard getControlBoard() {
    String user = securityProvider.getCurrentUser();
    ObjectEndpoint objectEndpoint = configurationProvider.getObjectEndPoint(user);
    UUID objectId = objectEndpoint.getObjectId();
    try {
      if (enableMock) {
        return new BaseMockControlBoard(objectId);
      }
      ControlBoard remoteControlBoard = createRemoteObject(
          ControlBoard.class,
          objectId,
          objectEndpoint.getEndpoint(),
          objectEndpoint.getPublicKey());
      return new DelegateControlBoardImpl(objectId, remoteControlBoard);
    } catch (Exception e) {
      throw new RemoteMethodCallException(
          Constants.SERVER_ERROR_CODE,
          "Failed to create the control board",
          e);
    }
  }

  @Override
  public DeviceEventScheduler getDeviceEventScheduler() {
    return deviceEventScheduler;
  }

  private <T extends RemoteObject> T createRemoteObject(Class<T> clazz, UUID objectId,
      String endpoint, PublicKey publicKey) {
    return clazz.cast(
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { clazz },
            (proxy, method, args) -> {
              return invokeMethod(clazz, objectId, method, args, endpoint, publicKey);
            }));
  }

  /*
   * This method takes care of auto-login in case of session expiry under the
   * hood when a method of the remote object is invoked.
   */
  private <T extends RemoteObject> Object invokeMethod(Class<T> clazz, UUID objectId, Method method,
      Object[] args, String endpoint, PublicKey publicKey) throws MalformedURLException {
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
            throw new RemoteMethodCallException(
                Constants.SERVER_ERROR_CODE,
                "Exception in remote method invocation",
                e.getTargetException());
          }
        } catch (Exception e) {
          LOG.error("Error occurred in delegated method call", e);
          throw new RemoteMethodCallException(
              Constants.SERVER_ERROR_CODE,
              "Exception in remote method invocation",
              e);
        }
      }
    } while (retry < CLIENT_RETRY_LIMIT);
    if (retry == 0) {
      throw new RemoteMethodCallException(
          Constants.SERVER_ERROR_CODE,
          String.format(
              "Method %s:%d not found in class %s",
              method.getName(),
              parameterCount,
              clazz.getName()));
    }
    throw new RemoteMethodCallException(
        Constants.SERVER_ERROR_CODE,
        "Retry limit exceeded in client call");
  }
}
