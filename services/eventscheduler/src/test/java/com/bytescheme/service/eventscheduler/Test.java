package com.bytescheme.service.eventscheduler;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.bytescheme.common.utils.Environment;
import com.bytescheme.service.eventscheduler.domains.Event;
import com.bytescheme.service.eventscheduler.domains.EventSchedulerDao;

public class Test {
  private static final UUID SCHEDULER_ID = new UUID(1L, 10L);

  public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    DynamoDBMapper dynamodbMapper = new DynamoDBMapper(
        AmazonDynamoDBClientBuilder.standard()
            .withCredentials(Environment.DEFAULT.getAwsCredentialsProvider()).build());
    EventSchedulerDao schedulerDao = new EventSchedulerDao(dynamodbMapper);
    Scheduler scheduler = new Scheduler(SCHEDULER_ID, schedulerDao, event -> {
      latch.countDown();
    });
    Scanner scanner = new Scanner(SCHEDULER_ID, schedulerDao, scheduler);
    scanner.startAsync();
    Event event = new Event();
    event.setDetails("test event 1");
    event.setTriggerTime(System.currentTimeMillis() + 20000);
    scheduler.schedule(event);
    latch.await();
  }
}
