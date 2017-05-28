package com.bytescheme.common.utils;

import java.io.FileInputStream;
import java.io.InputStream;

import org.ini4j.Ini;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public enum Environment {
  DEFAULT("default");

  private static final String AWS_CREDENTIALS_FILE = String.format("%s/.aws/credentials",
      System.getProperty("user.home"));
  private static final String AWS_ACCESS_KEY_ID_OPTION = "aws_access_key_id";
  private static final String AWS_SECRET_KEY_OPTION = "aws_secret_access_key";

  // Section in Windows style INI.
  private final String section;

  private Environment(String section) {
    this.section = section;
  }

  public String getSection() {
    return section;
  }

  public AWSCredentials getAwsCredentials() {
    try (InputStream inputStream = new FileInputStream(AWS_CREDENTIALS_FILE)) {
      Ini ini = new Ini();
      ini.load(inputStream);
      return new AWSCredentials() {

        @Override
        public String getAWSAccessKeyId() {
          return ini.get(section, AWS_ACCESS_KEY_ID_OPTION);
        }

        @Override
        public String getAWSSecretKey() {
          return ini.get(section, AWS_SECRET_KEY_OPTION);
        }

      };
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public AWSCredentialsProvider getAwsCredentialsProvider() {
    return new AWSCredentialsProvider() {

      @Override
      public AWSCredentials getCredentials() {
        return getAwsCredentials();
      }

      @Override
      public void refresh() {
        // NOOP
      }     
    };
  }
}
