package com.bytescheme.common.utilities.graphs;

import java.util.Iterator;
import java.util.UUID;

public interface NodeProvider {

  Node addOrUpdateNode(Node node);

  Node deleteNode(UUID nodeId);

  Node getNode(UUID nodeId);

  void addNextNode(UUID nodeId, UUID nextNodeId);

  void deleteNextNode(UUID nodeId, UUID nextNodeId);

  Iterator<UUID> getNextNodeIdIterator(UUID nodeId);

  Iterator<UUID> getNodeIdIterator();
}
