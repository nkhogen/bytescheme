package com.bytescheme.rpc.security;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.paths.PathProcessor;
import com.bytescheme.rpc.core.Constants;
import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * Security provider combines authentication, authorization and the session
 * management.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class SecurityProvider {
  private static final Logger LOG = LoggerFactory.getLogger(SecurityProvider.class);
  private static final String AUTH_PATH = "%s:%d";
  private final AuthenticationProvider authenticationProvider;
  private final PathProcessor pathProcessor;

  public SecurityProvider(AuthenticationProvider authenticationProvider) {
    this(authenticationProvider, null);
  }

  public SecurityProvider(AuthenticationProvider authenticationProvider,
      PathProcessor pathProcessor) {
    Preconditions.checkNotNull(authenticationProvider);
    this.authenticationProvider = authenticationProvider;
    this.pathProcessor = pathProcessor;
  }

  public Session authenticate(Authentication authentication) {
    Authentication checkedAuthentication = authenticationProvider
        .authenticate(authentication);
    if (checkedAuthentication == null) {
      String msg = String.format("Authentication failed for user %s",
          authentication.getUser());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHENTICATION_ERROR_CODE, msg);
    }
    return SessionManager.getInstance().addSessionId(authentication,
        createSessionId(authentication.getUser()));
  }

  public Authentication authenticate(MethodCallRequest request) {
    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(request.getObjectId());
    Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getSessionId()));
    Session session = SessionManager.getInstance().updateSession(request.getSessionId());
    if (session == null) {
      String msg = String.format("Session not found for request ID %s",
          request.getRequestId());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHENTICATION_ERROR_CODE, msg);
    }
    Authentication authentication = authenticationProvider
        .authenticate(session.getAuthentication());
    if (authentication == null) {
      String msg = String.format("Session authentication failed for request ID %s",
          request.getRequestId());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHORIZATION_ERROR_CODE, msg);
    }
    return authentication;
  }

  public boolean authorize(Authentication authentication, MethodCallRequest request) {
    if (pathProcessor == null) {
      LOG.info("Authorization is not enabled");
      return true;
    }
    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(authentication);
    int parameterCount = request.getParameters() == null ? 0
        : request.getParameters().length;
    String authPath = String.format(AUTH_PATH, request.getName(), parameterCount);
    LOG.info("Authorization check for path {} in request ID {}", authPath,
        request.getRequestId());
    Set<String> roles = pathProcessor.procesPath(request.getObjectId().toString(),
        authPath);
    Set<String> userRoles = authentication.getRoles();
    if (CollectionUtils.isEmpty(userRoles)) {
      return CollectionUtils.isEmpty(roles);
    }
    Set<String> commonRoles = Sets.intersection(userRoles, roles);
    if (commonRoles.isEmpty()) {
      String msg = String.format("No matching role found for path %s in request ID %s",
          authPath, request.getRequestId());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHORIZATION_ERROR_CODE, msg);
    }
    return true;
  }

  public Session destroySession(String sessionId) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
    return SessionManager.getInstance().deleteSession(sessionId);
  }

  protected String createSessionId(String user) {
    return UUID.randomUUID().toString();
  }
}
