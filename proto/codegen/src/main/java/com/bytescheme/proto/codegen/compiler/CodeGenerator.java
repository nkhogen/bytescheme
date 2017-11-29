package com.bytescheme.proto.codegen.compiler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class CodeGenerator {
  // Velocity or Free Marker supports variable checks and are better.
  private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();
  private Mustache mustache;

  public CodeGenerator() {
    try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/sample.mustache"))) {
      this.mustache = MUSTACHE_FACTORY.compile(reader, "template");
    } catch (IOException e) {
     throw new RuntimeException(e);
    }
  }

  public void generate(ClassDefinition classDefinition, OutputStream outputStream) throws IOException {
    try (Writer writer = new OutputStreamWriter(outputStream)) {
      mustache.execute(writer, classDefinition);
      writer.flush();
    }
  }
}
