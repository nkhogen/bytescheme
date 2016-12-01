package com.bytescheme.rpc.core;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Message encoder and decoder. This does the magic of identifying remote
 * objects and correctly deserializing Collection and Map generics.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class MessageCodec {
  private static final String OBJECT_ID_TAG = "::objId";
  private static final String GENERIC_TYPE_TAG = "::type";
  private static final String GENERIC_VALUE_TAG = "::value";
  private final RemoteObjectFactory remoteObjectFactory;
  private final RemoteObjectListener remoteObjectListener;
  private final Gson gson;

  public MessageCodec(RemoteObjectFactory remoteObjectFactory,
      RemoteObjectListener remoteObjectListener) {
    this.remoteObjectFactory = remoteObjectFactory;
    this.remoteObjectListener = remoteObjectListener;
    this.gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(RemoteObject.class, new RemoteObjectTypeAdapter())
        .registerTypeHierarchyAdapter(Collection.class, new CollectionTypeAdapter())
        .registerTypeHierarchyAdapter(Map.class, new MapTypeAdapter()).create();
  }

  private class RemoteObjectTypeAdapter
      implements JsonSerializer<RemoteObject>, JsonDeserializer<Object> {
    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      Preconditions.checkNotNull(remoteObjectFactory,
          "RemoteObject %s cannot be deserialized without RemoteObjectFactory",
          typeOfT.getTypeName());
      JsonObject object = (JsonObject) json;
      UUID objectId = context.deserialize(object.get(OBJECT_ID_TAG), UUID.class);
      try {
        RemoteObject remoteObject = remoteObjectFactory.createRemoteObject(
            (Class<RemoteObject>) Class.forName(typeOfT.getTypeName()), objectId);
        if (remoteObjectListener != null) {
          remoteObjectListener.onRemoteObjectFound(remoteObject);
        }
        return remoteObject;
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Unknown type " + typeOfT.getTypeName());
      }
    }

    @Override
    public JsonElement serialize(RemoteObject src, Type typeOfSrc,
        JsonSerializationContext context) {
      JsonObject object = new JsonObject();
      JsonElement element = context.serialize(src.getObjectId(), UUID.class);
      object.add(OBJECT_ID_TAG, element);
      if (remoteObjectListener != null) {
        remoteObjectListener.onRemoteObjectFound(src);
      }
      return object;
    }
  }

  private class CollectionTypeAdapter
      implements JsonSerializer<Collection<?>>, JsonDeserializer<Collection<?>> {

    @Override
    public Collection<?> deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      JsonArray array = (JsonArray) json;
      Collection<?> collection = new LinkedList<Object>();
      for (JsonElement item : array) {
        JsonObject innerObject = (JsonObject) item;
        String classname = innerObject.get(GENERIC_TYPE_TAG).getAsString();
        try {
          Class<?> clazz = Class.forName(classname);
          JsonElement element = innerObject.get(GENERIC_VALUE_TAG);
          collection.add(context.deserialize(element, clazz));
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException("Unknown type " + classname);
        }
      }
      return collection;
    }

    @Override
    public JsonElement serialize(Collection<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      JsonArray array = new JsonArray();
      for (Object item : src) {
        JsonObject innerObject = new JsonObject();
        JsonElement value = context.serialize(item, item.getClass());
        innerObject.add(GENERIC_VALUE_TAG, value);
        innerObject.addProperty(GENERIC_TYPE_TAG, item.getClass().getName());
        array.add(innerObject);
      }
      return array;
    }

  }

  private class MapTypeAdapter
      implements JsonSerializer<Map<String, ?>>, JsonDeserializer<Map<String, ?>> {

    @Override
    public Map<String, ?> deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      JsonObject object = (JsonObject) json;
      Map<String, ?> map = new LinkedHashMap<String, Object>();
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
        JsonObject innerObject = (JsonObject) entry.getValue();
        String classname = innerObject.get(GENERIC_TYPE_TAG).getAsString();
        try {
          Class<?> clazz = Class.forName(classname);
          JsonElement element = innerObject.get(GENERIC_VALUE_TAG);
          map.put(entry.getKey(), context.deserialize(element, clazz));
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException("Unknown type " + classname);
        }
      }
      return map;
    }

    @Override
    public JsonElement serialize(Map<String, ?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      JsonObject object = new JsonObject();
      for (Map.Entry<String, ?> entry : src.entrySet()) {
        JsonObject innerObject = new JsonObject();
        JsonElement value = context.serialize(entry.getValue(),
            entry.getValue().getClass());
        innerObject.add(GENERIC_VALUE_TAG, value);
        innerObject.addProperty(GENERIC_TYPE_TAG, entry.getValue().getClass().getName());
        object.add(entry.getKey(), innerObject);
      }
      return object;
    }

  }

  public <T> T getObject(String json, Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }

  public String getJson(Object object) {
    return gson.toJson(object);
  }
}
