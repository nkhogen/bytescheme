package com.bytescheme.rpc.core;

import java.util.UUID;

public class LoginCallRequest {
	private String user;
	private String password;
	private UUID requestId;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public UUID getRequestId() {
		return requestId;
	}

	public void setRequestId(UUID requestId) {
		this.requestId = requestId;
	}
}
