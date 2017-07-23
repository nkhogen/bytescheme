package com.bytescheme.common.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;

/**
 * Abstract implementation of PropertyChangePublisher. It polls property source
 * for changes every 10 secs. The first fetch is done in the ctor.
 *
 * @author Naorem Khogendro Singh
 *
 * @param <V>
 */
public abstract class AbstractPropertyChangePublisher<V> extends AbstractScheduledService
    implements PropertyChangePublisher<V> {
  private static final Logger LOG = LoggerFactory
      .getLogger(AbstractPropertyChangePublisher.class);
  private final int POLL_INTERVAL_SEC = 10;
  protected final Set<PropertyChangeListener<V>> listeners = Collections
      .newSetFromMap(new ConcurrentHashMap<>());
  private final AtomicReference<Map<String, V>> propertiesRef = new AtomicReference<>();

  public AbstractPropertyChangePublisher() {

  }

  protected void init() {
    try {
      Map<String, V> initialProperties = getProperties();
      Preconditions.checkNotNull(initialProperties, "Invalid properties received");
      propertiesRef.set(initialProperties);
      super.startAsync();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void runOneIteration() throws Exception {
    try {
      Map<String, V> latestProperties = getProperties();
      Preconditions.checkNotNull(latestProperties, "Invalid properties received");
      Map<String, V> properties = propertiesRef.get();
      if (properties.equals(latestProperties)) {
        return;
      }
      MapDifference<Object, Object> difference = Maps.difference(properties,
          latestProperties);
      Map<String, V> changedProperties = new HashMap<>();
      // Deleted ones
      for (Object key : difference.entriesOnlyOnLeft().keySet()) {
        changedProperties.put((String) key, null);
      }
      for (Map.Entry<Object, Object> entry : difference.entriesOnlyOnRight().entrySet()) {
        changedProperties.put((String) entry.getKey(), (V) entry.getValue());
      }
      for (Map.Entry<Object, MapDifference.ValueDifference<Object>> entry : difference
          .entriesDiffering().entrySet()) {
        Object key = entry.getKey();
        Object value = entry.getValue().rightValue();
        changedProperties.put((String) key, (V) value);
      }
      for (PropertyChangeListener<V> listener : listeners) {
        listener.onPropertyChange(changedProperties, latestProperties);
      }
      propertiesRef.set(latestProperties);
    } catch (Exception e) {
      LOG.error("Error in getting the properties", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, POLL_INTERVAL_SEC, TimeUnit.SECONDS);
  }

  @Override
  public void registerListener(PropertyChangeListener<V> listener) {
    Preconditions.checkNotNull(listener, "Invalid property change listener");
    Map<String, V> properties = propertiesRef.get();
    listener.onPropertyChange(properties, properties);
    listeners.add(listener);
  }

  @Override
  public void unregisterListener(PropertyChangeListener<V> listener) {
    Preconditions.checkNotNull(listener, "Invalid property change listener");
    listeners.remove(listener);
  }

  @Override
  public Map<String, V> getCurrentProperties() {
    return propertiesRef.get();
  }

  @Override
  public void shutDown() {
    try {
      super.shutDown();
    } catch (Exception e) {
      // Ignore
    }
  }

  protected abstract Map<String, V> getProperties() throws Exception;
}
