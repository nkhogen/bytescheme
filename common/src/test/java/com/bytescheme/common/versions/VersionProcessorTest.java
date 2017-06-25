package com.bytescheme.common.versions;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.common.versions.DeltaNode.Type;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class VersionProcessorTest {

  @Test
  public void testMethods() throws IOException {
    VersionProcessor processor = new VersionProcessor();
    Map<String, Object> map = Maps.newHashMap();
    Map<String, Object> map1 = Maps.newHashMap();
    Map<String, Object> map2 = Maps.newHashMap();
    map1.put("key2", 1);
    map2.put("key3", 3);
    map1.put("key4", map2);
    map.put("i1", map1);
    map.put("i2", "hello");
    processor.process(map);
    Entry<Long, DeltaNode> entry = processor.getLatest(0);
    assertEquals(map, entry.getValue().getData());
    assertEquals(processor.getVersionNode().getVersionId(), entry.getKey().longValue());
    long oldVersion = entry.getKey();
    map1.put("key2", 1);
    map2.put("key3", 3);
    map1.put("key4", map2);
    map.put("i1", map1);
    map.put("i2", "hello");
    map.put("i3", "hello2");
    processor.process(map);
    entry = processor.getLatest(oldVersion);
    oldVersion = entry.getKey();
    assertEquals(Type.MERGE, entry.getValue().getType());
    Map<String, DeltaNode> childNode = ImmutableMap.of("i3",
        DeltaNode.createUpdateNode("hello2"));
    assertEquals(processor.getVersionNode().getVersionId(), entry.getKey().longValue());
    assertEquals(Type.MERGE, entry.getValue().getType());
    assertEquals(childNode, entry.getValue().getChildNodes());
    Map<String, Object> mapp = Maps.newHashMap();
    Map<String, Object> mapp1 = Maps.newHashMap();
    mapp1.put("key2", 1);
    mapp1.put("key4", map2);
    mapp.put("i1", map1);
    mapp.remove("i2");
    mapp.put("i3", "hello3");
    mapp.put("i4", Lists.newArrayList(1, 2, 3, 4));
    for (int i = 10; i < 100; i++) {
      mapp.put("key" + i, i);
    }
    processor.process(mapp);
    entry = processor.getLatest(oldVersion);
    Object mergedData = processor.merge(map, entry.getValue());
    assertEquals(mapp, mergedData);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    processor.writeLatest(oldVersion, outputStream);
    System.out.println(outputStream.toString());
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    mergedData = processor.merge(map, inputStream);
    System.out.println(JsonUtils.toJson(mergedData));
  }
}
