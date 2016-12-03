package com.bytescheme.rpc.core.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.bytescheme.rpc.core.MessageCodec;
import com.bytescheme.rpc.core.MethodCallResponse;

public class ExceptionTest {

  @Test
  public void test() throws IOException {
    Path path = new File("src/test/resources/exception.json").toPath();
    String json = new String(Files.readAllBytes(path));
    MessageCodec codec = new MessageCodec(null, null);
    MethodCallResponse response = codec.getObject(json, MethodCallResponse.class);
    response.getException();
  }
}
