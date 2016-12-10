package com.bytescheme.service.controlboard;

import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

/**
 * Configurations from application.properties.
 *
 * @author Naorem Khogendro Singh
 *
 */
@ConfigurationProperties(prefix = "controlboard")
public class ServiceProperties {
  private String baseDir;
  private String sshKeysDir;
  private String authenticationJsonFile;
  private String videoUrlFormat;
  private Map<String, String> devices;
  private UUID objectId;
  private boolean enableMock;

  @PostConstruct
  public void init() {
    if (baseDir == null) {
      baseDir = System.getProperty("user.home");
    }
    if (sshKeysDir == null) {
      sshKeysDir = "/security/ssh-keys";
    }
    if (authenticationJsonFile == null) {
      authenticationJsonFile = "/security/auth/authentication.json";
    }
    Preconditions.checkNotNull(devices, "Invalid devices");
    Preconditions.checkNotNull(objectId, "Invalid object ID");
    Preconditions.checkNotNull(!Strings.isNullOrEmpty(videoUrlFormat),
        "Invalid video URL format");
  }

  public String getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
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

  public String getVideoUrlFormat() {
    return videoUrlFormat;
  }

  public void setVideoUrlFormat(String videoUrlFormat) {
    this.videoUrlFormat = videoUrlFormat;
  }

  public Map<String, String> getDevices() {
    return devices;
  }

  public void setDevices(Map<String, String> devices) {
    this.devices = devices;
  }

  public UUID getObjectId() {
    return objectId;
  }

  public void setObjectId(UUID objectId) {
    this.objectId = objectId;
  }

  public boolean isEnableMock() {
    return enableMock;
  }

  public void setEnableMock(boolean enableMock) {
    this.enableMock = enableMock;
  }
}
