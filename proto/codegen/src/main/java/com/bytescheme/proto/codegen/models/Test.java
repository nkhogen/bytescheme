package com.bytescheme.proto.codegen.models;

import com.bytescheme.proto.codegen.compiler.annotations.Model;

@Model
public interface Test extends Test1 {
  String hello(String s, int i);
}
