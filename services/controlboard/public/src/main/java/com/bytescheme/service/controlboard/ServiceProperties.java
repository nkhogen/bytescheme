package com.bytescheme.service.controlboard;

import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

/**
 * Service configuration properties from application.properties.
 * 
 * @author Naorem Khogendro Singh
 *
 */
@ConfigurationProperties(prefix = "controlboard")
public class ServiceProperties {
  private List<String> googleClientIds;
  private UUID schedulerId;
  private boolean enableMock;

  @PostConstruct
  public void init() {
    Preconditions.checkNotNull(
        CollectionUtils.isNotEmpty(googleClientIds),
        "Invalid Google client ID");
  }

  public List<String> getGoogleClientIds() {
    return googleClientIds;
  }

  public void setGoogleClientIds(List<String> googleClientIds) {
    this.googleClientIds = googleClientIds;
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
