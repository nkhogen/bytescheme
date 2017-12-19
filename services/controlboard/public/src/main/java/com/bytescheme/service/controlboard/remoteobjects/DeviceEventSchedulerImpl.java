package com.bytescheme.service.controlboard.remoteobjects;

import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.rpc.core.Constants;
import com.bytescheme.rpc.core.RemoteMethodCallException;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.common.models.DeviceEventDetails;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;
import com.bytescheme.service.eventscheduler.Scheduler;
import com.bytescheme.service.eventscheduler.domains.Event;

/**
 * Remote object to manage event scheduling.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DeviceEventSchedulerImpl implements DeviceEventScheduler {
  private static final long serialVersionUID = 1L;

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
  public boolean cancel(UUID eventId) {
    return scheduler.cancel(Objects.requireNonNull(eventId, "Invalid event ID"));
  }

  public Event createEvent(DeviceEventDetails eventDetails) {
    Objects.requireNonNull(eventDetails, "Invalid event details").validate();
    if (!securityProvider.getCurrentUser().equals(eventDetails.getUser())) {
      throw new RemoteMethodCallException(
          Constants.AUTHORIZATION_ERROR_CODE,
          "User is not authorized");
    }
    Event event = new Event();
    event.setTriggerTime(eventDetails.getTriggerTime());
    event.setDetails(JsonUtils.toJson(eventDetails));
    return event;
  }
}
