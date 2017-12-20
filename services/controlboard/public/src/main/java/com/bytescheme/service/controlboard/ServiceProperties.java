package com.bytescheme.service.controlboard;

import java.util.UUID;

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
  private String googleClientId;
  private UUID schedulerId;
  private boolean enableMock;

  @PostConstruct
  public void init() {
    Preconditions.checkNotNull(!Strings.isNullOrEmpty(googleClientId), "Invalid Google client ID");
  }

  public String getGoogleClientId() {
    return googleClientId;
  }

  public void setGoogleClientId(String googleClientId) {
    this.googleClientId = googleClientId;
  }

  public UUID getSchedulerId() {
    return schedulerId;
  }

  public void setSchedulerId(UUID schedulerId) {
    this.schedulerId = schedulerId;
  }

  public boolean isEnableMock() {
    return enableMock;
  }

  public void setEnableMock(boolean enableMock) {
    this.enableMock = enableMock;
  }
}
