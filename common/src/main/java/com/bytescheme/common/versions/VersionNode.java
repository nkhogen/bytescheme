package com.bytescheme.common.versions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;

import com.bytescheme.common.utils.JsonUtils;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public class VersionNode {
  private final long versionId;
  private final Object data;
  private final Map<String, VersionNode> childNodes;

  private VersionNode(long versionId, Object data, Map<String, VersionNode> childNodes) {
    this.versionId = versionId;
    this.data = data;
    this.childNodes = childNodes;
  }

  public static VersionNode createWithData(long versionId, Object data) {
    checkNotNull(data, "Invalid data");
    return new VersionNode(versionId, data, null);
  }

  public static VersionNode createWithChildNodes(long versionId,
      Map<String, VersionNode> childNodes) {
    checkNotNull(childNodes, "Invalid child nodes");
    return new VersionNode(versionId, null, childNodes);
  }

  public static VersionNode createDeleted(long versionId) {
    return new VersionNode(versionId, null, null);
  }

  public long getVersionId() {
    return versionId;
  }

  public Object getData() {
    return data;
  }

  public Map<String, VersionNode> getChildNodes() {
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
    VersionNode other = (VersionNode) object;
    if (versionId != other.getVersionId()) {
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
