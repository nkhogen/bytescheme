package com.bytescheme.rpc.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class ClientRequestProcessor implements RemoteObjectFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ClientRequestProcessor.class);
  private final URL url;
  private final MessageCodec messageCodec = new MessageCodec(this, null);

  public ClientRequestProcessor(String url) throws MalformedURLException {
    Preconditions.checkNotNull(url, "Invalid URL");
    this.url = new URL(url);
  }

  public <T extends RemoteObject> T createRemoteObject(Class<T> clazz, UUID objectId) {
    return clazz.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { clazz },
        new RemoteInvocationHandler(objectId)));
  }

  private class RemoteInvocationHandler implements InvocationHandler {
    private final UUID objectId;

    public RemoteInvocationHandler(UUID objectId) {
      this.objectId = objectId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      CloseableHttpClient httpClient = HttpClients.createDefault();
      try {
        if (method.getName().equals("getObjectId") && method.getParameterCount() == 0) {
          return objectId;
        }
        int parameterCount = args == null ? 0 : args.length;
        MethodCallRequest request = new MethodCallRequest();
        request.setName(method.getName());
        request.setObjectId(objectId);
        request.setParameters(args);
        request.setRequestId(UUID.randomUUID());
        LOG.info("Invoking remote method {}, parameter count {} on object ID {} with request ID {}",
            request.getName(), parameterCount, objectId, request.getRequestId());
        StringEntity entity = new StringEntity(messageCodec.getJson(request));
        HttpPost httpPost = new HttpPost(url.toString());
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setEntity(entity);
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
          throw new RemoteMethodCallException("Response is not OK");
        }
        String responseMessage = EntityUtils.toString(httpResponse.getEntity());
        MethodCallResponse response = messageCodec.getObject(responseMessage,
            MethodCallResponse.class);
        if (response.getException() != null) {
          throw response.getException();
        }
        return messageCodec.getObject(response.getReturnValue(), method.getReturnType());
      } catch (RemoteMethodCallException e) {
        throw e;
      } catch (Exception e) {
        throw new RemoteMethodCallException("Error occurred in client", e);
      } finally {
        if (httpClient != null) {
          IOUtils.closeQuietly(httpClient);
        }
      }
    }
  }
}
