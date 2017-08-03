package com.bytescheme.common.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Some basic util methods.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class BasicUtils {
  private static final SecureRandom RANDOM = new SecureRandom();

  private BasicUtils() {
  }

  public static Collection<?> deepCopy(Collection<?> collection) {
    if (collection == null) {
      return null;
    }
    Collection<Object> copy = collection instanceof Set ? Sets.newHashSet()
        : Lists.newArrayList();
    for (Object object : collection) {
      if (object instanceof Map) {
        copy.add(deepCopy((Map<?, ?>) object));
      } else if (object instanceof Collection) {
        copy.add(deepCopy((Collection<?>) object));
      } else {
        copy.add(object);
      }
    }
    return copy;
  }

  public static Map<?, ?> deepCopy(Map<?, ?> map) {
    if (map == null) {
      return null;
    }
    Map<Object, Object> copy = Maps.newHashMap();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      if (key instanceof Map) {
        key = deepCopy((Map<?, ?>) key);
      } else if (key instanceof Collection) {
        key = deepCopy((Collection<?>) key);
      }
      if (value instanceof Map) {
        value = deepCopy((Map<?, ?>) value);
      } else if (value instanceof Collection) {
        value = deepCopy((Collection<?>) value);
      }
      copy.put(key, value);
    }
    return copy;
  }

  public static String createSessionId() {
    return new BigInteger(500, RANDOM).toString(32);
  }
}
