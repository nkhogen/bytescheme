package com.bytescheme.common.utilities.graphs;

import java.util.UUID;

public interface NodeManager {
  Node addOrUpdateNode(Node node);

  Node getNode(UUID nodeId);

  Node deleteNode(UUID nodeId);

  Node addNextNode(UUID nodeId, UUID nextNodeId);

  Node deleteNextNode(UUID nodeId, UUID nextNodeId);
}
