package com.bytescheme.common.paths;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;

import com.bytescheme.common.properties.PropertyChangeListener;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * This class finds a string path like k1/k2/../kN in a tree. The matcher can be
 * a pattern matcher. Example application is in pattern based authorization.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class PathProcessor implements PropertyChangeListener<Object> {
  private final AtomicReference<Node<String>> dataRef = new AtomicReference<>();

  public static class NodeTransformer implements Function<Object, Node<String>> {

    @SuppressWarnings("unchecked")
    @Override
    public Node<String> apply(Object input) {
      if (input instanceof String) {
        return Node.withValue((String) input);
      } else if (input instanceof Map) {
        return Node.withMap(Maps.transformValues((Map<String, Object>) input, this));
      }
      throw new IllegalArgumentException("Unknown type " + input.getClass());
    }

  }

  public PathProcessor(Node<String> data) {
    Preconditions.checkNotNull(data, "Invalid node data");
    this.dataRef.set(data);
  }

  public PathProcessor(Map<String, Object> map) {
    transformAndSetData(map);
  }

  public Set<String> procesPath(String path) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "Invalid search path");
    Set<String> values = new HashSet<>();
    process(0, path, values, dataRef.get());
    return values;
  }

  protected boolean match(String input, String key) {
    Pattern pattern = Pattern.compile(input);
    Matcher matcher = pattern.matcher(key);
    return matcher.matches();
  }

  private void transformAndSetData(Map<String, Object> map) {
    Preconditions.checkArgument(MapUtils.isNotEmpty(map), "Invalid authorization data");
    dataRef.set(
        Node.withMap(Maps.transformValues(map, new NodeTransformer())));
  }

  private void process(int startIndex, String path, Set<String> values,
      Node<String> data) {
    if (data == null) {
      return;
    }
    if (startIndex >= path.length()) {
      if (data.isMap()) {
        throw new IllegalArgumentException(String.format(
            "Incompatible data type %s. Map found but value expected", data.getClass()));
      }
      values.addAll(Arrays.asList(data.getValue().split(",", -1)));
      return;
    }
    if (!data.isMap()) {
      throw new IllegalArgumentException(String.format(
          "Incompatible data type %s. Value found but Map expected", data.getClass()));
    }
    int index = path.indexOf("/", startIndex);
    int endIndex = index < 0 ? path.length() : index;
    String dir = path.substring(startIndex, endIndex);
    Map<String, Node<String>> map = data.getMap();
    for (Map.Entry<String, Node<String>> entry : map.entrySet()) {
      if (match(entry.getKey(), dir)) {
        process(endIndex + 1, path, values, entry.getValue());
      }
    }
  }

  @Override
  public void onPropertyChange(Map<String, Object> changedProperties,
      Map<String, Object> allProperties) {
    transformAndSetData(allProperties);
  }
}
