package com.bytescheme.common.properties;

import java.util.Map;

/**
 * @author Naorem Khogendro Singh
 *
 * @param <V>
 */
public interface PropertyChangePublisher<V> {
  void registerListener(PropertyChangeListener<V> listener);

  void unregisterListener(PropertyChangeListener<V> listener);

  Map<String, V> getCurrentProperties();
}
