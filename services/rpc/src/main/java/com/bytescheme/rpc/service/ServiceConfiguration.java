package com.bytescheme.rpc.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bytescheme.rpc.core.ServerRequestProcessor;
import com.bytescheme.rpc.core.TestClient;

@Configuration
public class ServiceConfiguration {
	@Bean
	public ServerRequestProcessor serverRequestProcessor() {
		ServerRequestProcessor processor = new ServerRequestProcessor();
		processor.register(new TestClient.HelloProcessImpl());
		return processor;
	}
}
