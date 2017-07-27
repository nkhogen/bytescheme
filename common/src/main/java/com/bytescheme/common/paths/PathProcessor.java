package com.bytescheme.common.paths;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * This class finds a string path like k1/k2/../kN in a tree. The matcher can be
 * a pattern matcher. Example application is in pattern based authorization.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class PathProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(PathProcessor.class);

  private final Function<String, Node<String>> dataProvider;

  public PathProcessor(Function<String, Node<String>> dataProvider) {
    this.dataProvider = Preconditions.checkNotNull(dataProvider, "Invalid data provider");
  }

  public Set<String> processPath(String key, String path) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "Invalid search path");
    Set<String> values = new HashSet<>();
    process(0, path, values, dataProvider.apply(key));
    return values;
  }

  protected boolean match(String input, String key) {
    Pattern pattern = Pattern.compile(input);
    Matcher matcher = pattern.matcher(key);
    return matcher.matches();
  }

  private void process(int startIndex, String path, Set<String> values,
      Node<String> data) {
    if (data == null) {
      return;
    }
    LOG.info("Path: {}", data);
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
}
