package com.bytescheme.rpc.security;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.utils.PathProcessor;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class SecurityProvider {
	private static final Logger LOG = LoggerFactory.getLogger(SecurityProvider.class);
	private static final String AUTH_PATH = "%s/%s:%d";
	private final AuthenticationProvider authenticationProvider;
	private final PathProcessor pathProcessor;

	public SecurityProvider(AuthenticationProvider authenticationProvider, PathProcessor pathProcessor) {
		Preconditions.checkNotNull(authenticationProvider);
		Preconditions.checkNotNull(pathProcessor);
		this.authenticationProvider = authenticationProvider;
		this.pathProcessor = pathProcessor;
	}

	public Session authenticate(Authentication authentication) {
		Authentication checkedAuthentication = authenticationProvider.authenticate(authentication);
		if (checkedAuthentication == null) {
			return null;
		}
		return SessionManager.getInstance().addSessionId(authentication, createSession(authentication.getUser()));
	}

	public boolean authorize(MethodCallRequest request) {
		Preconditions.checkNotNull(request);
		Preconditions.checkNotNull(request.getObjectId());
		Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getSessionId()));
		Session session = SessionManager.getInstance().getSession(request.getSessionId());
		if (session == null) {
			LOG.info("Session not found for request ID {}", request.getRequestId());
			return false;
		}
		Authentication authentication = authenticationProvider.authenticate(session.getAuthentication());
		if (authentication == null) {
			LOG.info("Session authentication failed for request ID {}", request.getRequestId());
			return false;
		}
		int parameterCount = request.getParameters() == null ? 0 : request.getParameters().length;
		String authPath = String.format(AUTH_PATH, request.getObjectId(), request.getName(), parameterCount);
		LOG.info("Authorization check for path {} in request ID {}", authPath, request.getRequestId());
		Set<String> roles = pathProcessor.procesPath(authPath);
		Set<String> userRoles = authentication.getRoles();
		if (CollectionUtils.isEmpty(userRoles)) {
			return CollectionUtils.isEmpty(roles);
		}
		Set<String> commonRoles = Sets.intersection(userRoles, roles);
		if (commonRoles.isEmpty()) {
			LOG.info("No matching role found for path {} in request ID {}", authPath, request.getRequestId());
			return false;
		}
		return true;
	}

	public Session destroySession(String sessionId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionId));
		return SessionManager.getInstance().deleteSession(sessionId);
	}

	protected String createSession(String user) {
		return UUID.randomUUID().toString();
	}
}
