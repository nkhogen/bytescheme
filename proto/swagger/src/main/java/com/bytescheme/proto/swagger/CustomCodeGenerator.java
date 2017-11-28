package com.bytescheme.proto.swagger;

import java.util.List;
import java.util.Map;

import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.languages.SpringCodegen;
import io.swagger.models.Swagger;

public class CustomCodeGenerator extends SpringCodegen {

  public CustomCodeGenerator() {
    super();
  }

  @Override
  public String getName() {
    return "spring-custom";
  }

  @Override
  public void preprocessSwagger(Swagger swagger) {
    super.preprocessSwagger(swagger);
  }

  /**
   * Process model classes.
   */
  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> postProcessModels(Map<String, Object> map) {
    Map<String, Object> result = super.postProcessModels(map);
    List<Object> models = (List<Object>) result.get("models");
    for (Object model : models) {
      Map<String, Object> mo = (Map<String, Object>) model;
      CodegenModel codegenModel = (CodegenModel) mo.get("model");
      System.out.println(codegenModel);
    }
    System.out.println(result);
    return result;
  }

  /**
   * Process controllers.
   */
  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> postProcessOperations(Map<String, Object> map) {
    Map<String, Object> result = super.postProcessOperations(map);
    Map<String, Object> operations = (Map<String, Object>) result.get("operations");
    String classname = (String) operations.get("classname");
    System.out.println(classname);
    List<CodegenOperation> codegenOperations = (List<CodegenOperation>) operations.get("operation");
    for (CodegenOperation codegenOperation : codegenOperations) {
      System.out.println(codegenOperation);
    }
    return map;
  }
}
