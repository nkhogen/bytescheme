package com.bytescheme.common.paths;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;

import com.bytescheme.common.properties.PropertyChangeListener;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * File based node provider. This class is meant for
 * FilePropertyChangePublisher.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DefaultNodeProvider
    implements Function<String, Node<String>>, PropertyChangeListener<Object> {
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

  @Override
  public Node<String> apply(String key) {
    Preconditions.checkNotNull(key, "Invalid key");
    Node<String> data = dataRef.get();
    if (data == null) {
      return null;
    }
    Map<String, Node<String>> map = Maps.newHashMap();
    for (Map.Entry<String, Node<String>> entry : data.getMap().entrySet()) {
      if (match(entry.getKey(), key)) {
        map.put(entry.getKey(), entry.getValue());
      }
    }
    return Node.withMap(map);
  }

  @Override
  public void onPropertyChange(Map<String, Object> changedProperties,
      Map<String, Object> allProperties) {
    transformAndSetData(allProperties);
  }

  protected boolean match(String input, String key) {
    Pattern pattern = Pattern.compile(input);
    Matcher matcher = pattern.matcher(key);
    return matcher.matches();
  }

  private void transformAndSetData(Map<String, Object> map) {
    Preconditions.checkArgument(MapUtils.isNotEmpty(map), "Invalid authorization data");
    dataRef.set(Node.withMap(Maps.transformValues(map, new NodeTransformer())));
  }
}
