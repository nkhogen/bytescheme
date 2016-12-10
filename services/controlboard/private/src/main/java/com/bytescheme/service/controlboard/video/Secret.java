package com.bytescheme.service.controlboard.video;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

public class Secret implements Delayed {
  private final String secret;
  private final long expiryTime;

  public Secret(String secret, long expiryTime) {
    this.secret = secret;
    this.expiryTime = expiryTime;
  }

  public String getSecret() {
    return secret;
  }

  @Override
  public int compareTo(Delayed delayed) {
    Secret secret = (Secret) delayed;
    return this.secret.compareTo(secret.secret);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (getClass() != object.getClass()) {
      return false;
    }
    Secret secret = (Secret) object;
    return compareTo(secret) == 0;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public int hashCode() {
    return secret.hashCode();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
