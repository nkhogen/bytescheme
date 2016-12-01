package com.bytescheme.rpc.security;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public interface AuthenticationProvider {
	Authentication authenticate(Authentication authentication);
}
