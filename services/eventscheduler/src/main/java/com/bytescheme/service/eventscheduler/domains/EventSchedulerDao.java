package com.bytescheme.service.eventscheduler.domains;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;

public class EventSchedulerDao {
  private static final Logger LOG = LoggerFactory.getLogger(EventSchedulerDao.class);

  private final DynamoDBMapper dbMapper;

  public EventSchedulerDao(DynamoDBMapper dbMapper) {
    this.dbMapper = Objects.requireNonNull(dbMapper);
  }

  public boolean save(Object model) {
    Objects.nonNull(model);
    boolean[] holder = new boolean[] { false };
    doWithRetry(t -> {
      try {
        dbMapper.save(model);
        holder[0] = true;
      } catch (ConditionalCheckFailedException e) {
        LOG.error("Failed to save the record");
      }
    });
    return holder[0];
  }

  public <T> T load(Class<T> modelClass, Object hashKey, Object rangeKey,
      boolean isConsistentRead) {
    Objects.nonNull(modelClass);
    Objects.nonNull(hashKey);
    Objects.nonNull(rangeKey);
    Object[] holder = new Object[1];
    DynamoDBMapperConfig dbMapperConfig = isConsistentRead
        ? DynamoDBMapperConfig.builder().withConsistentReads(ConsistentReads.CONSISTENT).build()
        : DynamoDBMapperConfig.DEFAULT;
    doWithRetry(t -> {
      holder[0] = dbMapper
          .load(modelClass, hashKey, rangeKey, dbMapperConfig);
    });
    return modelClass.cast(holder[0]);
  }

  public void scanEvents(UUID schedulerId, long endTime, Consumer<Event> consumer) {
    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
        .withFilterConditionEntry(
            Constants.TRIGGER_TIME_FIELD,
            new Condition().withComparisonOperator(ComparisonOperator.LE)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(endTime))))
        .withFilterConditionEntry(
            Constants.STATUS_FIELD,
            new Condition().withComparisonOperator(ComparisonOperator.NE)
                .withAttributeValueList(new AttributeValue().withS(Event.EventStatus.ENDED.name())))
        .withFilterConditionEntry(
            Constants.SCHEDULER_ID,
            new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(schedulerId.toString())));
    PaginatedScanList<Event> pageList = dbMapper.scan(Event.class, scanExpression);
    pageList.forEach(event -> {
      try {
        consumer.accept(event);
      } catch (Exception e) {
        LOG.error(String.format("Error publishing event %s ", event), e);
      }
    });
  }

  private void doWithRetry(Consumer<Long> consumer) {
    ExponentialBackOff backOff = new ExponentialBackOff.Builder().setInitialIntervalMillis(500)
        .setMaxElapsedTimeMillis(5000).setMaxIntervalMillis(1500).setMultiplier(1.5)
        .setRandomizationFactor(0.5).build();
    while (true) {
      RuntimeException exception = null;
      try {
        consumer.accept(backOff.getElapsedTimeMillis());
        break;
      } catch (AmazonServiceException e) {
        if (!"ThrottlingException".equals(e.getErrorCode())) {
          throw e;
        }
        exception = e;
      }

      try {
        long backOffTime = backOff.nextBackOffMillis();
        if (backOffTime == BackOff.STOP) {
          throw new RuntimeException("Retry limit exceeded", exception);
        }
        Thread.sleep(backOffTime);
      } catch (IOException | InterruptedException e) {
      }
    }
  }
}
