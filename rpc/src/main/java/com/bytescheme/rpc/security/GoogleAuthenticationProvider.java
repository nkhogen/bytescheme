package com.bytescheme.rpc.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.base.Preconditions;

/**
 * Google OAuth based authenticator. The roles come from a file.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class GoogleAuthenticationProvider extends FileAuthenticationProvider {
  private static final Logger LOG = LoggerFactory
      .getLogger(GoogleAuthenticationProvider.class);
  private static final String ISSUER = "accounts.google.com";
  private static final String CLIENT_ID = "91456297737-d1p2ha4n2847bpsrdrcp72uhp614ar9q.apps.googleusercontent.com";
  private final GoogleIdTokenVerifier verifier;

  public GoogleAuthenticationProvider(Map<String, AuthData> authDataMap)
      throws IOException, GeneralSecurityException {
    super(authDataMap);
    NetHttpTransport transport = new NetHttpTransport.Builder().doNotValidateCertificate()
        .build();
    verifier = new GoogleIdTokenVerifier.Builder(transport, new GsonFactory())
        .setAudience(Arrays.asList(CLIENT_ID))
        // If you retrieved the token on Android using the Play Services 8.3
        // API or newer, set
        // the issuer to "https://accounts.google.com". Otherwise, set the
        // issuer to
        // "accounts.google.com". If you need to verify tokens from multiple
        // sources, build
        // a GoogleIdTokenVerifier for each issuer and try them both.
        .setIssuer(ISSUER).build();
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    Preconditions.checkNotNull(authentication, "Invalid authentication object");
    Preconditions.checkNotNull(authentication.getUser(), "Invalid user");
    Map<String, AuthData> authDataMap = super.authDataMapRef.get();
    GoogleIdToken idToken = null;
    try {
      idToken = verifier.verify(authentication.getPassword());
    } catch (Exception e) {
      LOG.error("Exception in token verfication", e);
    }
    if (idToken == null) {
      return null;
    }
    Payload payload = idToken.getPayload();
    String email = payload.getEmail();
    AuthData authData = authDataMap.get(email);
    return new Authentication(email, authentication.getPassword(),
        authData == null ? null : authData.getRoles());
  }
}
