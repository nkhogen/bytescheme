package com.bytescheme.rpc.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PathProcessor {
	private static final Gson GSON = new Gson();
	private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
	}.getType();
	private final Node<String> data;

	public static class NodeTransformer implements Function<Object, Node<String>> {

		@SuppressWarnings("unchecked")
		@Override
		public Node<String> apply(Object input) {
			if (input instanceof String) {
				return Node.withValue((String) input);
			} else if (input instanceof Map) {
				return Node.withMap(Maps.transformValues((Map<String, Object>) input, this), String.class);
			}
			throw new IllegalArgumentException("Unknown type " + input.getClass());
		}

	}

	public PathProcessor(Node<String> data) {
		Preconditions.checkNotNull(data, "Invalid node data");
		this.data = data;
	}

	public PathProcessor(Map<String, Object> map) {
		Preconditions.checkArgument(MapUtils.isNotEmpty(map), "Invalid authorization data");
		this.data = Node.withMap(Maps.transformValues(map, new NodeTransformer()), String.class);
	}

	public PathProcessor(String jsonFile) throws IOException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(jsonFile), "Invalid JSON file");
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile)));
		try {
			Map<String, Object> map = GSON.fromJson(reader, MAP_TYPE);
			this.data = Node.withMap(Maps.transformValues(map, new NodeTransformer()), String.class);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public Set<String> procesPath(String path) {
		Set<String> values = new HashSet<>();
		process(0, path, values, data);
		return values;
	}

	protected boolean match(String input, String key) {
		Pattern pattern = Pattern.compile(input);
		Matcher matcher = pattern.matcher(key);
		return matcher.matches();
	}

	private void process(int startIndex, String path, Set<String> values, Node<String> data) {
		if (data == null) {
			return;
		}
		if (startIndex >= path.length()) {
			if (data.isMap()) {
				throw new IllegalArgumentException(
						String.format("Incompatible data type %s. Map found but value expected", data.getClass()));
			}
			values.addAll(Arrays.asList(data.getValue().split(",", -1)));
			return;
		}
		if (!data.isMap()) {
			throw new IllegalArgumentException(
					String.format("Incompatible data type %s. Value found but Map expected", data.getClass()));
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
