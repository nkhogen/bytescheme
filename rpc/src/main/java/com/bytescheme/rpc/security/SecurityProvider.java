package com.bytescheme.rpc.security;

import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.paths.PathProcessor;
import com.bytescheme.common.utils.BasicUtils;
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
  private static final ThreadLocal<Session> SESSION = new ThreadLocal<>();

  private final AuthenticationProvider[] authenticationProviders;
  private final PathProcessor pathProcessor;

  public SecurityProvider(AuthenticationProvider... authenticationProviders) {
    this(null, authenticationProviders);
  }

  public SecurityProvider(PathProcessor pathProcessor,
      AuthenticationProvider... authenticationProviders) {
    Preconditions.checkNotNull(authenticationProviders);
    this.pathProcessor = pathProcessor;
    this.authenticationProviders = authenticationProviders;
  }

  public Session authenticate(Authentication authentication) {
    Authentication checkedAuthentication = null;
    for (AuthenticationProvider authenticationProvider : authenticationProviders) {
      try {
        checkedAuthentication = authenticationProvider.authenticate(authentication);
      } catch (Exception e) {
        LOG.warn("Authentication failed. Trying the next authentication provider");
      }
      if (checkedAuthentication != null) {
        break;
      }
    }
    if (checkedAuthentication == null) {
      String msg = String.format("Authentication failed for user %s", authentication.getUser());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHENTICATION_ERROR_CODE, msg);
    }
    Session session = SessionManager.getInstance()
        .addSessionId(authentication, createSessionId(authentication.getUser()));
    SESSION.set(session);
    return session;
  }

  public Authentication authenticate(MethodCallRequest request) {
    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(request.getObjectId());
    Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getSessionId()));
    Session session = SessionManager.getInstance().updateSession(request.getSessionId());
    if (session == null) {
      String msg = String.format("Session not found for request ID %s", request.getRequestId());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHENTICATION_ERROR_CODE, msg);
    }
    Authentication checkedAuthentication = null;
    for (AuthenticationProvider authenticationProvider : authenticationProviders) {
      try {
        checkedAuthentication = authenticationProvider.authenticate(session.getAuthentication());
      } catch (Exception e) {
        LOG.warn("Authentication failed. Trying the next authentication provider");
      }
      if (checkedAuthentication != null) {
        break;
      }
    }
    if (checkedAuthentication == null) {
      String msg = String
          .format("Session authentication failed for request ID %s", request.getRequestId());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHORIZATION_ERROR_CODE, msg);
    }
    SESSION.set(session);
    return checkedAuthentication;
  }

  public void authorize(Authentication authentication, MethodCallRequest request) {
    if (pathProcessor == null) {
      LOG.info("Authorization is not enabled");
      return;
    }
    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(authentication);
    int parameterCount = request.getParameters() == null ? 0 : request.getParameters().length;
    String authPath = String.format(AUTH_PATH, request.getName(), parameterCount);
    LOG.info("Authorization check for path {} in request ID {}", authPath, request.getRequestId());
    Set<String> roles = pathProcessor.processPath(request.getObjectId().toString(), authPath);
    Set<String> userRoles = authentication.getRoles();
    if (CollectionUtils.isEmpty(userRoles) && CollectionUtils.isEmpty(roles)) {
      LOG.info("No roles are required for the object {}", request.getObjectId());
      return;
    }
    Set<String> commonRoles = Sets.intersection(userRoles, roles);
    if (commonRoles.isEmpty()) {
      String msg = String.format(
          "No matching role found for path %s in request ID %s",
          authPath,
          request.getRequestId());
      LOG.info(msg);
      throw new RemoteMethodCallException(Constants.AUTHORIZATION_ERROR_CODE, msg);
    }
  }

  public String getCurrentUser() {
    Session session = SESSION.get();
    if (session == null || !SessionManager.getInstance().isSessionValid(session.getId())) {
      LOG.info("Current session is not available");
      return null;
    }
    return session.getAuthentication().getUser();
  }

  public Session destroySession(String sessionId) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
    return SessionManager.getInstance().deleteSession(sessionId);
  }

  protected String createSessionId(String user) {
    return BasicUtils.createSessionId();
  }
}
