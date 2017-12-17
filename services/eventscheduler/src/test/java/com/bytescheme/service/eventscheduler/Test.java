package com.bytescheme.service.eventscheduler;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.bytescheme.common.utils.Environment;
import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.EventSchedulerDao;
import com.google.common.base.Stopwatch;

/**
 * @author Naorem Khogendro Singh
 *
 */
public class Test {
  private static final UUID SCHEDULER_ID = new UUID(1L, 10L);

  public static void main(String[] args) throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createUnstarted();
    CountDownLatch latch = new CountDownLatch(1);
    DynamoDBMapper dynamodbMapper = new DynamoDBMapper(
        AmazonDynamoDBClientBuilder.standard()
            .withCredentials(Environment.DEFAULT.getAwsCredentialsProvider()).build());
    EventSchedulerDao schedulerDao = new EventSchedulerDao(dynamodbMapper);
    Scheduler scheduler = new Scheduler(SCHEDULER_ID, schedulerDao, event -> {
      System.out.println(
          "Event triggered " + event.getDetails() + " "
              + stopwatch.elapsed(TimeUnit.MILLISECONDS));
      latch.countDown();
    });
    Scanner scanner = new Scanner(SCHEDULER_ID, schedulerDao, scheduler);
    scanner.startAsync();
    Event event = new Event();
    event.setDetails("test event 1");
    event.setTriggerTime(Instant.now().getEpochSecond() + 30);
    stopwatch.start();
    scheduler.schedule(event);
    latch.await();
    System.out.println("Stopping...");
    scanner.stop();
    scheduler.stop();
  }
}
