package com.bytescheme.common.properties;

import java.util.Map;

/**
 * @author Naorem Khogendro Singh
 *
 * @param <V>
 */
@FunctionalInterface
public interface PropertyChangeListener<V> {
  void onPropertyChange(Map<String, V> changedProperties, Map<String, V> allProperties);
}
