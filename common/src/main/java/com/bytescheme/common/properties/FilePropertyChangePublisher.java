package com.bytescheme.common.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.bytescheme.common.utils.JsonUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * File based property poller and change publisher.
 *
 * @author Naorem Khogendro Singh
 *
 * @param <V>
 */
public class FilePropertyChangePublisher<V> extends AbstractPropertyChangePublisher<V> {
  private final File file;
  private final Class<V> valueClazz;;

  public FilePropertyChangePublisher(String file, Class<V> valueClazz) {
    super();
    Preconditions.checkArgument(!Strings.isNullOrEmpty(file), "Invalid property file");
    Preconditions.checkNotNull(valueClazz, "Invalid value class");
    this.file = new File(file);
    Preconditions.checkArgument(
        this.file.exists() && this.file.isFile() && this.file.canRead(),
        "Invalid property file %s", file);
    this.valueClazz = valueClazz;
    super.init();
  }

  @Override
  protected Map<String, V> getProperties() throws FileNotFoundException, JsonIOException,
      JsonSyntaxException, UnsupportedEncodingException {
    return JsonUtils.mapFromJsonFile(file.getAbsolutePath(), valueClazz);
  }
}
