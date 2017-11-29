package com.bytescheme.proto.codegen.compiler;

import java.util.ArrayList;
import java.util.List;

// Just a sample definition
public class ClassDefinition {
  static class MethodDefinition {
    String methodName;
    String parameters;
    String returnType;
    boolean isReturn;

    public MethodDefinition(String methodName, String parameters, String returnType) {
      this.methodName = methodName;
      this.parameters = parameters;
      this.returnType = returnType;
      this.isReturn = !returnType.equals("void");

    }
  }

  String className;
  String packageName;
  List<MethodDefinition> methodDefinitions = new ArrayList<>();

  public ClassDefinition(String packageName, String className) {
    this.packageName = packageName;
    this.className = className;
  }
}
