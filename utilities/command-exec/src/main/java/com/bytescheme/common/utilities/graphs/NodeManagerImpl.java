package com.bytescheme.common.utilities.graphs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

public class NodeManagerImpl implements NodeManager {
  private static final Logger LOG = LoggerFactory.getLogger(NodeManagerImpl.class);

  private static final NodeManagerImpl INSTANCE = new NodeManagerImpl();
  private final List<NodeProvider> nodeProviders = new ArrayList<NodeProvider>();

  private NodeManagerImpl() {

  }

  public static NodeManagerImpl getInstance() {
    return INSTANCE;
  }

  @Override
  public Node addOrUpdateNode(Node node) {
    Preconditions.checkNotNull(node);
    int index = getProviderIndex(node.getId(), nodeProviders.size());
    LOG.debug("Node found in provider index {}", index);
    return nodeProviders.get(index).addOrUpdateNode(node);
  }

  @Override
  public Node getNode(UUID nodeId) {
    Preconditions.checkNotNull(nodeId);
    int index = getProviderIndex(nodeId, nodeProviders.size());
    LOG.debug("Node found in provider index {}", index);
    return nodeProviders.get(index).getNode(nodeId);
  }

  @Override
  public Node deleteNode(UUID nodeId) {
    Preconditions.checkNotNull(nodeId);
    int index = getProviderIndex(nodeId, nodeProviders.size());
    LOG.debug("Node found in provider index {}", index);
    return nodeProviders.get(index).deleteNode(nodeId);
  }

  @Override
  public Node addNextNode(UUID nodeId, UUID nextNodeId) {
    Preconditions.checkNotNull(nodeId);
    Preconditions.checkNotNull(nextNodeId);
    int index = getProviderIndex(nodeId, nodeProviders.size());
    LOG.debug("Node found in provider index {}", index);
    int nextNodeIndex = getProviderIndex(nextNodeId, nodeProviders.size());
    LOG.debug("Next node found in provider index {}", nextNodeIndex);
    Node nextNode = nodeProviders.get(nextNodeIndex).getNode(nextNodeId);
    Preconditions.checkNotNull(nextNode);
    nodeProviders.get(index).addNextNode(nodeId, nextNodeId);
    return nextNode;
  }

  @Override
  public Node deleteNextNode(UUID nodeId, UUID nextNodeId) {
    Preconditions.checkNotNull(nodeId);
    Preconditions.checkNotNull(nextNodeId);
    int index = getProviderIndex(nodeId, nodeProviders.size());
    LOG.debug("Node found in provider index {}", index);
    int nextNodeIndex = getProviderIndex(nextNodeId, nodeProviders.size());
    LOG.debug("Next node found in provider index {}", nextNodeIndex);
    Node nextNode = nodeProviders.get(nextNodeIndex).getNode(nextNodeId);
    Preconditions.checkNotNull(nextNode);
    nodeProviders.get(index).deleteNextNode(nodeId, nextNodeId);
    return nextNode;
  }

  public void register(NodeProvider nodeProvider) {
    int size = nodeProviders.size();
    int newSize = size + 1;
    LOG.debug("Expandind node provider list");
    List<NodeProvider> newNodeProviders = new ArrayList<NodeProvider>(nodeProviders);
    newNodeProviders.add(nodeProvider);
    for (NodeProvider provider : nodeProviders) {
      Iterator<UUID> nodeIdIterator = provider.getNodeIdIterator();
      while (nodeIdIterator.hasNext()) {
        UUID nodeId = nodeIdIterator.next();
        int newProviderId = getProviderIndex(nodeId, newSize);
        int currProviderId = getProviderIndex(nodeId, size);
        NodeProvider newNodeProvider = newNodeProviders.get(newProviderId);
        NodeProvider currNodeProvider = nodeProviders.get(currProviderId);
        if (newNodeProvider == provider || newNodeProvider == currNodeProvider) {
          continue;
        }
        Node node = currNodeProvider.getNode(nodeId);
        LOG.debug("Moving node {} from node provider {} to node provider {}", node,
            currNodeProvider, newNodeProvider);
        newNodeProvider.addOrUpdateNode(node);
        Iterator<Node> nextNodeIterator = node.getNextNodeIterator();
        while (nextNodeIterator.hasNext()) {
          newNodeProvider.addNextNode(node.getId(), nextNodeIterator.next().getId());
        }
      }
    }
    // Delete old mapping
    for (NodeProvider provider : nodeProviders) {
      Iterator<UUID> nodeIdIterator = provider.getNodeIdIterator();
      while (nodeIdIterator.hasNext()) {
        UUID nodeId = nodeIdIterator.next();
        int newProviderId = getProviderIndex(nodeId, newSize);
        int currProviderId = getProviderIndex(nodeId, size);
        NodeProvider newNodeProvider = newNodeProviders.get(newProviderId);
        NodeProvider currNodeProvider = nodeProviders.get(currProviderId);
        if (newNodeProvider == provider || newNodeProvider == currNodeProvider) {
          continue;
        }
        currNodeProvider.deleteNode(nodeId);
        LOG.debug("Deleting node {} from old node provider {}", nodeId, currNodeProvider);
      }
    }
    nodeProviders.add(nodeProvider);
  }

  public void unregister(int nodeProviderId) {
    int size = nodeProviders.size();
    int newSize = size - 1;
    LOG.debug("Shrinking node provider list");
    Preconditions.checkArgument(nodeProviderId < size, "Index is not valid");
    Preconditions.checkArgument(newSize > 0, "Cannot remove the single provider");
    List<NodeProvider> newNodeProviders = new ArrayList<NodeProvider>(nodeProviders);
    newNodeProviders.remove(nodeProviderId);
    for (NodeProvider provider : nodeProviders) {
      Iterator<UUID> nodeIdIterator = provider.getNodeIdIterator();
      while (nodeIdIterator.hasNext()) {
        UUID nodeId = nodeIdIterator.next();
        int newProviderId = getProviderIndex(nodeId, newSize);
        int currProviderId = getProviderIndex(nodeId, size);
        NodeProvider newNodeProvider = newNodeProviders.get(newProviderId);
        NodeProvider currNodeProvider = nodeProviders.get(currProviderId);
        if (newNodeProvider == provider || newNodeProvider == currNodeProvider) {
          continue;
        }
        Node node = currNodeProvider.getNode(nodeId);
        newNodeProvider.addOrUpdateNode(node);
        Iterator<Node> nextNodeIterator = node.getNextNodeIterator();
        while (nextNodeIterator.hasNext()) {
          newNodeProvider.addNextNode(node.getId(), nextNodeIterator.next().getId());
        }
        LOG.debug("Moving node {} from node provider {} to node provider {}", node,
            currNodeProvider, newNodeProvider);
      }
    }
    for (NodeProvider provider : nodeProviders) {
      Iterator<UUID> nodeIdIterator = provider.getNodeIdIterator();
      while (nodeIdIterator.hasNext()) {
        UUID nodeId = nodeIdIterator.next();
        int newProviderId = getProviderIndex(nodeId, newSize);
        int currProviderId = getProviderIndex(nodeId, size);
        NodeProvider newNodeProvider = newNodeProviders.get(newProviderId);
        NodeProvider currNodeProvider = nodeProviders.get(currProviderId);
        if (newNodeProvider == provider || newNodeProvider == currNodeProvider) {
          continue;
        }
        currNodeProvider.deleteNode(nodeId);
        LOG.debug("Deleting node {} from old node provider {}", nodeId, currNodeProvider);
      }
    }
    nodeProviders.remove(nodeProviderId);
  }

  public NodeProvider getNodeProvider(UUID nodeId) {
    return nodeProviders.get(getProviderIndex(nodeId, nodeProviders.size()));
  }

  public int getProviderIndex(UUID nodeId, int size) {
    Preconditions.checkNotNull(nodeId);
    long diff = nodeId.getMostSignificantBits() ^ nodeId.getLeastSignificantBits();
    return Hashing.consistentHash(diff, size);
  }

  public int getNodeProvideSize() {
    return nodeProviders.size();
  }

}
