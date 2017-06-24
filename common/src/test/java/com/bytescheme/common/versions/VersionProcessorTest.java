package com.bytescheme.common.versions;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.bytescheme.common.versions.DeltaNode.Type;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class VersionProcessorTest {

  @Test
  public void testMethods() {
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
    Map<String, Object> mapp2 = Maps.newHashMap();
    mapp1.put("key2", 1);
    for (int i = 0; i < 100; i++) {
      mapp2.put("key" + i, i);
    }
    mapp1.put("key4", map2);
    mapp.put("i1", map1);
    mapp.remove("i2");
    mapp.put("i3", "hello3");
    processor.process(mapp);
    entry = processor.getLatest(oldVersion);
    Object mergedData = processor.merge(map, entry.getValue());
    assertEquals(mergedData, mapp);
  }
}
