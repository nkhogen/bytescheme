package com.bytescheme.common.utilities.graphs;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;

public class MemoryNodeProvider implements NodeProvider {
  private final Map<UUID, Node> nodes = new ConcurrentHashMap<UUID, Node>();
  private final Map<UUID, List<UUID>> nextNodes = new ConcurrentHashMap<UUID, List<UUID>>();

  @Override
  public Node addOrUpdateNode(Node node) {
    Preconditions.checkNotNull(node);
    Preconditions.checkNotNull(node.getId());
    nodes.put(node.getId(), node);
    return node;
  }

  @Override
  public Node deleteNode(UUID nodeId) {
    Preconditions.checkNotNull(nodeId);
    Node node = nodes.remove(nodeId);
    nextNodes.remove(nodeId);
    return node;
  }

  @Override
  public Node getNode(UUID nodeId) {
    Preconditions.checkNotNull(nodeId);
    return nodes.get(nodeId);
  }

  @Override
  public void addNextNode(UUID nodeId, UUID nextNodeId) {
    Preconditions.checkNotNull(nodeId);
    Preconditions.checkNotNull(nextNodeId);
    List<UUID> nextNodeIds = nextNodes.get(nodeId);
    if (nextNodeIds == null) {
      nextNodeIds = new LinkedList<UUID>();
      nextNodes.put(nodeId, nextNodeIds);
    }
    nextNodeIds.add(nextNodeId);
  }

  @Override
  public void deleteNextNode(UUID nodeId, UUID nextNodeId) {
    Preconditions.checkNotNull(nodeId);
    Preconditions.checkNotNull(nextNodeId);
    List<UUID> nextNodeIds = nextNodes.get(nodeId);
    if (nextNodeIds != null) {
      nextNodeIds.remove(nextNodeId);
      if (nextNodeIds.isEmpty()) {
        nextNodes.remove(nodeId);
      }
    }
  }

  @Override
  public Iterator<UUID> getNextNodeIdIterator(UUID nodeId) {
    Preconditions.checkNotNull(nodeId);
    List<UUID> nextUuids = nextNodes.get(nodeId);
    return nextUuids == null ? Collections.<UUID>emptyList().iterator() : nextUuids.iterator();
  }

  @Override
  public Iterator<UUID> getNodeIdIterator() {
    return nodes.keySet().iterator();
  }

}
