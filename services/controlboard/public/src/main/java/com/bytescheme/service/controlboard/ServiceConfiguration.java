package com.bytescheme.service.controlboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bytescheme.common.paths.DefaultNodeProvider;
import com.bytescheme.common.paths.PathProcessor;
import com.bytescheme.common.properties.FilePropertyChangePublisher;
import com.bytescheme.common.properties.PropertyChangePublisher;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.rpc.security.DefaultAuthenticationDataProvider;
import com.bytescheme.rpc.security.GoogleAuthenticationProvider;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.remoteobjects.DefaultObjectEndpointsProvider;
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
    DefaultAuthenticationDataProvider authDataProvider = new DefaultAuthenticationDataProvider();
    authenticationPublisher.registerListener(authDataProvider);
    GoogleAuthenticationProvider googleAuthenticationProvider = new GoogleAuthenticationProvider(
        serviceProperties.getGoogleClientId(), authDataProvider);
    DefaultNodeProvider nodeProvider = new DefaultNodeProvider();
    authorizationPublisher.registerListener(nodeProvider);
    PathProcessor pathProcessor = new PathProcessor(nodeProvider);
    SecurityProvider securityProvider = new SecurityProvider(googleAuthenticationProvider,
        pathProcessor);
    RemoteObjectServer server = new RemoteObjectServer(true, securityProvider);
    DefaultObjectEndpointsProvider objectEndpointsProvider = new DefaultObjectEndpointsProvider(
        serviceProperties.getBaseDir() + serviceProperties.getSshKeysDir());
    PropertyChangePublisher<String> objectsChangePublisher = new FilePropertyChangePublisher<String>(
        serviceProperties.getBaseDir() + serviceProperties.getObjectsJsonFile(),
        String.class);
    objectsChangePublisher.registerListener((updated, all) -> {
      objectEndpointsProvider.updateObjectIds(all);
    });
    PropertyChangePublisher<String> endpointsChangePublisher = new FilePropertyChangePublisher<String>(
        serviceProperties.getBaseDir() + serviceProperties.getEndpointsJsonFile(),
        String.class);
    endpointsChangePublisher.registerListener((updated, all) -> {
      objectEndpointsProvider.updateEndpoints(all);
    });
    RootImpl root = new RootImpl(objectEndpointsProvider);
    root.setEnableMock(serviceProperties.isEnableMock());
    server.register(root);
    return server;
  }
}
