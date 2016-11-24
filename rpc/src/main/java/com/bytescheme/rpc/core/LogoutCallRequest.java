package com.bytescheme.rpc.core;

import java.util.UUID;

public class LogoutCallRequest {
	private String sessionId;
	private UUID requestId;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public UUID getRequestId() {
		return requestId;
	}

	public void setRequestId(UUID requestId) {
		this.requestId = requestId;
	}
}
