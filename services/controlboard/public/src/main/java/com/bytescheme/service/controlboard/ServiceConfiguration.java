package com.bytescheme.service.controlboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bytescheme.common.paths.PathProcessor;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.security.GoogleAuthenticationProvider;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.remoteobjects.RootImpl;

/**
 * Bean configuration.
 * 
 * @author Naorem Khogendro Singh
 *
 */
@Configuration
@EnableConfigurationProperties(ServiceProperties.class)
public class ServiceConfiguration {
  @Autowired
  private ServiceProperties serviceProperties;

  @Bean
  @Autowired
  public RemoteObjectServer remoteObjectServer(
      ConfigurationProvider configurationProvider) throws Exception {
    GoogleAuthenticationProvider googleAuthenticationProvider = new GoogleAuthenticationProvider(
        serviceProperties.getGoogleClientId(),
        configurationProvider.getAuthenticationDataProvider());
    PathProcessor pathProcessor = new PathProcessor(
        configurationProvider.getNodeProvider());
    SecurityProvider securityProvider = new SecurityProvider(googleAuthenticationProvider,
        pathProcessor);
    RemoteObjectServer server = new RemoteObjectServer(true, securityProvider);
    RootImpl root = new RootImpl(configurationProvider.getObjectEndpointsProvider());
    root.setEnableMock(serviceProperties.isEnableMock());
    server.register(root);
    return server;
  }

  @Bean
  public ConfigurationProvider configurationProvider() {
    return new DefaultConfigurationProvider(serviceProperties);
  }
}
