package com.bytescheme.rpc.security;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.bytescheme.common.utils.CryptoUtils;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.Preconditions;

/**
 * RSA key based authentication provider. The roles come from a file.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class RSAAuthenticationProvider extends FileAuthenticationProvider {
  private final File keyFolder;

  public RSAAuthenticationProvider(String keyFolder, Map<String, AuthData> authDataMap)
      throws IOException {
    super(authDataMap);
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
    Map<String, AuthData> authDataMap = super.authDataMapRef.get();
    AuthData authData = authDataMap.get(user);
    return new Authentication(user, authentication.getPassword(),
        authData == null ? null : authData.getRoles());
  }
}
