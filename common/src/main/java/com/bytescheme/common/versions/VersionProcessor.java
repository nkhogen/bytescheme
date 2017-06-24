package com.bytescheme.common.versions;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.bytescheme.common.utils.BasicUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Finds the changes in the object properties from the previous ones. Each
 * caller can get different deltas depending on the last seen version.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class VersionProcessor {
  private final AtomicReference<VersionNode> rootVersionNodeRef = new AtomicReference<VersionNode>();

  /**
   * Finds the changes.
   *
   * @param newData
   */
  public void process(Object newData) {
    long versionId = System.currentTimeMillis();
    VersionNode rootVersionNode = rootVersionNodeRef.get();
    Object currentObject = getObject(rootVersionNode);
    VersionNode newRootVersionNode = buildVersionNode(rootVersionNode, currentObject,
        newData, versionId);
    if (newRootVersionNode != null) {
      rootVersionNodeRef.compareAndSet(rootVersionNode, newRootVersionNode);
    }
  }

  /**
   * Get the delta changes from the last version.
   *
   * @param versionId
   * @return
   */
  public Entry<Long, DeltaNode> getLatest(long versionId) {
    VersionNode versionNode = rootVersionNodeRef.get();
    DeltaNode deltaNode = getLatest(versionNode, versionId);
    if (deltaNode == null) {
      return null;
    }
    return new SimpleEntry<Long, DeltaNode>(versionNode.getVersionId(), deltaNode);
  }

  /**
   * Merges the current data with the delta changes.
   *
   * @param currentData
   * @param deltaNode
   * @return
   */
  @SuppressWarnings("unchecked")
  public Object merge(Object currentData, DeltaNode deltaNode) {
    if (deltaNode == null) {
      return currentData;
    }
    if (deltaNode.isTerminal()) {
      return deltaNode.isDeleted() ? null : deltaNode.getData();
    }
    if (!(currentData instanceof Map)) {
      return deltaNode.getData();
    }
    Map<String, Object> map = Maps.newHashMap((Map<String, Object>) currentData);
    Map<String, DeltaNode> childNodes = deltaNode.getChildNodes();
    for (Map.Entry<String, DeltaNode> entry : childNodes.entrySet()) {
      Object childData = merge(map.get(entry.getKey()), entry.getValue());
      if (childData == null) {
        map.remove(entry.getKey());
      } else {
        map.put(entry.getKey(), childData);
      }
    }
    return map;
  }

  @VisibleForTesting
  VersionNode getVersionNode() {
    return rootVersionNodeRef.get();
  }

  private DeltaNode getLatest(VersionNode versionNode, long versionId) {
    DeltaNode deltaNode = null;
    if (versionNode != null && versionNode.getVersionId() > versionId) {
      if (versionNode.isTerminal()) {
        if (versionNode.isDeleted()) {
          deltaNode = DeltaNode.createDeleteNode();
        } else {
          deltaNode = DeltaNode.createUpdateNode(versionNode.getData());
        }
      } else {
        Map<String, DeltaNode> childDeltaNodes = Maps.newHashMap();
        Map<String, VersionNode> childVersionNodes = versionNode.getChildNodes();
        for (Map.Entry<String, VersionNode> entry : childVersionNodes.entrySet()) {
          DeltaNode childDeltaNode = getLatest(entry.getValue(), versionId);
          if (childDeltaNode != null) {
            childDeltaNodes.put(entry.getKey(), childDeltaNode);
          }
        }
        deltaNode = DeltaNode.createMergeNode(childDeltaNodes);
      }
    }
    return deltaNode;
  }

  private Object getObject(VersionNode versionNode) {
    if (versionNode == null) {
      return null;
    }
    if (versionNode.isTerminal()) {
      return versionNode.isDeleted() ? null : versionNode.getData();
    }
    Map<String, Object> map = Maps.newHashMap();
    Map<String, VersionNode> childVersionNodes = versionNode.getChildNodes();
    for (Map.Entry<String, VersionNode> entry : childVersionNodes.entrySet()) {
      Object childObject = getObject(entry.getValue());
      if (childObject != null) {
        map.put(entry.getKey(), childObject);
      }
    }
    return map;
  }

  private VersionNode buildVersionNode(VersionNode versionNode, Object currentData,
      Object newData, long versionId) {
    if (currentData == newData) {
      return null;
    }

    if (newData == null) {
      return VersionNode.createDeleted(versionId);
    }

    if (!(newData instanceof Map)) {
      if (newData.equals(currentData)) {
        return null;
      }
      if (newData instanceof Collection) {
        return VersionNode.createWithData(versionId,
            BasicUtils.deepCopy((Collection<?>) newData));
      }
      return VersionNode.createWithData(versionId, newData);
    }
    if (!(currentData instanceof Map)) {
      return VersionNode.createWithData(versionId,
          BasicUtils.deepCopy((Map<?, ?>) newData));
    }
    boolean isVersionChanged = false;
    Map<?, ?> currentMap = (Map<?, ?>) currentData;
    Map<?, ?> newMap = (Map<?, ?>) newData;
    Set<?> missingKeys = Sets.difference(currentMap.keySet(), newMap.keySet());
    Set<?> addedKeys = Sets.difference(newMap.keySet(), currentMap.keySet());
    Set<?> commonKeys = Sets.intersection(currentMap.keySet(), newMap.keySet());
    Map<String, VersionNode> newVersionNodeMap = Maps.newHashMap();
    for (Object key : missingKeys) {
      newVersionNodeMap.put((String) key, VersionNode.createDeleted(versionId));
      isVersionChanged = true;
    }
    for (Object key : addedKeys) {
      newVersionNodeMap.put((String) key,
          VersionNode.createWithData(versionId, newMap.get(key)));
      isVersionChanged = true;
    }
    Map<String, VersionNode> childVersionNodes = versionNode.getChildNodes();
    for (Object key : commonKeys) {
      VersionNode childVersionNode = childVersionNodes != null
          ? childVersionNodes.get(key) : null;
      if (childVersionNode == null) {
        childVersionNode = VersionNode.createWithData(versionNode.getVersionId(),
            newMap.get(key));
      }
      VersionNode newVersionNode = buildVersionNode(childVersionNode, currentMap.get(key),
          newMap.get(key), versionId);
      if (newVersionNode == null) {
        newVersionNodeMap.put((String) key, childVersionNode);
      } else {
        newVersionNodeMap.put((String) key, newVersionNode);
        isVersionChanged = true;
      }
    }
    return isVersionChanged
        ? VersionNode.createWithChildNodes(versionId, newVersionNodeMap) : null;
  }
}
