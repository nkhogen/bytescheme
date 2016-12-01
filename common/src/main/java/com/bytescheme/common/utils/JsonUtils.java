package com.bytescheme.common.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Common JSON conversion util class.
 *
 * @author Naorem Khogendro Singh
 *
 */
public final class JsonUtils {
  public static final Gson GSON = new Gson();
  public static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
  }.getType();

  private JsonUtils() {
  }

  public static String toJson(Object object) {
    return GSON.toJson(object);
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    return GSON.fromJson(json, clazz);
  }

  public static <T> T fromJsonFile(String file, Class<T> clazz) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(file), "Invalid JSON file");
    Preconditions.checkNotNull(clazz, "Invalid class");
    InputStream stream = null;
    try {
      stream = new FileInputStream(file);
      return GSON.fromJson(new BufferedReader(new InputStreamReader(stream, "UTF-8")),
          clazz);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  public static <V> Map<String, V> mapFromJson(String json, Class<V> valueClazz) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(json), "Invalid JSON");
    Preconditions.checkNotNull(valueClazz, "Invalid value class");
    Map<String, Object> map = GSON.fromJson(json, MAP_TYPE);
    return Maps.transformValues(map, new Function<Object, V>() {
      @Override
      public V apply(Object input) {
        return fromJson(toJson(input), valueClazz);
      }
    });
  }

  public static <V> Map<String, V> mapFromJsonFile(String file, Class<V> valueClazz) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(file), "Invalid JSON file");
    Preconditions.checkNotNull(valueClazz, "Invalid value class");
    InputStream stream = null;
    try {
      stream = new FileInputStream(file);
      Map<String, Object> map = GSON
          .fromJson(new BufferedReader(new InputStreamReader(stream, "UTF-8")), MAP_TYPE);
      return Maps.transformValues(map, new Function<Object, V>() {
        @Override
        public V apply(Object input) {
          return fromJson(toJson(input), valueClazz);
        }
      });
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }
}
