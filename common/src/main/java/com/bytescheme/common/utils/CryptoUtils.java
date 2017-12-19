package com.bytescheme.common.utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.Cipher;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * RSA crypto utils. The keys in DER format are base64 encoded. The encrypted
 * output is also base64 encoded.
 *
 * @author Naorem Khogendro Singh
 *
 */
public final class CryptoUtils {

  public static String KEY_ALIAS = "alias/authentication-key";

  public static final Encoder ENCODER = Base64.getEncoder();

  public static final Decoder DECODER = Base64.getDecoder();

  private static final String ALGORITHM = "RSA";

  private CryptoUtils() {

  }

  public static PublicKey getPublicKey(String keyFile) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(keyFile),
        "Invalid public key file");
    try {
      byte[] keyBytes = Files.readAllBytes(new File(keyFile).toPath());
      keyBytes = new String(keyBytes, "UTF-8").trim().getBytes();
      return getPublicKey(keyBytes);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get public key", e);
    }
  }

  public static PrivateKey getPrivateKey(String keyFile) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(keyFile),
        "Invalid private key file");
    try {
      byte[] keyBytes = Files.readAllBytes(new File(keyFile).toPath());
      keyBytes = new String(keyBytes, "UTF-8").trim().getBytes();
      return getPrivateKey(keyBytes);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get private key", e);
    }
  }

  public static PublicKey getPublicKey(byte[] keyBytes) {
    Preconditions.checkNotNull(keyBytes, "Invalid public key");
    try {
      X509EncodedKeySpec spec = new X509EncodedKeySpec(DECODER.decode(keyBytes));
      KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
      return keyFactory.generatePublic(spec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get public key", e);
    }
  }

  public static PrivateKey getPrivateKey(byte[] keyBytes) {
    Preconditions.checkNotNull(keyBytes, "Invalid private key");
    try {
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(DECODER.decode(keyBytes));
      KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get private key", e);
    }
  }

  public static String encrypt(String text, Key key) {
    Preconditions.checkNotNull(text, "Invalid text for encryption");
    Preconditions.checkNotNull(key, "Invalid key");
    String cipherText = null;
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      cipherText = ENCODER.encodeToString(cipher.doFinal(text.getBytes()));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to encrypt the text ", e);
    }
    return cipherText;
  }

  public static String decrypt(String text, Key key) {
    Preconditions.checkNotNull(text, "Invalid text for decryption");
    Preconditions.checkNotNull(key, "Invalid key");
    byte[] dectyptedText = null;
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, key);
      dectyptedText = cipher.doFinal(DECODER.decode(text));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to decrypt the text ", e);
    }
    return new String(dectyptedText);
  }

  public static String[] createKeyPair() {
    try {
      KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM);
      KeyPair pair = gen.generateKeyPair();
      String[] keys = new String[2];
      keys[0] = ENCODER.encodeToString(pair.getPrivate().getEncoded());
      keys[1] = ENCODER.encodeToString(pair.getPublic().getEncoded());
      return keys;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to generate RSA key pair", e);
    }
  }

  public static String kmsEncrypt(String plaintext) {
    Preconditions.checkNotNull(plaintext);
    AWSKMS kmsClient = AWSKMSClientBuilder.standard().withRegion(Regions.US_WEST_1)
        .build();
    EncryptRequest request = new EncryptRequest();
    request.setKeyId(KEY_ALIAS);
    ByteBuffer byteBuffer = ByteBuffer.wrap(plaintext.getBytes());
    request.setPlaintext(byteBuffer);
    EncryptResult result = kmsClient.encrypt(request);
    ByteBuffer cipherBlob = result.getCiphertextBlob().asReadOnlyBuffer();
    byte[] bytes = new byte[cipherBlob.remaining()];
    cipherBlob.get(bytes);
    return ENCODER.encodeToString(bytes);
  }

  public static String kmsDecrypt(String cipher) {
    Preconditions.checkNotNull(cipher);
    AWSKMS kmsClient = AWSKMSClientBuilder.standard().withRegion(Regions.US_WEST_1)
        .build();
    DecryptRequest request = new DecryptRequest();
    ByteBuffer byteBuffer = ByteBuffer.wrap(DECODER.decode(cipher));
    request.setCiphertextBlob(byteBuffer);
    DecryptResult result = kmsClient.decrypt(request);
    ByteBuffer plaintext = result.getPlaintext().asReadOnlyBuffer();
    byte[] bytes = new byte[plaintext.remaining()];
    plaintext.get(bytes);
    return new String(bytes);
  }

  public static void main(String[] args) {
    String[] keys = createKeyPair();
    String cipher = encrypt("Hello", getPublicKey(keys[1].getBytes()));
    System.out.println(cipher);
    System.out.println(decrypt(cipher, getPrivateKey(keys[0].getBytes())));
  }
}
