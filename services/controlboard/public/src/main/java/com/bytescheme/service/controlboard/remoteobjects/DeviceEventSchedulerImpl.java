package com.bytescheme.service.controlboard.remoteobjects;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.rpc.core.Constants;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.ConfigurationProvider;
import com.bytescheme.service.controlboard.common.models.DeviceEventDetails;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;
import com.bytescheme.service.controlboard.domains.ObjectEndpoint;
import com.bytescheme.service.eventscheduler.Scheduler;
import com.bytescheme.service.eventscheduler.domains.Event;
import com.google.common.base.Strings;

/**
 * Remote object to manage event scheduling.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceEventSchedulerImpl implements DeviceEventScheduler {
  private static final long serialVersionUID = 1L;

  @Autowired
  private ConfigurationProvider configurationProvider;

  @Autowired
  private SecurityProvider securityProvider;

  @Autowired
  private Scheduler scheduler;

  public SecurityProvider getSecurityProvider() {
    return securityProvider;
  }

  public void setSecurityProvider(SecurityProvider securityProvider) {
    this.securityProvider = securityProvider;
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  public void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public UUID getObjectId() {
    return scheduler.getSchedulerId();
  }

  @Override
  public boolean schedule(DeviceEventDetails eventDetails) {
    Objects.requireNonNull(eventDetails, "Invalid device event details").validate();
    return scheduler.schedule(createEvent(eventDetails));
  }

  @Override
  public boolean cancel(DeviceEventDetails eventDetails) {
    Objects.requireNonNull(eventDetails);
    if (!securityProvider.getCurrentUser().equals(eventDetails.getUser())) {
      throw new RemoteMethodCallException(
          Constants.AUTHORIZATION_ERROR_CODE,
          "User is not authorized");
    }
    return scheduler.cancel(
        Objects.requireNonNull(eventDetails.getSchedulerId(), "Invalid scheduler ID"),
        Objects.requireNonNull(eventDetails.getId(), "Invalid event ID"));
  }

  @Override
  public List<DeviceEventDetails> list() {
    ObjectEndpoint objectEndpoint = configurationProvider
        .getObjectEndPoint(securityProvider.getCurrentUser());
    return scheduler.list(objectEndpoint.getObjectId().toString()).stream().map(e -> {
      DeviceEventDetails eventDetails = JsonUtils
          .fromJson(e.getDetails(), DeviceEventDetails.class);
      eventDetails.setId(e.getId());
      eventDetails.setSchedulerId(e.getSchedulerId());
      return eventDetails;
    }).collect(Collectors.toList());
  }

  private Event createEvent(DeviceEventDetails eventDetails) {
    Objects.requireNonNull(eventDetails, "Invalid event details").validate();
    if (!Strings.isNullOrEmpty(eventDetails.getUser())
        && !securityProvider.getCurrentUser().equals(eventDetails.getUser())) {
      throw new RemoteMethodCallException(
          Constants.AUTHORIZATION_ERROR_CODE,
          "User is not authorized");
    }
    eventDetails.setUser(securityProvider.getCurrentUser());
    ObjectEndpoint objectEndpoint = configurationProvider.getObjectEndPoint(eventDetails.getUser());
    Event event = new Event();
    event.setOwner(objectEndpoint.getObjectId().toString());
    event.setTriggerTime(eventDetails.getTriggerTime());
    event.setDetails(JsonUtils.toJson(eventDetails));
    return event;
  }
}
