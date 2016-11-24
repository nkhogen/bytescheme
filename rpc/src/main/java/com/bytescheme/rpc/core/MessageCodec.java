package com.bytescheme.rpc.core;

import java.lang.reflect.Type;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MessageCodec implements JsonSerializer<RemoteObject>, JsonDeserializer<Object> {
  private static final String OBJECT_ID_TAG = ":objId";
  private final RemoteObjectFactory remoteObjectFactory;
  private final RemoteObjectListener remoteObjectListener;
  private final Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(RemoteObject.class, this)
      .registerTypeHierarchyAdapter(RemoteObject.class, this).create();

  public MessageCodec(RemoteObjectFactory remoteObjectFactory,
      RemoteObjectListener remoteObjectListener) {
    this.remoteObjectFactory = remoteObjectFactory;
    this.remoteObjectListener = remoteObjectListener;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Preconditions.checkNotNull(remoteObjectFactory,
        "RemoteObject %s cannot be deserialized without RemoteObjectFactory",
        typeOfT.getTypeName());
    JsonObject object = (JsonObject) json;
    UUID objectId = context.deserialize(object.get(OBJECT_ID_TAG), UUID.class);
    try {
			RemoteObject remoteObject = remoteObjectFactory
          .createRemoteObject((Class<RemoteObject>) Class.forName(typeOfT.getTypeName()), objectId);
      if (remoteObjectListener != null) {
        remoteObjectListener.onRemoteObjectFound(remoteObject);
      }
      return remoteObject;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Unknown type " + typeOfT.getTypeName());
    }
  }

  @Override
  public JsonElement serialize(RemoteObject src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject object = new JsonObject();
    JsonElement element = context.serialize(src.getObjectId(), UUID.class);
    object.add(OBJECT_ID_TAG, element);
    if (remoteObjectListener != null) {
      remoteObjectListener.onRemoteObjectFound(src);
    }
    return object;
  }

  public <T> T getObject(String json, Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }

  public String getJson(Object object) {
    return gson.toJson(object);
  }
}
