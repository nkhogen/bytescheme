package com.bytescheme.service.controlboard;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.bytescheme.common.paths.PathProcessor;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.security.AWSAuthenticationProvider;
import com.bytescheme.rpc.security.GoogleAuthenticationProvider;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.domains.DynamoDBConfigurationProvider;
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

  @Autowired
  @Bean
  public GoogleAuthenticationProvider googleAuthenticationProvider(
      ConfigurationProvider configurationProvider) throws IOException, GeneralSecurityException {
    return new GoogleAuthenticationProvider(serviceProperties.getGoogleClientId(),
        configurationProvider.getAuthenticationDataProvider());
  }

  @Autowired
  @Bean
  public AWSAuthenticationProvider awsAuthenticationProvider(
      ConfigurationProvider configurationProvider) {
    return new AWSAuthenticationProvider(configurationProvider.getAuthenticationDataProvider());
  }

  @Autowired
  @Bean
  public SecurityProvider securityProvider(ConfigurationProvider configurationProvider,
      GoogleAuthenticationProvider googleAuthenticationProvider,
      AWSAuthenticationProvider awsAuthenticationProvider) {
    return new SecurityProvider(new PathProcessor(configurationProvider.getNodeProvider()),
        googleAuthenticationProvider, awsAuthenticationProvider);
  }

  @Autowired
  @Bean
  public RemoteObjectServer remoteObjectServer(ConfigurationProvider configurationProvider,
      SecurityProvider securityProvider) throws Exception {
    RemoteObjectServer server = new RemoteObjectServer(true, securityProvider);
    RootImpl root = new RootImpl(configurationProvider.getObjectEndpointsProvider());
    root.setEnableMock(serviceProperties.isEnableMock());
    server.register(root);
    return server;
  }

  @Bean
  public ConfigurationProvider configurationProvider() {
    return serviceProperties.isEnableFileConfig()
        ? new DefaultConfigurationProvider(serviceProperties)
        : new DynamoDBConfigurationProvider(
            new DynamoDBMapper(AmazonDynamoDBClientBuilder.defaultClient()));
  }
}
