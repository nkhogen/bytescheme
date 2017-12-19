package com.bytescheme.rpc.security;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
@FunctionalInterface
public interface AuthenticationProvider {
  Authentication authenticate(Authentication authentication);
}
