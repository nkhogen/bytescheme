package com.bytescheme.common.versions;

import java.util.Map;
import java.util.Objects;

import com.bytescheme.common.utils.JsonUtils;

/**
 * 
 * @author Naorem Khogendro Singh
 *
 */
public class DeltaNode {
  enum Type {
    DELETE, UPDATE, MERGE
  }

  private final Type type;
  private final Object data;
  private final Map<String, DeltaNode> childNodes;

  private DeltaNode(Type type, Object data, Map<String, DeltaNode> childNodes) {
    this.type = type;
    this.data = data;
    this.childNodes = childNodes;
  }

  public static DeltaNode createUpdateNode(Object data) {
    return new DeltaNode(Type.UPDATE, data, null);
  }

  public static DeltaNode createDeleteNode() {
    return new DeltaNode(Type.DELETE, null, null);
  }

  public static DeltaNode createMergeNode(Map<String, DeltaNode> childNodes) {
    return new DeltaNode(Type.MERGE, null, childNodes);
  }

  public Type getType() {
    return type;
  }

  public Object getData() {
    return data;
  }

  public Map<String, DeltaNode> getChildNodes() {
    return childNodes;
  }

  public boolean isDeleted() {
    return data == null && childNodes == null;
  }

  public boolean isTerminal() {
    return childNodes == null;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (getClass() != object.getClass()) {
      return false;
    }
    DeltaNode other = (DeltaNode) object;
    if (type != other.getType()) {
      return false;
    }
    if (!Objects.equals(data, other.getData())) {
      return false;
    }
    if (!Objects.equals(childNodes, other.getChildNodes())) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return JsonUtils.toJson(this);
  }
}
