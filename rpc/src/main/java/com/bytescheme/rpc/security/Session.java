package com.bytescheme.rpc.security;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;

/**
 * Session model.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class Session implements Delayed {
  private final Authentication authentication;
  private final String id;
  private final long creationTime;
  private long expiryTime;

  public Session(Authentication authentication, String id, long expiry) {
    Preconditions.checkNotNull(authentication);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
    Preconditions.checkArgument(expiry > System.currentTimeMillis());
    this.authentication = authentication;
    this.id = id;
    this.creationTime = System.currentTimeMillis();
    this.expiryTime = expiry;
  }

  public Authentication getAuthentication() {
    return authentication;
  }

  public String getId() {
    return id;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public long getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(long expiryTime) {
    this.expiryTime = expiryTime;
  }

  @Override
  public int compareTo(Delayed delayed) {
    Session session = (Session) delayed;
    return session.id.compareTo(session.id);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (getClass() != object.getClass()) {
      return false;
    }
    Session session = (Session) object;
    return compareTo(session) == 0;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
