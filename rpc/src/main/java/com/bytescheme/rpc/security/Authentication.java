package com.bytescheme.rpc.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Authentication model.
 * @author Naorem Khogendro Singh
 *
 */
public class Authentication {
	private final String user;
	private final String password;
	private final Set<String> roles;

	public Authentication(String user, String password) {
		this(user, password, null);
	}

	public Authentication(String user, String password, Set<String> roles) {
		this.user = user;
		this.password = password;
		this.roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<String>(roles));
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public Set<String> getRoles() {
		return roles;
	}
}
