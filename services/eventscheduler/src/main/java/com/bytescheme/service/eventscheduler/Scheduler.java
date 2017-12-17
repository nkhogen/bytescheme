package com.bytescheme.service.eventscheduler;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.Event.EventStatus;
import com.bytescheme.service.eventscheduler.domains.EventSchedulerDao;
import com.bytescheme.service.eventscheduler.domains.ScannerMetadata;

public class Scheduler implements Consumer<Event> {
  private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

  private static final int MAX_EVENT_PROCESS_TIME_SEC = 10;

  private final ScheduledExecutorService scheduledExecutorService = Executors
      .newScheduledThreadPool(4);
  private final Map<UUID, Event> events = Collections.synchronizedMap(new WeakHashMap<>());
  private final EventSchedulerDao eventSchedulerDao;
  private final UUID schedulerId;
  private final Consumer<Event> consumer;

  public Scheduler(UUID schedulerId, EventSchedulerDao eventSchedulerDao,
      Consumer<Event> consumer) {
    this.schedulerId = Objects.requireNonNull(schedulerId);
    this.eventSchedulerDao = Objects.requireNonNull(eventSchedulerDao);
    this.consumer = Objects.requireNonNull(consumer);
  }

  @Override
  public void accept(Event event) {
    if (events.containsKey(event.getId())) {
      LOG.info("Event {} is scheduled", event.getId());
      return;
    }
    updateEvent(event);
    long currentTime = System.currentTimeMillis();
    long delay = currentTime >= event.getTriggerTime() ? 0 : event.getTriggerTime() - currentTime;
    scheduleEvent(event.getId(), delay, false);
  }

  public void scheduleEvent(UUID eventId, long delay, boolean isOwner) {
    scheduledExecutorService.schedule(() -> {
      Event event = eventSchedulerDao.load(Event.class, eventId, schedulerId, true);
      if (event == null) {
        events.remove(eventId);
        LOG.info("Event {} is not found", eventId);
      } else {
        updateEvent(event);
        if (event.getStatus() == EventStatus.SCHEDULED) {
          processEvent(event);
        } else if (event.getStatus() == EventStatus.STARTED) {
          if (isOwner) {
            processEvent(event);
          } else {
            scheduleEvent(eventId, System.currentTimeMillis() + MAX_EVENT_PROCESS_TIME_SEC, true);
          }
        } else {
          events.remove(eventId);
          LOG.info("Event {} is already cancelled or completed", event.getId());
        }
      }
    }, delay, TimeUnit.SECONDS);
  }

  private void processEvent(Event event) {
    event.setModifyTime(System.currentTimeMillis());
    event.setStatus(EventStatus.STARTED);
    if (eventSchedulerDao.save(event)) {
      event = eventSchedulerDao.load(Event.class, event.getId(), schedulerId, true);
      updateEvent(event);
      consumer.accept(event);
      event.setModifyTime(System.currentTimeMillis());
      event.setStatus(EventStatus.ENDED);
      eventSchedulerDao.save(event);
      events.remove(event.getId());
    } else {
      scheduleEvent(event.getId(), System.currentTimeMillis() + MAX_EVENT_PROCESS_TIME_SEC, true);
      LOG.info("Another process has already taken this event {}", event.getId());
      LOG.info("Waiting for it to complete");
    }
  }

  private void updateEvent(Event event) {
    // Making weak reference work
    events.put(
        new UUID(event.getId().getMostSignificantBits(), event.getId().getLeastSignificantBits()),
        event);
  }

  public void schedule(Event event) {
    Objects.nonNull(event);
    event.setSchedulerId(schedulerId);
    event.setId(UUID.randomUUID());
    event.setCreateTime(System.currentTimeMillis());
    event.setModifyTime(event.getCreateTime());
    event.setStatus(EventStatus.SCHEDULED);
    if (eventSchedulerDao.save(event)) {
      while (true) {
        boolean isChanged = false;
        ScannerMetadata scannerMetadata = eventSchedulerDao
            .load(ScannerMetadata.class, schedulerId, null, false);
        if (scannerMetadata == null) {
          scannerMetadata = new ScannerMetadata();
          scannerMetadata.setId(schedulerId);
          scannerMetadata.setScanTime(event.getTriggerTime());
          isChanged = true;
        } else if (scannerMetadata.getScanTime() > event.getTriggerTime()) {
          scannerMetadata.setScanTime(event.getTriggerTime());
          isChanged = true;
        }
        if (!isChanged) {
          break;
        }
        if (eventSchedulerDao.save(scannerMetadata)) {
          break;
        }
      }
    }
  }
}
