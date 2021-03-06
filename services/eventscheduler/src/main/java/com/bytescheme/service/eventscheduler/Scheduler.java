package com.bytescheme.service.eventscheduler;

import static com.bytescheme.service.eventscheduler.Scanner.POLLING_INTERVAL_SEC;
import static com.bytescheme.service.eventscheduler.Scanner.SCAN_INTERVAL_SEC;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.Event.EventStatus;
import com.bytescheme.service.eventscheduler.domains.SchedulerDao;
import com.bytescheme.service.eventscheduler.domains.ScannerMetadata;
import com.google.common.base.Preconditions;
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
  private static final int CANCELLED_EVENT_LIFETIME_SEC = 2 * SCAN_INTERVAL_SEC;

  private final ScheduledExecutorService scheduledExecutorService = Executors
      .newScheduledThreadPool(THREAD_POOL_SIZE);
  private final Map<UUID, ScheduledFuture<?>> futures = Maps.newConcurrentMap();
  private final SchedulerDao schedulerDao;
  private final UUID schedulerId;
  private final Consumer<Event> consumer;
  private final Scanner scanner;

  public Scheduler(
      UUID schedulerId,
      SchedulerDao eventSchedulerDao,
      Consumer<Event> consumer) {
    this.schedulerId = Objects.requireNonNull(schedulerId);
    this.schedulerDao = Objects.requireNonNull(eventSchedulerDao);
    this.consumer = Objects.requireNonNull(consumer);
    this.scanner = new Scanner(schedulerId, eventSchedulerDao, this);
  }

  public UUID getSchedulerId() {
    return schedulerId;
  }

  @Override
  public void accept(Event event) {
    ScheduledFuture<?> future = futures.get(event.getId());
    if (event.getStatus() == EventStatus.CANCELLED) {
      if (future != null) {
        LOG.info("Event {} is cancelled", event);
        future.cancel(true);
      }
      if (Instant.now().getEpochSecond()
          - event.getModifyTime() >= CANCELLED_EVENT_LIFETIME_SEC) {
        schedulerDao.delete(event);
      }
      return;
    }
    if (future != null) {
      LOG.info("Event {} is already scheduled", event);
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
    Preconditions.checkArgument(
        event.getTriggerTime() > currentTime + POLLING_INTERVAL_SEC,
        "Invalid event trigger time");
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

  // Cancel events from any scheduler
  public boolean cancel(UUID schedulerId, UUID eventId) {
    Event event = schedulerDao.load(
        Event.class,
        Objects.requireNonNull(eventId),
        Objects.requireNonNull(schedulerId),
        false);
    if (event == null) {
      return false;
    }
    event.setModifyTime(Instant.now().getEpochSecond());
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

  // Lists events from all schedulers
  public List<Event> list(String owner) {
    return schedulerDao.listEvents(Objects.requireNonNull(owner));
  }

  @PostConstruct
  public void start() {
    scanner.startAsync();
  }

  @PreDestroy
  public void stop() {
    scanner.stop();
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
      schedulerDao.delete(event);
    } else {
      scheduleEvent(event.getId(), MAX_EVENT_PROCESS_TIME_SEC, true);
      LOG.info("Another process has already taken this event {}", event);
      LOG.info("Waiting for it to complete");
    }
  }
}
