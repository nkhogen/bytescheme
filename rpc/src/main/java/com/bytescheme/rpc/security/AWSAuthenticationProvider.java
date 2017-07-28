package com.bytescheme.rpc.security;

import com.bytescheme.common.utils.CryptoUtils;
import com.google.api.client.util.Preconditions;
import com.google.common.base.Function;

/**
 * Authenticates using AWS KMS. This is used for AWS internal services.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class AWSAuthenticationProvider extends BasicAuthenticationProvider {

  public AWSAuthenticationProvider(Function<String, AuthData> authDataProvider) {
    super(authDataProvider);
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    Preconditions.checkNotNull(authentication, "Invalid authentication object");
    Preconditions.checkNotNull(authentication.getUser(), "Invalid user");
    String user = authentication.getUser();
    String decryptedText = CryptoUtils.kmsDecrypt(authentication.getPassword());
    if (!user.equals(decryptedText)) {
      return null;
    }
    AuthData authData = authDataProvider.apply(user);
    return new Authentication(user, authentication.getPassword(),
        authData == null ? null : authData.getRoles());
  }
}
