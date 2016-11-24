package com.bytescheme.rpc.security;

public interface AuthenticationProvider {
	Authentication authenticate(Authentication authentication);
}
