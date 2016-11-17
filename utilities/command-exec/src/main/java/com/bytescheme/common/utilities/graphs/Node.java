package com.bytescheme.common.utilities.graphs;

import java.util.Iterator;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.gson.Gson;

public class Node {
  private UUID id;
  private Object data;

  public Node() {

  }

  public Node(Object data) {
    this(data, UUID.randomUUID());
  }

  public Node(Object data, UUID id) {
    this.data = data;
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public Iterator<Node> getNextNodeIterator() {
    return new NodeIterator();
  }

  private class NodeIterator extends AbstractIterator<Node> {
    public final NodeProvider nodeProvider;
    public final Iterator<UUID> nextNodeIdIterator;

    public NodeIterator() {
      this.nodeProvider = NodeManagerImpl.getInstance().getNodeProvider(id);
      Preconditions.checkNotNull(this.nodeProvider);
      this.nextNodeIdIterator = nodeProvider.getNextNodeIdIterator(id);
    }

    @Override
    public Node computeNext() {
      Node nextNode = null;
      do {
        if (!nextNodeIdIterator.hasNext()) {
          super.endOfData();
          return null;
        }
        UUID nextNodeId = nextNodeIdIterator.next();
        NodeProvider nodeProvider = NodeManagerImpl.getInstance().getNodeProvider(nextNodeId);
        Preconditions.checkNotNull(nodeProvider);
        nextNode = nodeProvider.getNode(nextNodeId);
        if (nextNode == null) {
          nodeProvider.deleteNextNode(id, nextNodeId);
        }
      } while (nextNode == null);
      return nextNode;
    }
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
