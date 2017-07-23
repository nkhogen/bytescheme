package com.bytescheme.rpc.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.rpc.security.Authentication;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.rpc.security.Session;
import com.google.common.base.Preconditions;

/**
 * Remote server to call methods on the actual objects implementing the remote
 * object interface. Function overloading on the number of parameters is
 * supported. Type-based overloading is not supported.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class RemoteObjectServer implements RemoteObjectListener {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteObjectServer.class);
  private final MessageCodec messageCodec = new MessageCodec(null, this);
  private final Map<UUID, RemoteObject> objectMap = new ConcurrentHashMap<>();
  private final Map<Class<? extends RemoteObject>, ClassMetaData> classMetaDataMap = new ConcurrentHashMap<>();
  private final SecurityProvider[] securityProviders;
  private final boolean autoRegisterRemoteObject;

  public RemoteObjectServer() {
    this(true, new SecurityProvider[0]);
  }

  /**
   * Constructs the server instance with the chain of security providers.
   *
   * @param autoRegisterRemoteObject
   * @param securityProviders
   */
  public RemoteObjectServer(boolean autoRegisterRemoteObject,
      SecurityProvider... securityProviders) {
    this.securityProviders = securityProviders;
    this.autoRegisterRemoteObject = autoRegisterRemoteObject;
  }

  public void register(RemoteObject object) {
    Preconditions.checkNotNull(object, "Class is invalid");
    if (!objectMap.containsKey(object.getObjectId())) {
      Class<? extends RemoteObject> clazz = object.getClass();
      synchronized (clazz) {
        if (!objectMap.containsKey(object.getObjectId())) {
          Method[] methods = clazz.getMethods();
          ClassMetaData classMetaData = classMetaDataMap.get(clazz);
          if (classMetaData == null) {
            classMetaData = new ClassMetaData();
            for (Method method : methods) {
              classMetaData.addMethod(method);
            }
            classMetaDataMap.put(clazz, classMetaData);
          }
          objectMap.put(object.getObjectId(), object);
          classMetaData.setInstanceCount(classMetaData.getInstanceCount() + 1);
        }
      }
    }
  }

  public void unregister(UUID objectId) {
    Preconditions.checkNotNull(objectId, "Object ID is invalid");
    RemoteObject remoteObject = objectMap.remove(objectId);
    if (remoteObject != null) {
      Class<? extends RemoteObject> clazz = remoteObject.getClass();
      synchronized (clazz) {
        ClassMetaData classMetaData = classMetaDataMap.get(clazz);
        Preconditions.checkNotNull(classMetaData);
        if (classMetaData.getInstanceCount() <= 1) {
          classMetaDataMap.remove(clazz);
        } else {
          classMetaData.setInstanceCount(classMetaData.getInstanceCount() - 1);
        }
      }
    }
  }

  public RemoteObject lookup(UUID objectId) {
    Preconditions.checkNotNull(objectId, "Invalid object ID");
    return objectMap.get(objectId);
  }

  protected RemoteObject lookupExternal(UUID objectId) {
    return null;
  }

  public MethodCallResponse login(LoginCallRequest request) {
    RemoteMethodCallException exception = null;
    MethodCallResponse response = new MethodCallResponse();
    MethodCallRecorder.init(request.getRequestId(),
        (requestId, elapsedTime, methodTime) -> {
          LOG.info(Constants.METHOD_ENTER_LOG_FORMAT, requestId, "login", elapsedTime);
        });
    try {
      Preconditions.checkNotNull(request);
      if (securityProviders == null || securityProviders.length == 0) {
        LOG.info("Security provider not configured");
        return response;
      }
      Session session = null;
      // Chain of security providers.
      for (SecurityProvider securityProvider : securityProviders) {
        try {
          session = securityProvider
              .authenticate(new Authentication(request.getUser(), request.getPassword()));
          if (session != null) {
            break;
          }
        } catch (Exception e) {
          LOG.error("Error occurred in authentication", e);
        }
      }
      if (session == null) {
        throw new RemoteMethodCallException(Constants.AUTHENTICATION_ERROR_CODE,
            String.format("Failed to authenticate user %s", request.getUser()));
      }
      response.setReturnValue(session.getId());
    } catch (RemoteMethodCallException e) {
      exception = e;
    } catch (Exception e) {
      LOG.error("Error occurred in login", e);
      exception = new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
          "Error occurred in login", e);
    } finally {
      MethodCallRecorder.uninit((requestId, elapsedTime, methodTime) -> {
        LOG.info(Constants.METHOD_EXIT_LOG_FORMAT, requestId, "login", elapsedTime,
            methodTime);
      });
    }
    response.setException(exception);
    return response;
  }

  public MethodCallResponse logout(LogoutCallRequest request) {
    RemoteMethodCallException exception = null;
    MethodCallResponse response = new MethodCallResponse();
    MethodCallRecorder.init(request.getRequestId(),
        (requestId, elapsedTime, methodTime) -> {
          LOG.info(Constants.METHOD_ENTER_LOG_FORMAT, requestId, "logout", elapsedTime);
        });
    try {
      Preconditions.checkNotNull(request);
      if (securityProviders == null || securityProviders.length == 0) {
        LOG.info("Security provider not configured");
        return response;
      }
      // Chain of security providers.
      for (SecurityProvider securityProvider : securityProviders) {
        securityProvider.destroySession(request.getSessionId());
      }
      response.setReturnValue(request.getSessionId());
    } catch (RemoteMethodCallException e) {
      exception = e;
    } catch (Exception e) {
      LOG.error("Error occurred in logout", e);
      exception = new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
          "Error occurred in logout", e);
    } finally {
      MethodCallRecorder.uninit((requestId, elapsedTime, methodTime) -> {
        LOG.info(Constants.METHOD_EXIT_LOG_FORMAT, requestId, "logout", elapsedTime,
            methodTime);
      });
    }
    response.setException(exception);
    return response;
  }

  public MethodCallResponse process(MethodCallRequest request) {
    RemoteMethodCallException exception = null;
    MethodCallResponse response = new MethodCallResponse();
    MethodCallRecorder.init(request.getRequestId(),
        (requestId, elapsedTime, methodTime) -> {
          LOG.info(Constants.METHOD_ENTER_LOG_FORMAT, requestId, "process", elapsedTime);
        });
    boolean isAutoRegistered = false;
    try {
      Preconditions.checkNotNull(request);
      Preconditions.checkNotNull(request.getObjectId());
      checkSecurity(request);
      RemoteObject object = objectMap.get(request.getObjectId());
      if (object == null) {
        object = lookupExternal(request.getObjectId());
        Preconditions.checkNotNull(object);
        if (autoRegisterRemoteObject) {
          register(object);
          isAutoRegistered = true;
        }
      }
      Class<? extends RemoteObject> clazz = object.getClass();
      ClassMetaData classMetaData = classMetaDataMap.get(clazz);
      Preconditions.checkNotNull(classMetaData, "Unknown class name %s", clazz.getName());
      String[] jsonParameters = request.getParameters();
      int parameterCount = (jsonParameters == null) ? 0 : jsonParameters.length;
      Method method = classMetaData.getMethod(request.getName(), parameterCount);
      Preconditions.checkNotNull(method, "Unknown method name %s with parameter count %s",
          request.getName(), parameterCount);
      Object[] parameters = new Object[parameterCount];
      if (jsonParameters != null) {
        int index = 0;
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (String jsonParameter : jsonParameters) {
          Class<?> parameterType = parameterTypes[index];
          parameters[index] = messageCodec.getObject(jsonParameter, parameterType);
          index++;
        }
      }
      if (LOG.isDebugEnabled() && request.getParameters() != null) {
        for (Object param : parameters) {
          LOG.debug("Parameter type {}", param.getClass().getName());
        }
      }
      Object returnObj = method.invoke(object, parameters);
      response.setReturnValue(messageCodec.getJson(returnObj));
    } catch (InvocationTargetException e) {
      LOG.error("Error occurred in server", e.getTargetException());
      if (e.getTargetException() instanceof RemoteMethodCallException) {
        exception = (RemoteMethodCallException) e.getTargetException();
      } else {
        exception = new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
            "Error occurred in server", e.getTargetException());
      }
    } catch (RemoteMethodCallException e) {
      exception = e;
    } catch (Exception e) {
      LOG.error("Error occurred in server", e);
      exception = new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
          "Error occurred in server", e);
    } finally {
      if (isAutoRegistered) {
        unregister(request.getObjectId());
      }
      MethodCallRecorder.uninit((requestId, elapsedTime, methodTime) -> {
        LOG.info(Constants.METHOD_EXIT_LOG_FORMAT, requestId, "process", elapsedTime,
            methodTime);
      });
    }
    response.setException(exception);
    return response;
  }

  @Override
  public void onRemoteObjectFound(RemoteObject remoteObject) {
    if (autoRegisterRemoteObject) {
      register(remoteObject);
    }
  }

  private void checkSecurity(MethodCallRequest request) {
    if (securityProviders == null || securityProviders.length == 0) {
      LOG.info("Security provider not configured");
      return;
    }
    RemoteMethodCallException exception = null;
    for (SecurityProvider securityProvider : securityProviders) {
      try {
        Authentication authentication = securityProvider.authenticate(request);
        if (authentication == null) {
          continue;
        }
        if (securityProvider.authorize(authentication, request)) {
          return;
        }
      } catch (RemoteMethodCallException e) {
        exception = e;
      } catch (Exception e) {
        String msg = String.format("Error occurred in security check for request ID %s",
            request.getRequestId());
        LOG.error(msg, e);
        exception = new RemoteMethodCallException(Constants.SERVER_ERROR_CODE, msg, e);
      }
    }
    if (exception != null) {
      throw exception;
    }
    throw new RemoteMethodCallException(Constants.AUTHORIZATION_ERROR_CODE,
        "Unauthorized user");
  }

  private class ClassMetaData {
    private static final String METHOD_KEY_FORMAT = "%s:%d";
    private int instanceCount = 0;
    private Map<String, Method> methods = new ConcurrentHashMap<>();

    public int getInstanceCount() {
      return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
    }

    public void addMethod(Method method) {
      String methodKey = String.format(METHOD_KEY_FORMAT, method.getName(),
          method.getParameterCount());
      LOG.info("Adding method {} in class {}", methodKey, method.getDeclaringClass());
      Method currentMethod = methods.put(methodKey, method);
      Preconditions.checkArgument(currentMethod == null,
          "Method %s with parameter count %d already exists", method.getName(),
          method.getParameterCount());
    }

    public Method getMethod(String name, int parameterCount) {
      return methods.get(String.format(METHOD_KEY_FORMAT, name, parameterCount));
    }
  }
}
