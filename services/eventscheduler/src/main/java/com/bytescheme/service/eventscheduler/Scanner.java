package com.bytescheme.service.eventscheduler;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.ScannerMetadata;
import com.bytescheme.service.eventscheduler.domains.SchedulerDao;
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
  private final SchedulerDao schedulerDao;

  public Scanner(UUID schedulerId, SchedulerDao eventSchedulerDao, Consumer<Event> consumer) {
    this.consumer = Objects.requireNonNull(consumer);
    this.schedulerId = Objects.requireNonNull(schedulerId);
    this.schedulerDao = Objects.requireNonNull(eventSchedulerDao);
  }

  @Override
  protected void runOneIteration() throws Exception {
    try {
      LOG.info("Loading scanner meta data");
      ScannerMetadata scannerMetadata = schedulerDao
          .load(ScannerMetadata.class, schedulerId, null, false);
      long currentTime = Instant.now().getEpochSecond();
      if (scannerMetadata == null) {
        scannerMetadata = new ScannerMetadata();
        scannerMetadata.setId(schedulerId);
        scannerMetadata.setScanTime(currentTime);
      }
      LOG.debug("Current time {}, scan time {}", currentTime, scannerMetadata.getScanTime());
      if (currentTime >= scannerMetadata.getScanTime()) {
        long endTime = currentTime + SCAN_INTERVAL_SEC;
        LOG.info("Scanning for events upto {} ...", endTime);
        schedulerDao.scanEvents(schedulerId, endTime, consumer);
        scannerMetadata.setScanTime(endTime);
        schedulerDao.save(scannerMetadata);
      }
    } catch (Exception e) {
      LOG.error("Exception while scanning...", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0L, POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
  }

  public void start() {
    super.startAsync();
  }

  public void stop() {
    Service service = super.stopAsync();
    service.awaitTerminated();
  }
}
