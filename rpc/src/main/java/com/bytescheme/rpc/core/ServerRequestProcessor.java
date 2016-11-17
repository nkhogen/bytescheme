package com.bytescheme.rpc.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ServerRequestProcessor implements RemoteObjectListener {
  private static final Logger LOG = LoggerFactory.getLogger(ServerRequestProcessor.class);
  private final MessageCodec messageCodec = new MessageCodec(null, this);
  private final Map<UUID, RemoteObject> objectMap = new ConcurrentHashMap<>();
  private final Map<Class<? extends RemoteObject>, ClassMetaData> classMetaDataMap = new ConcurrentHashMap<>();
  private final boolean autoRegisterRemoteObject;

  public ServerRequestProcessor() {
    this(true);
  }

  public ServerRequestProcessor(boolean autoRegisterRemoteObject) {
    this.autoRegisterRemoteObject = autoRegisterRemoteObject;
  }

  public synchronized void register(RemoteObject object) {
    Preconditions.checkNotNull(object, "Class is invalid");
    if (!objectMap.containsKey(object.getObjectId())) {
      Class<? extends RemoteObject> clazz = object.getClass();
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

  public synchronized void unregister(UUID objectId) {
    Preconditions.checkNotNull(objectId, "Object ID is invalid");
    RemoteObject remoteObject = objectMap.remove(objectId);
    if (remoteObject != null) {
      ClassMetaData classMetaData = classMetaDataMap.get(remoteObject.getClass());
      Preconditions.checkNotNull(classMetaData);
      if (classMetaData.getInstanceCount() <= 1) {
        classMetaDataMap.remove(remoteObject.getClass());
      } else {
        classMetaData.setInstanceCount(classMetaData.getInstanceCount() - 1);
      }
    }
  }

  protected RemoteObject lookup(UUID objectId) {
    return null;
  }

  public MethodCallResponse process(MethodCallRequest request) {
    RemoteMethodCallException exception = null;
    MethodCallResponse response = new MethodCallResponse();
    MethodCallRecorder.init(request.getRequestId(), (requestId, elapsedTime, methodTime) -> {
      LOG.info(Constants.METHOD_ENTER_LOG_FORMAT, requestId, "process", elapsedTime);
    });
    try {
      Preconditions.checkNotNull(request);
      Preconditions.checkNotNull(request.getObjectId());
      RemoteObject object = objectMap.get(request.getObjectId());
      if (object == null) {
        object = lookup(request.getObjectId());
        Preconditions.checkNotNull(object);
        if (autoRegisterRemoteObject) {
          register(object);
        }
      }
      Class<? extends RemoteObject> clazz = object.getClass();
      ClassMetaData classMetaData = classMetaDataMap.get(clazz);
      Preconditions.checkNotNull(classMetaData, "Unknown class name %s", clazz.getName());
      int parameterCount = request.getParameters() == null ? 0 : request.getParameters().length;
      Method method = classMetaData.getMethod(request.getName(), parameterCount);
      Preconditions.checkNotNull(method, "Unknown method name %s", request.getName());
      Object returnObj = method.invoke(object, request.getParameters());
      response.setReturnValue(messageCodec.getJson(returnObj));
    } catch (InvocationTargetException e) {
      LOG.error("Error occurred in server", e);
      exception = new RemoteMethodCallException("Error occurred in server", e.getTargetException());
    } catch (Exception e) {
      LOG.error("Error occurred in server", e);
      exception = new RemoteMethodCallException("Error occurred in server", e);
    } finally {
      MethodCallRecorder.uninit((requestId, elapsedTime, methodTime) -> {
        LOG.info(Constants.METHOD_EXIT_LOG_FORMAT, requestId, "process", elapsedTime, methodTime);
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
      Method currentMethod = methods.put(
          String.format(METHOD_KEY_FORMAT, method.getName(), method.getParameterCount()), method);
      Preconditions.checkArgument(currentMethod == null,
          "Method %s with parameter count %d already exists", method.getName(),
          method.getParameterCount());
    }

    public Method getMethod(String name, int parameterCount) {
      return methods.get(String.format(METHOD_KEY_FORMAT, name, parameterCount));
    }
  }
}
