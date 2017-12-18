package com.bytescheme.service.controlboard.remoteobjects;

import java.net.MalformedURLException;
import java.util.Objects;
import java.util.UUID;

import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.service.controlboard.common.models.DeviceEventDetails;
import com.bytescheme.service.controlboard.common.models.DeviceEventScheduler;
import com.bytescheme.service.eventscheduler.Scheduler;
import com.bytescheme.service.eventscheduler.domains.Event;

public class DeviceEventSchedulerImpl implements DeviceEventScheduler {
  private static final long serialVersionUID = 1L;

  private final Scheduler scheduler;

  public DeviceEventSchedulerImpl(Scheduler scheduler) throws MalformedURLException {
    this.scheduler = Objects.requireNonNull(scheduler);
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

  public static Event createEvent(DeviceEventDetails eventDetails) {
    Objects.requireNonNull(eventDetails, "Invalid event details").validate();
    Event event = new Event();
    event.setTriggerTime(eventDetails.getTriggerTime());
    event.setDetails(JsonUtils.toJson(eventDetails));
    return event;
  }
}
