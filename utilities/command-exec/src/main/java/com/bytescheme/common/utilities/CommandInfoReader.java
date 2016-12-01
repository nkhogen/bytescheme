package com.bytescheme.common.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public class CommandInfoReader extends AbstractIterator<CommandInfo> implements Closeable {
	private final JsonReader jsonReader;
	private final Gson gson = new Gson();

	public CommandInfoReader(final InputStream inputStream) throws IOException {
		Preconditions.checkNotNull(inputStream);
		this.jsonReader = gson.newJsonReader(new BufferedReader(new InputStreamReader(inputStream)));
		this.jsonReader.setLenient(true);
	}

	@Override
	protected CommandInfo computeNext() {
		try {
			if (jsonReader.hasNext() && !jsonReader.peek().equals(JsonToken.END_DOCUMENT)) {
				return gson.fromJson(jsonReader, CommandInfo.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.endOfData();
		IOUtils.closeQuietly(jsonReader);
		return null;
	}

	public void close() throws IOException {
		this.jsonReader.close();
	}
}
