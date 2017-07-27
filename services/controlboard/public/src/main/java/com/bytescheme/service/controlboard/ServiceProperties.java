package com.bytescheme.service.controlboard;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

/**
 * Service configuration properties from application.properties.
 * 
 * @author Naorem Khogendro Singh
 *
 */
@ConfigurationProperties(prefix = "controlboard")
public class ServiceProperties {
  private String baseDir;
  private String googleClientId;
  private String objectsJsonFile;
  private String endpointsJsonFile;
  private String sshKeysDir;
  private String authenticationJsonFile;
  private String authorizationJsonFile;
  private boolean enableMock;
  private boolean enableFileConfig;

  @PostConstruct
  public void init() {
    if (baseDir == null) {
      baseDir = System.getProperty("user.home");
    }
    if (objectsJsonFile == null) {
      objectsJsonFile = "/objects/objects.json";
    }
    if (endpointsJsonFile == null) {
      endpointsJsonFile = "/objects/endpoints.json";
    }
    if (sshKeysDir == null) {
      sshKeysDir = "/security/ssh-keys";
    }
    if (authenticationJsonFile == null) {
      authenticationJsonFile = "/security/auth/authentication.json";
    }
    if (authorizationJsonFile == null) {
      authorizationJsonFile = "/security/auth/authorization.json";
    }
    Preconditions.checkNotNull(!Strings.isNullOrEmpty(googleClientId),
        "Invalid Google client ID");
  }

  public String getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
  }

  public String getGoogleClientId() {
    return googleClientId;
  }

  public void setGoogleClientId(String googleClientId) {
    this.googleClientId = googleClientId;
  }

  public String getObjectsJsonFile() {
    return objectsJsonFile;
  }

  public void setObjectsJsonFile(String objectsJsonFile) {
    this.objectsJsonFile = objectsJsonFile;
  }

  public String getEndpointsJsonFile() {
    return endpointsJsonFile;
  }

  public void setEndpointsJsonFile(String endpointsJsonFile) {
    this.endpointsJsonFile = endpointsJsonFile;
  }

  public String getSshKeysDir() {
    return sshKeysDir;
  }

  public void setSshKeysDir(String sshKeysDir) {
    this.sshKeysDir = sshKeysDir;
  }

  public String getAuthenticationJsonFile() {
    return authenticationJsonFile;
  }

  public void setAuthenticationJsonFile(String authenticationJsonFile) {
    this.authenticationJsonFile = authenticationJsonFile;
  }

  public String getAuthorizationJsonFile() {
    return authorizationJsonFile;
  }

  public void setAuthorizationJsonFile(String authorizationJsonFile) {
    this.authorizationJsonFile = authorizationJsonFile;
  }

  public boolean isEnableMock() {
    return enableMock;
  }

  public void setEnableMock(boolean enableMock) {
    this.enableMock = enableMock;
  }

  public boolean isEnableFileConfig() {
    return enableFileConfig;
  }

  public void setEnableFileConfig(boolean enableFileConfig) {
    this.enableFileConfig = enableFileConfig;
  }
}
