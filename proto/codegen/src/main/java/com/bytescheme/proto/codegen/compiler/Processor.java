package com.bytescheme.proto.codegen.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import com.bytescheme.proto.codegen.compiler.ClassDefinition.MethodDefinition;
import com.squareup.javawriter.JavaWriter;

@SuppressWarnings("unused")
@SupportedAnnotationTypes({ "com.bytescheme.proto.codegen.compiler.annotations.*" })
public class Processor extends AbstractProcessor {
  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private CodeGenerator codeGenerator;
  private Map<String, ClassDefinition> classDefinitions;

  public Processor() {
    super();
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    codeGenerator = new CodeGenerator();
    classDefinitions = new HashMap<>();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      try {
        for (Map.Entry<String, ClassDefinition> entry : classDefinitions.entrySet()) {
          // Sending the generated source to compile
          JavaFileObject jfo = filer.createSourceFile(entry.getKey() + "Impl");
          try (OutputStream outputStream = jfo.openOutputStream()) {
            codeGenerator.generate(entry.getValue(), outputStream);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return false;
    }
    for (TypeElement annotation : annotations) {
      Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
      for (Element element : elements) {
        PackageElement packageElement = elementUtils.getPackageOf(element);
        String className = element.toString();
        String packageName = packageElement.getQualifiedName().toString();
        ClassDefinition classDefinition = classDefinitions.get(className);
        if (classDefinition == null) {
          classDefinition = new ClassDefinition(packageName, element.getSimpleName().toString());
          classDefinitions.put(className, classDefinition);
        }
        try {
          Class<?> clazz = Class.forName(className);
          if (clazz.isInterface()) {
            for (Method method : clazz.getMethods()) {
              Class<?>[] parameterTypes = method.getParameterTypes();
              StringBuilder sb = new StringBuilder();
              int index = 0;
              for (Class<?> parameterType : parameterTypes) {
                sb.append(parameterType.getName());
                sb.append(" param").append(index);
                if (index < parameterTypes.length - 1) {
                  sb.append(", ");
                }
                index++;
              }
              MethodDefinition methodDefinition = new MethodDefinition(method.getName(),
                  sb.toString(), method.getReturnType().getName());
              classDefinition.methodDefinitions.add(methodDefinition);
            }
          }
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return true;
  }
}
