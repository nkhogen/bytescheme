package com.bytescheme.service.eventscheduler;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.EventSchedulerDao;
import com.bytescheme.service.eventscheduler.domains.ScannerMetadata;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

/**
 * Scans tables for events. Events table is scanned less frequently using a
 * metadata table.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class Scanner extends AbstractScheduledService {
  private static final Logger LOG = LoggerFactory.getLogger(Scanner.class);
  // Polling time every 5 secs to check the scan time
  private static final int POLLING_INTERVAL_SEC = 3;
  // Scanning is done every 5 mins unless smaller event is scheduled
  private static final int SCAN_INTERVAL_SEC = 300;
  private final Consumer<Event> consumer;

  private final UUID schedulerId;
  private final EventSchedulerDao eventSchedulerDao;

  public Scanner(
      UUID schedulerId,
      EventSchedulerDao eventSchedulerDao,
      Consumer<Event> consumer) {
    this.consumer = Objects.requireNonNull(consumer);
    this.schedulerId = Objects.requireNonNull(schedulerId);
    this.eventSchedulerDao = Objects.requireNonNull(eventSchedulerDao);
  }

  @Override
  protected void runOneIteration() throws Exception {
    LOG.info("Loading scanner meta data");
    ScannerMetadata scannerMetadata = eventSchedulerDao
        .load(ScannerMetadata.class, schedulerId, null, false);
    long currentTime = Instant.now().getEpochSecond();
    if (scannerMetadata == null) {
      scannerMetadata = new ScannerMetadata();
      scannerMetadata.setId(schedulerId);
      scannerMetadata.setScanTime(currentTime);
    }
    LOG.debug(
        "Current time {}, scan time {}",
        currentTime,
        scannerMetadata.getScanTime());
    if (currentTime >= scannerMetadata.getScanTime()) {
      long endTime = currentTime + SCAN_INTERVAL_SEC;
      LOG.info("Scanning for events upto {} ...", endTime);
      eventSchedulerDao.scanEvents(schedulerId, endTime, consumer);
      scannerMetadata.setScanTime(endTime);
      eventSchedulerDao.save(scannerMetadata);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0L, POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
  }

  public void stop() {
    Service service = super.stopAsync();
    service.awaitTerminated();
  }
}
