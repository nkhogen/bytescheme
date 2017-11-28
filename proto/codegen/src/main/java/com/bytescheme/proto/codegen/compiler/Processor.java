package com.bytescheme.proto.codegen.compiler;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({"com.bytescheme.proto.codegen.compiler.Model"})
public class Processor extends AbstractProcessor {

  public Processor() {
    super();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    System.out.println("@@@@@@@ " + annotations + roundEnv);
    return false;
  }

}
