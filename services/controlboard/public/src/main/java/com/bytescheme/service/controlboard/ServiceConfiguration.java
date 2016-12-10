package com.bytescheme.service.controlboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bytescheme.common.paths.PathProcessor;
import com.bytescheme.common.properties.FilePropertyChangePublisher;
import com.bytescheme.common.properties.PropertyChangePublisher;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.security.AuthData;
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
  public RemoteObjectServer remoteObjectServer() throws Exception {
    PropertyChangePublisher<Object> authorizationPublisher = new FilePropertyChangePublisher<Object>(
        serviceProperties.getBaseDir() + serviceProperties.getAuthorizationJsonFile(),
        Object.class);
    PropertyChangePublisher<AuthData> authenticationPublisher = new FilePropertyChangePublisher<AuthData>(
        serviceProperties.getBaseDir() + serviceProperties.getAuthenticationJsonFile(),
        AuthData.class);
    GoogleAuthenticationProvider googleAuthenticationProvider = new GoogleAuthenticationProvider(
        authenticationPublisher.getCurrentProperties(),
        serviceProperties.getGoogleClientId());
    authenticationPublisher.registerListener(googleAuthenticationProvider);
    PathProcessor pathProcessor = new PathProcessor(
        authorizationPublisher.getCurrentProperties());
    authorizationPublisher.registerListener(pathProcessor);
    SecurityProvider securityProvider = new SecurityProvider(googleAuthenticationProvider,
        pathProcessor);
    RemoteObjectServer server = new RemoteObjectServer(true, securityProvider);
    RootImpl root = new RootImpl(
        serviceProperties.getBaseDir() + serviceProperties.getObjectsJsonFile(),
        serviceProperties.getBaseDir() + serviceProperties.getEndpointsJsonFile(),
        serviceProperties.getBaseDir() + serviceProperties.getSshKeysDir());
    root.setEnableMock(serviceProperties.isEnableMock());
    server.register(root);
    return server;
  }
}
