package com.bytescheme.common.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * Abstract implementation of PropertyChangePublisher. It polls property source
 * for changes every 10 secs. The first fetch is done in the ctor.
 *
 * @author Naorem Khogendro Singh
 *
 * @param <V>
 */
public abstract class AbstractPropertyChangePublisher<V>
    implements PropertyChangePublisher<V> {
  private static final Logger LOG = LoggerFactory
      .getLogger(AbstractPropertyChangePublisher.class);
  private final int POLL_INTERVAL_SEC = 10;
  protected final Set<PropertyChangeListener<V>> listeners = Collections
      .newSetFromMap(new ConcurrentHashMap<>());
  private final AtomicReference<Map<String, V>> propertiesRef = new AtomicReference<>();
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4,
      runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName(AbstractPropertyChangePublisher.class.getSimpleName());
        thread.setDaemon(true);
        return thread;
      });
  private Future<?> future;

  public AbstractPropertyChangePublisher() {

  }

  @SuppressWarnings("unchecked")
  protected void init() {
    try {
      Map<String, V> initialProperties = getProperties();
      Preconditions.checkNotNull(initialProperties, "Invalid properties received");
      propertiesRef.set(initialProperties);
      future = EXECUTOR_SERVICE.submit(() -> {
        while (true) {
          try {
            Map<String, V> latestProperties = getProperties();
            Preconditions.checkNotNull(latestProperties, "Invalid properties received");
            Map<String, V> properties = propertiesRef.get();
            if (properties.equals(latestProperties)) {
              continue;
            }
            MapDifference<Object, Object> difference = Maps.difference(properties,
                latestProperties);
            Map<String, V> changedProperties = new HashMap<>();
            // Deleted ones
            for (Object key : difference.entriesOnlyOnLeft().keySet()) {
              changedProperties.put((String) key, null);
            }
            for (Map.Entry<Object, Object> entry : difference.entriesOnlyOnRight()
                .entrySet()) {
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
          } finally {
            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SEC);
          }
        }
      });
    } catch (RuntimeException e) {
      shutdown();
      throw e;
    } catch (Exception e) {
      shutdown();
      throw new RuntimeException(e);
    }
  }

  @Override
  public void registerListener(PropertyChangeListener<V> listener) {
    Preconditions.checkNotNull(listener, "Invalid property change listener");
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
  public void shutdown() {
    if (future != null) {
      future.cancel(true);
      future = null;
    }
  }

  protected abstract Map<String, V> getProperties() throws Exception;
}
