package com.bytescheme.rpc.core;

import java.net.MalformedURLException;
import java.net.URL;

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
 * Http REST client request handler.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class HttpClientRequestHandler implements ClientRequestHandler {
  private static final String LOGIN_PATH = "/login";
  private static final String LOGOUT_PATH = "/logout";

  private static final Logger LOG = LoggerFactory.getLogger(HttpClientRequestHandler.class);

  private final URL loginUrl;
  private final URL logoutUrl;
  private final URL remoteObjectUrl;

  public HttpClientRequestHandler(String url) throws MalformedURLException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "Invalid URL");
    this.loginUrl = new URL(url + LOGIN_PATH);
    this.logoutUrl = new URL(url + LOGOUT_PATH);
    this.remoteObjectUrl = new URL(url);
  }

  @Override
  public MethodCallResponse invoke(RemoteCallRequest request, MessageCodec messageCodec) {
    CloseableHttpClient httpClient = null;
    try {
      String targetUrl = remoteObjectUrl.toString();
      if (request instanceof LoginCallRequest) {
        targetUrl = loginUrl.toString();
      } else if (request instanceof LogoutCallRequest) {
        targetUrl = logoutUrl.toString();
      }
      SSLContext sslContext = new SSLContextBuilder()
          .loadTrustMaterial(null, (certificate, authType) -> true).build();
      httpClient = HttpClients.custom().setSSLContext(sslContext)
          .setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
      LOG.info("Contacting url {}", targetUrl);
      StringEntity entity = new StringEntity(messageCodec.getJson(request));
      HttpPost httpPost = new HttpPost(targetUrl);
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
      return messageCodec.getObject(responseMessage, MethodCallResponse.class);
    } catch (Exception e) {
      throw new RemoteMethodCallException(Constants.CLIENT_ERROR_CODE, "Error occurred in client",
          e);
    } finally {
      IOUtils.closeQuietly(httpClient);
    }
  }
}
