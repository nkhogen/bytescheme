package com.bytescheme.service.eventscheduler;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.Event.EventStatus;
import com.bytescheme.service.eventscheduler.domains.SchedulerDao;
import com.bytescheme.service.eventscheduler.domains.ScannerMetadata;
import com.google.common.collect.Maps;

/**
 * Scheduler consumes the events emitted by the Scanner and schedules them.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class Scheduler implements Consumer<Event> {
  private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

  // Maximum time taken to process an event once triggered
  private static final int MAX_EVENT_PROCESS_TIME_SEC = 10;

  private static final int THREAD_POOL_SIZE = 10;

  private final ScheduledExecutorService scheduledExecutorService = Executors
      .newScheduledThreadPool(THREAD_POOL_SIZE);
  private final Map<UUID, ScheduledFuture<?>> futures = Maps.newConcurrentMap();
  private final SchedulerDao schedulerDao;
  private final UUID schedulerId;
  private final Consumer<Event> consumer;

  public Scheduler(
      UUID schedulerId,
      SchedulerDao eventSchedulerDao,
      Consumer<Event> consumer) {
    this.schedulerId = Objects.requireNonNull(schedulerId);
    this.schedulerDao = Objects.requireNonNull(eventSchedulerDao);
    this.consumer = Objects.requireNonNull(consumer);
  }

  public UUID getSchedulerId() {
    return schedulerId;
  }

  @Override
  public void accept(Event event) {
    ScheduledFuture<?> future = futures.get(event.getId());
    if (future != null) {
      LOG.info("Event {} is already scheduled", event);
      if (event.getStatus() == EventStatus.CANCELLED) {
        LOG.info("Event {} is cancelled", event);
        future.cancel(true);
      }
      return;
    }
    long currentTime = Instant.now().getEpochSecond();
    long delay = currentTime >= event.getTriggerTime() ? 0
        : event.getTriggerTime() - currentTime;
    scheduleEvent(event.getId(), delay, false);
  }

  public boolean schedule(Event event) {
    Objects.requireNonNull(event);
    long currentTime = Instant.now().getEpochSecond();
    event.setSchedulerId(schedulerId);
    event.setId(UUID.randomUUID());
    event.setCreateTime(currentTime);
    event.setModifyTime(event.getCreateTime());
    event.setStatus(EventStatus.SCHEDULED);
    if (!schedulerDao.save(event)) {
      return false;
    }
    while (true) {
      boolean isChanged = false;
      ScannerMetadata scannerMetadata = schedulerDao
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
      if (schedulerDao.save(scannerMetadata)) {
        break;
      }
    }
    return true;
  }

  public boolean cancel(UUID eventId) {
    Event event = schedulerDao.load(Event.class, eventId, schedulerId, false);
    if (event == null) {
      return false;
    }
    event.setStatus(EventStatus.CANCELLED);
    if (!schedulerDao.save(event)) {
      return false;
    }
    ScheduledFuture<?> future = futures.get(event.getId());
    if (future != null) {
      future.cancel(true);
    }
    return true;
  }

  @PreDestroy
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  private void scheduleEvent(UUID eventId, long delay, boolean isOwner) {
    ScheduledFuture<?> future = scheduledExecutorService.schedule(() -> {
      boolean isRescheduled = false;
      try {
        Event event = schedulerDao.load(Event.class, eventId, schedulerId, true);
        if (event == null) {
          LOG.info("Event {} is not found", eventId);
        } else {
          if (event.getStatus() == EventStatus.SCHEDULED) {
            processEvent(event);
          } else if (event.getStatus() == EventStatus.STARTED) {
            if (isOwner) {
              processEvent(event);
            } else {
              scheduleEvent(eventId, MAX_EVENT_PROCESS_TIME_SEC, true);
              isRescheduled = true;
            }
          } else {
            LOG.info("Event {} is already cancelled or completed", event);
          }
        }
      } finally {
        if (!isRescheduled) {
          futures.remove(eventId);
        }
      }
    }, delay, TimeUnit.SECONDS);
    futures.put(eventId, future);
  }

  private void processEvent(Event event) {
    event.setModifyTime(Instant.now().getEpochSecond());
    event.setStatus(EventStatus.STARTED);
    if (schedulerDao.save(event)) {
      event = schedulerDao.load(Event.class, event.getId(), schedulerId, true);
      try {
        consumer.accept(event);
      } catch (Exception e) {
        LOG.error(String.format("Exception in sending the event %s", event), e);
      }
      event.setModifyTime(Instant.now().getEpochSecond());
      event.setStatus(EventStatus.ENDED);
      schedulerDao.save(event);
    } else {
      scheduleEvent(event.getId(), MAX_EVENT_PROCESS_TIME_SEC, true);
      LOG.info("Another process has already taken this event {}", event);
      LOG.info("Waiting for it to complete");
    }
  }
}
