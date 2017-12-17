package com.bytescheme.service.eventscheduler;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.bytescheme.common.utils.Environment;
import com.bytescheme.service.eventscheduler.Controller.EventDetails;
import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.EventSchedulerDao;
import com.google.common.base.Stopwatch;

/**
 * This demonstrates scheduling of events to turn on/off a device.
 * For HA, two instances can be used to run the scheduler.
 * To reduce AWS cost, I am running only one. Moreover,
 * the public controlboard is running on a single instance
 * just because of expense.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class Test {
  private static final UUID SCHEDULER_ID = new UUID(1L, 10L);

  public static void main(String[] args) throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createUnstarted();
    CountDownLatch latch = new CountDownLatch(2);
    Controller controller = new Controller();
    DynamoDBMapper dynamodbMapper = new DynamoDBMapper(
        AmazonDynamoDBClientBuilder.standard()
            .withCredentials(Environment.DEFAULT.getAwsCredentialsProvider()).build());
    EventSchedulerDao schedulerDao = new EventSchedulerDao(dynamodbMapper);
    Scheduler scheduler = new Scheduler(SCHEDULER_ID, schedulerDao, event -> {
      controller.accept(event);
      System.out.println(
          "Event triggered " + event.getDetails() + " "
              + stopwatch.elapsed(TimeUnit.MILLISECONDS));
      latch.countDown();
    });
    Scanner scanner = new Scanner(SCHEDULER_ID, schedulerDao, scheduler);
    scanner.startAsync();
    EventDetails eventDetails = new EventDetails();
    eventDetails.setDeviceId(0);
    eventDetails.setUser("abc@mymail.com");
    eventDetails.setPowerOn(false);
    Event event1 = Controller.createEvent(eventDetails, Instant.now().getEpochSecond() + 30);
    eventDetails.setPowerOn(true);
    Event event2 = Controller.createEvent(eventDetails, Instant.now().getEpochSecond() + 60);
    scheduler.schedule(event1);
    scheduler.schedule(event2);
    latch.await();
    System.out.println("Stopping...");
    scanner.stop();
    scheduler.stop();
  }
}
