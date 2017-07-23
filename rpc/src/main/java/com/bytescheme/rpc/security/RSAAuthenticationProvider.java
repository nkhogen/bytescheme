package com.bytescheme.rpc.security;

import java.io.File;
import java.io.IOException;

import com.bytescheme.common.utils.CryptoUtils;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.Preconditions;
import com.google.common.base.Function;

/**
 * RSA key based authentication provider. The roles come from a file.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class RSAAuthenticationProvider extends BasicAuthenticationProvider {
  private final File keyFolder;

  public RSAAuthenticationProvider(String keyFolder, Function<String, AuthData> authDataProvider)
      throws IOException {
    super(authDataProvider);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(keyFolder), "Invalid key folder");
    this.keyFolder = new File(keyFolder);
    Preconditions.checkArgument(this.keyFolder.exists() && this.keyFolder.isDirectory()
        && this.keyFolder.canRead(), "Invalid key folder %s", keyFolder);
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    Preconditions.checkNotNull(authentication, "Invalid authentication object");
    Preconditions.checkNotNull(authentication.getUser(), "Invalid user");
    String user = authentication.getUser();
    String decryptedText = CryptoUtils.decrypt(authentication.getPassword(),
        CryptoUtils.getPrivateKey(keyFolder + File.separator + user));
    if (!user.equals(decryptedText)) {
      return null;
    }
    AuthData authData = authDataProvider.apply(user);
    return new Authentication(user, authentication.getPassword(),
        authData == null ? null : authData.getRoles());
  }
}
