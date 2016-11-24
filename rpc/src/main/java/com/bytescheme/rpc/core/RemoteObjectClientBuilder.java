package com.bytescheme.rpc.core;

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
import com.google.common.base.Strings;

public class RemoteObjectClientBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(RemoteObjectClientBuilder.class);
	private static final String LOGIN_PATH = "/login";
	private static final String LOGOUT_PATH = "/logout";
	private static final MessageCodec MESSAGE_CODEC = new MessageCodec(null, null);
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
		LoginCallRequest request = new LoginCallRequest();
		request.setUser(user);
		request.setPassword(password);
		request.setRequestId(UUID.randomUUID());
		LOG.info("Invoking login method {} with user %s and request ID {}", request.getUser(), request.getRequestId());
		String sessionId = invokeHttpPost(loginUrl, MESSAGE_CODEC, request, String.class);
		return new ClientRemoteObjectFactory(sessionId);
	}

	private static <T> T invokeHttpPost(URL url, MessageCodec messageCodec, Object request, Class<T> responseClass) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			LOG.info("Contacting url {}", url.toString());
			StringEntity entity = new StringEntity(messageCodec.getJson(request));
			HttpPost httpPost = new HttpPost(url.toString());
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			httpPost.setEntity(entity);
			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new RemoteMethodCallException(String.format("Response %d is not OK ", statusCode));
			}
			String responseMessage = EntityUtils.toString(httpResponse.getEntity());
			MethodCallResponse response = messageCodec.getObject(responseMessage, MethodCallResponse.class);
			if (response.getException() != null) {
				throw response.getException();
			}
			return messageCodec.getObject(response.getReturnValue(), responseClass);
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

	private class ClientRemoteObjectFactory implements RemoteObjectClient {
		private final String sessionId;
		private final MessageCodec messageCodec = new MessageCodec(this, null);

		ClientRemoteObjectFactory(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public <T extends RemoteObject> T createRemoteObject(Class<T> clazz, UUID objectId) {
			return clazz.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { clazz },
					(proxy, method, args) -> {
						if (method.getName().equals("getObjectId") && method.getParameterCount() == 0) {
							return objectId;
						}
						CloseableHttpClient httpClient = HttpClients.createDefault();
						try {
							int parameterCount = args == null ? 0 : args.length;
							MethodCallRequest request = new MethodCallRequest();
							request.setName(method.getName());
							request.setObjectId(objectId);
							request.setParameters(args);
							request.setRequestId(UUID.randomUUID());
							request.setSessionId(sessionId);
							LOG.info("Invoking remote method {}, parameter count {} on object ID {} with request ID {}",
									request.getName(), parameterCount, objectId, request.getRequestId());
							return invokeHttpPost(remoteObjectUrl, messageCodec, request, method.getReturnType());
						} catch (RemoteMethodCallException e) {
							throw e;
						} catch (Exception e) {
							throw new RemoteMethodCallException("Error occurred in client", e);
						} finally {
							if (httpClient != null) {
								IOUtils.closeQuietly(httpClient);
							}
						}
					}));
		}

		@Override
		public void logout() {
			LogoutCallRequest request = new LogoutCallRequest();
			request.setSessionId(sessionId);
			request.setRequestId(UUID.randomUUID());
			LOG.info("Invoking logout method {} with session ID %s and request ID {}", request.getSessionId(),
					request.getRequestId());
			invokeHttpPost(logoutUrl, messageCodec, request, String.class);
		}
	}
}