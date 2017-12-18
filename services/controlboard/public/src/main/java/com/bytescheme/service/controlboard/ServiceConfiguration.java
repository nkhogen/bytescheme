package com.bytescheme.service.controlboard;

import java.io.IOException;
import java.net.MalformedURLException;
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
import com.bytescheme.service.controlboard.common.remoteobjects.Root;
import com.bytescheme.service.controlboard.domains.DynamoDBConfigurationProvider;
import com.bytescheme.service.controlboard.remoteobjects.DeviceEventConsumer;
import com.bytescheme.service.controlboard.remoteobjects.DeviceEventSchedulerImpl;
import com.bytescheme.service.controlboard.remoteobjects.RootImpl;
import com.bytescheme.service.eventscheduler.Scheduler;
import com.bytescheme.service.eventscheduler.domains.SchedulerDao;

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
      ConfigurationProvider configurationProvider)
      throws IOException, GeneralSecurityException {
    return new GoogleAuthenticationProvider(
        serviceProperties.getGoogleClientId(),
        configurationProvider.getAuthenticationDataProvider());
  }

  @Autowired
  @Bean
  public AWSAuthenticationProvider awsAuthenticationProvider(
      ConfigurationProvider configurationProvider) {
    return new AWSAuthenticationProvider(
        configurationProvider.getAuthenticationDataProvider());
  }

  @Autowired
  @Bean
  public SecurityProvider securityProvider(ConfigurationProvider configurationProvider,
      GoogleAuthenticationProvider googleAuthenticationProvider,
      AWSAuthenticationProvider awsAuthenticationProvider) {
    return new SecurityProvider(
        new PathProcessor(configurationProvider.getNodeProvider()),
        googleAuthenticationProvider,
        awsAuthenticationProvider);
  }

  @Autowired
  @Bean
  public Root root(ConfigurationProvider configurationProvider,
      RemoteObjectServer remoteObjectServer) {
    RootImpl root = new RootImpl(configurationProvider.getObjectEndpointsProvider());
    root.setEnableMock(serviceProperties.isEnableMock());
    remoteObjectServer.register(root);
    return root;
  }

  @Autowired
  @Bean
  public RemoteObjectServer remoteObjectServer(SecurityProvider securityProvider)
      throws Exception {
    return new RemoteObjectServer(true, securityProvider);
  }

  @Bean
  public ConfigurationProvider configurationProvider() {
    return serviceProperties.isEnableFileConfig()
        ? new DefaultConfigurationProvider(serviceProperties)
        : new DynamoDBConfigurationProvider(
            new DynamoDBMapper(AmazonDynamoDBClientBuilder.defaultClient()));
  }

  @Bean
  public SchedulerDao schedulerDao() {
    DynamoDBMapper dynamodbMapper = new DynamoDBMapper(
        AmazonDynamoDBClientBuilder.defaultClient());
    return new SchedulerDao(dynamodbMapper);
  }

  @Autowired
  @Bean
  public Scheduler scheduler(RootImpl root, SchedulerDao schedulerDao)
      throws MalformedURLException {
    Scheduler scheduler = new Scheduler(
        serviceProperties.getSchedulerId(),
        schedulerDao,
        new DeviceEventConsumer(root));
    root.setDeviceEventScheduler(new DeviceEventSchedulerImpl(scheduler));
    return scheduler;
  }
}
