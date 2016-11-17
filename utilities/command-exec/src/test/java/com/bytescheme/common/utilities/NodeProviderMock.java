package com.bytescheme.common.utilities;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import com.bytescheme.common.utilities.graphs.Node;
import com.bytescheme.common.utilities.graphs.NodeManagerImpl;
import com.bytescheme.common.utilities.graphs.NodeProvider;
import com.google.common.base.Preconditions;

public class NodeProviderMock {
  public static NodeProvider getNodeProvider() {
    Map<UUID, Node> nodes = new ConcurrentHashMap<UUID, Node>();
    Map<UUID, List<UUID>> nextNodes = new ConcurrentHashMap<UUID, List<UUID>>();
    NodeProvider nodeProvider = mock(NodeProvider.class);
    doAnswer(invocation -> {
      Node node = (Node) invocation.getArguments()[0];
      Preconditions.checkNotNull(node.getId());
      nodes.put(node.getId(), node);
      return node;
    }).when(nodeProvider).addOrUpdateNode(any());
    doAnswer(invocation -> {
      UUID uuid = (UUID) invocation.getArguments()[0];
      Node node = nodes.remove(uuid);
      nextNodes.remove(uuid);
      return node;
    }).when(nodeProvider).deleteNode(any(UUID.class));
    doAnswer(invocation -> {
      UUID uuid = (UUID) invocation.getArguments()[0];
      return nodes.get(uuid);
    }).when(nodeProvider).getNode(any(UUID.class));
    doAnswer(invocation -> {
      UUID nodeId = (UUID) invocation.getArguments()[0];
      UUID nextNodeId = (UUID) invocation.getArguments()[1];
      List<UUID> nextNodeIds = nextNodes.get(nodeId);
      if (nextNodeIds == null) {
        nextNodeIds = new LinkedList<UUID>();
        nextNodes.put(nodeId, nextNodeIds);
      }
      nextNodeIds.add(nextNodeId);
      return null;
    }).when(nodeProvider).addNextNode(any(UUID.class), any(UUID.class));
    doAnswer(invocation -> {
      UUID nodeId = (UUID) invocation.getArguments()[0];
      UUID nextNodeId = (UUID) invocation.getArguments()[1];
      List<UUID> nextNodeIds = nextNodes.get(nodeId);
      if (nextNodeIds != null) {
        nextNodeIds.remove(nextNodeId);
        if (nextNodeIds.isEmpty()) {
          nextNodes.remove(nodeId);
        }
      }
      return null;
    }).when(nodeProvider).deleteNextNode(any(UUID.class), any(UUID.class));
    doAnswer(invocation -> {
      UUID uuid = (UUID) invocation.getArguments()[0];
      List<UUID> nextUuids = nextNodes.get(uuid);
      return nextUuids == null ? Collections.emptyList().iterator() : nextUuids.iterator();
    }).when(nodeProvider).getNextNodeIdIterator(any(UUID.class));
    doAnswer(invocation -> {
      return nodes.keySet().iterator();
    }).when(nodeProvider).getNodeIdIterator();
    return nodeProvider;
  }

  public static Node getNode(Object data) {
    return new Node(data);
  }

  // @Test
  public void test1() {
    NodeManagerImpl nodeManager = NodeManagerImpl.getInstance();
    NodeProvider nodeProvider1 = getNodeProvider();
    NodeProvider nodeProvider2 = getNodeProvider();
    NodeProvider nodeProvider3 = getNodeProvider();
    NodeProvider nodeProvider4 = getNodeProvider();
    NodeProvider nodeProvider5 = getNodeProvider();
    nodeManager.register(nodeProvider1);
    nodeManager.register(nodeProvider2);
    nodeManager.register(nodeProvider3);
    nodeManager.register(nodeProvider4);
    nodeManager.register(nodeProvider5);
    for (int i = 0; i < 100; i++) {
      nodeManager.addOrUpdateNode(getNode(i));
    }
    Iterator<UUID> iterator = nodeProvider1.getNodeIdIterator();
    while (iterator.hasNext()) {
      System.out.println(iterator.next());
    }
    System.out.println("##########################");
    iterator = nodeProvider2.getNodeIdIterator();
    while (iterator.hasNext()) {
      System.out.println(iterator.next());
    }
  }

  @Test
  public void test2() {
    NodeManagerImpl nodeManager = NodeManagerImpl.getInstance();
    NodeProvider nodeProvider1 = getNodeProvider();
    NodeProvider nodeProvider2 = getNodeProvider();
    NodeProvider nodeProvider3 = getNodeProvider();
    NodeProvider nodeProvider4 = getNodeProvider();
    NodeProvider nodeProvider5 = getNodeProvider();
    nodeManager.register(nodeProvider1);
    nodeManager.register(nodeProvider2);
    nodeManager.register(nodeProvider3);
    nodeManager.register(nodeProvider4);
    nodeManager.register(nodeProvider5);
    Node firstNode = null;
    List<UUID> nodeIds = new LinkedList<UUID>();
    for (int i = 0; i < 20; i++) {
      Node node = getNode(i);
      nodeManager.addOrUpdateNode(node);
      if (i == 0) {
        firstNode = node;
      }
      if (i >= 10) {
        nodeIds.add(node.getId());
      }
    }
    for (UUID nextNodeId : nodeIds) {
      nodeManager.addNextNode(firstNode.getId(), nextNodeId);
    }
    Iterator<Node> iterator = firstNode.getNextNodeIterator();
    while (iterator.hasNext()) {
      System.out.println(iterator.next());
    }
    System.out.println("##########################");
    while (nodeManager.getNodeProvideSize() > 1) {
      nodeManager.unregister(0);
    }
    iterator = firstNode.getNextNodeIterator();
    while (iterator.hasNext()) {
      System.out.println(iterator.next());
    }
    System.out.println("##########################");
    nodeManager.deleteNextNode(firstNode.getId(), nodeIds.get(0));
    iterator = firstNode.getNextNodeIterator();
    while (iterator.hasNext()) {
      System.out.println(iterator.next());
    }
  }

}
