package com.bytescheme.rpc.service;

import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceContainer {
  @Bean
  public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
    return new JettyEmbeddedServletContainerFactory(8080);
  }
}
