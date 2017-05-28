package com.bytescheme.common.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

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
public class CommandInfoReader extends AbstractIterator<CommandInfo>
    implements AutoCloseable {
  private final JsonReader jsonReader;
  private final Gson gson = new Gson();
  private final Function<CommandInfo, CommandInfo> callback;

  public CommandInfoReader(InputStream inputStream) throws IOException {
    this(inputStream, null);
  }

  public CommandInfoReader(InputStream inputStream,
      Function<CommandInfo, CommandInfo> callback) throws IOException {
    Preconditions.checkNotNull(inputStream);
    this.jsonReader = gson
        .newJsonReader(new BufferedReader(new InputStreamReader(inputStream)));
    this.jsonReader.setLenient(true);
    this.callback = callback;
  }

  @Override
  protected CommandInfo computeNext() {
    try {
      if (jsonReader.hasNext() && !jsonReader.peek().equals(JsonToken.END_DOCUMENT)) {
        CommandInfo commandInfo = gson.fromJson(jsonReader, CommandInfo.class);
        return callback == null ? commandInfo : callback.apply(commandInfo);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed parsing JSON file", e);
    }
    super.endOfData();
    IOUtils.closeQuietly(jsonReader);
    return null;
  }

  public void close() throws IOException {
    this.jsonReader.close();
  }
}
