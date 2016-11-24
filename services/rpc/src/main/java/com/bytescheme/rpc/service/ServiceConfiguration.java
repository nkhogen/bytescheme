package com.bytescheme.rpc.service;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.core.TestClient;
import com.bytescheme.rpc.security.FileAuthenticationProvider;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.rpc.utils.PathProcessor;

@Configuration
public class ServiceConfiguration {
	@Bean
	public RemoteObjectServer remoteObjectServer() throws IOException {
		SecurityProvider securityProvider = new SecurityProvider(
				new FileAuthenticationProvider("src/main/resources/authentication.json"),
				new PathProcessor("src/main/resources/authorization.json"));
		RemoteObjectServer server = new RemoteObjectServer(securityProvider, true);
		server.register(new TestClient.HelloProcessImpl());
		return server;
	}
}
