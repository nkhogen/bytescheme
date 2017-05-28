package com.bytescheme.common.paths;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.google.common.base.Preconditions;

/**
 * N-ary trees representation with multiple root nodes/keys.
 *
 * @author Naorem Khogendro Singh
 *
 * @param <T>
 */
public class Node<T> {
  private Map<String, Node<T>> map;
  private T value;

  private Node() {
  }

  public static <T> Node<T> createMapNode() {
    Node<T> data = new Node<T>();
    data.map = new HashMap<>();
    return data;
  }

  public static <T> Node<T> withMap(Map<String, Node<T>> map) {
    Preconditions.checkArgument(MapUtils.isNotEmpty(map));
    Node<T> data = new Node<T>();
    data.map = map;
    return data;
  }

  public static <T> Node<T> withValue(T value) {
    Preconditions.checkNotNull(value);
    Node<T> data = new Node<T>();
    data.value = value;
    return data;
  }

  public boolean isMap() {
    if (map != null) {
      return true;
    }
    return false;
  }

  public Map<String, Node<T>> getMap() {
    return map;
  }

  public T getValue() {
    return value;
  }
}
