package com.bytescheme.rpc.core;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
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
  private static final Logger LOG = LoggerFactory
      .getLogger(RemoteObjectClientBuilder.class);
  private static final String LOGIN_PATH = "/login";
  private static final String LOGOUT_PATH = "/logout";
  private static final MessageCodec DEFAULT_MESSAGE_CODEC = new MessageCodec(null, null);
  private final URL loginUrl;
  private final URL logoutUrl;
  private final URL remoteObjectUrl;

  public RemoteObjectClientBuilder(String url) throws MalformedURLException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "Invalid URL");
    this.loginUrl = new URL(url + LOGIN_PATH);
    this.logoutUrl = new URL(url + LOGOUT_PATH);
    this.remoteObjectUrl = new URL(url);
  }

  public RemoteObjectClient login(String user, String password) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user), "Invalid user");
    LoginCallRequest request = new LoginCallRequest();
    request.setUser(user);
    request.setPassword(password);
    request.setRequestId(UUID.randomUUID());
    LOG.info("Invoking login method {} with user {} and request ID {}", request.getUser(),
        request.getRequestId());
    String sessionId = invokeHttpPost(loginUrl, request, String.class);
    return new ClientRemoteObjectFactory(sessionId);
  }

  private <T> T invokeHttpPost(URL url, Object request, Class<T> responseClass) {
    return invokeHttpPost(url, request, responseClass, DEFAULT_MESSAGE_CODEC);
  }

  private <T> T invokeHttpPost(URL url, Object request, Class<T> responseClass,
      MessageCodec messageCodec) {
    CloseableHttpClient httpClient = null;
    try {
      SSLContext sslContext = new SSLContextBuilder()
          .loadTrustMaterial(null, (certificate, authType) -> true).build();
      httpClient = HttpClients.custom().setSSLContext(sslContext)
          .setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
      LOG.info("Contacting url {}", url.toString());
      StringEntity entity = new StringEntity(messageCodec.getJson(request));
      HttpPost httpPost = new HttpPost(url.toString());
      httpPost.setHeader("Accept", "application/json");
      httpPost.setHeader("Content-type", "application/json");
      httpPost.setEntity(entity);
      CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new RemoteMethodCallException(Constants.SERVER_ERROR_CODE,
            String.format("Response %d is not OK ", statusCode));
      }
      String responseMessage = EntityUtils.toString(httpResponse.getEntity());
      LOG.info("Received server message: {}", responseMessage);
      MethodCallResponse response = messageCodec.getObject(responseMessage,
          MethodCallResponse.class);
      if (response.getException() != null) {
        LOG.error("Error occurred in method call", response.getException());
        throw response.getException();
      }
      return messageCodec.getObject(response.getReturnValue(), responseClass);
    } catch (RemoteMethodCallException e) {
      throw e;
    } catch (Exception e) {
      throw new RemoteMethodCallException(Constants.CLIENT_ERROR_CODE,
          "Error occurred in client", e);
    } finally {
      IOUtils.closeQuietly(httpClient);
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
            if (method.getName().equals("getObjectId")
                && method.getParameterCount() == 0) {
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
            return invokeHttpPost(remoteObjectUrl, request, method.getReturnType(),
                messageCodec);
          }));
    }

    @Override
    public void logout() {
      LogoutCallRequest request = new LogoutCallRequest();
      request.setSessionId(sessionId);
      request.setRequestId(UUID.randomUUID());
      LOG.info("Invoking logout method {} with session ID %s and request ID {}",
          request.getSessionId(), request.getRequestId());
      invokeHttpPost(logoutUrl, request, String.class);
    }
  }
}
