package com.bytescheme.rpc.security;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FileAuthenticationProvider implements AuthenticationProvider {
	private static final Gson GSON = new Gson();
	private static final Type MAP_TYPE = new TypeToken<Map<String, AuthData>>() {
	}.getType();
	private Map<String, AuthData> authDataMap;

	public FileAuthenticationProvider(String jsonFile) throws IOException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(jsonFile), "Invalid JSON file");
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile)));
		try {
			this.authDataMap = GSON.fromJson(reader, MAP_TYPE);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	@Override
	public Authentication authenticate(Authentication authentication) {
		AuthData authData = authDataMap.get(authentication.getUser());
		if (authData == null) {
			return null;
		}
		if (!authData.getPassword().equals(authentication.getPassword())) {
			return null;
		}
		return new Authentication(authentication.getUser(), authData.getPassword(), authData.getRoles());
	}

	static class AuthData {
		private String password;
		private Set<String> roles;

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Set<String> getRoles() {
			return roles;
		}

		public void setRoles(Set<String> roles) {
			this.roles = roles;
		}
	}

}
