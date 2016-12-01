package com.bytescheme.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * RSA crypto utils. The keys in DER format are base64 encoded. The encrypted
 * output is also base64 encoded.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class CryptoUtils {
  private static final String ALGORITHM = "RSA";
  private static final Encoder ENCODER = Base64.getEncoder();
  private static final Decoder DECODER = Base64.getDecoder();

  private CryptoUtils() {

  }

  public static PublicKey getPublicKey(String keyFile) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(keyFile),
        "Invalid public key file");
    try {
      byte[] keyBytes = Files.readAllBytes(new File(keyFile).toPath());
      keyBytes = new String(keyBytes, "UTF-8").trim().getBytes();
      X509EncodedKeySpec spec = new X509EncodedKeySpec(DECODER.decode(keyBytes));
      KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
      return keyFactory.generatePublic(spec);
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
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(DECODER.decode(keyBytes));
      KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
      return kf.generatePrivate(spec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get p key", e);
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
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      KeyPair pair = gen.generateKeyPair();
      ByteArrayOutputStream streamPvt = new ByteArrayOutputStream();
      streamPvt.write(pair.getPrivate().getEncoded());
      ByteArrayOutputStream streamPub = new ByteArrayOutputStream();
      streamPub.write(pair.getPublic().getEncoded());
      String[] keys = new String[2];
      keys[0] = ENCODER.encodeToString(streamPvt.toByteArray());
      keys[1] = ENCODER.encodeToString(streamPub.toByteArray());
      return keys;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to generate RSA key pair", e);
    }
  }
}
