package com.bytescheme.service.controlboard;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.bytescheme.common.sockets.SimpleEventServer;
import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.rpc.security.RSAAuthenticationProvider;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.remoteobjects.TargetControlBoardImpl;
import com.bytescheme.service.controlboard.remoteobjects.TargetMockControlBoardImpl;
import com.bytescheme.service.controlboard.video.VideoBroadcastHandler;
import com.bytescheme.service.controlboard.video.VideoServer;

/**
 * Bean configurations.
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
  public RSAAuthenticationProvider rsaAuthenticationProvider() throws IOException {
    Map<String, AuthData> authDataMap = JsonUtils.mapFromJsonFile(
        serviceProperties.getBaseDir() + serviceProperties.getAuthenticationJsonFile(),
        AuthData.class);
    return new RSAAuthenticationProvider(
        serviceProperties.getBaseDir() + serviceProperties.getSshKeysDir(),
        key -> authDataMap.get(key));
  }

  @Autowired
  @Bean
  public SecurityProvider securityProvider(RSAAuthenticationProvider rsaAuthenticationProvider) {
    return new SecurityProvider(rsaAuthenticationProvider);
  }

  @Autowired
  @Bean
  public RemoteObjectServer remoteObjectServer(SimpleEventServer simpleEventServer,
      SecurityProvider securityProvider) throws Exception {
    RemoteObjectServer server = new RemoteObjectServer(true, securityProvider);
    if (serviceProperties.isEnableMock()) {
      server.register(new TargetMockControlBoardImpl(serviceProperties.getObjectId(),
          serviceProperties.getVideoUrlFormat()));
    } else {
      server.register(new TargetControlBoardImpl(serviceProperties.getObjectId(),
          serviceProperties.getDevices(), serviceProperties.getVideoUrlFormat(),
          simpleEventServer));
    }
    return server;
  }

  @Bean
  public VideoBroadcastHandler broadcaster() {
    VideoBroadcastHandler broadcastHandler = VideoBroadcastHandler.getInstance();
    broadcastHandler
        .setCommandFile(serviceProperties.getBaseDir() + serviceProperties.getCommandFile());
    return broadcastHandler;
  }

  @Bean
  public ServerEndpointExporter serverEndpointExporter() {
    ServerEndpointExporter endpointExporter = new ServerEndpointExporter();
    endpointExporter.setAnnotatedEndpointClasses(VideoServer.class);
    return endpointExporter;
  }

  @Bean
  public SimpleEventServer simpleEventServer() throws IOException {
    SimpleEventServer simpleEventServer = new SimpleEventServer(
        serviceProperties.getEventServerPort());
    simpleEventServer.start();
    return simpleEventServer;
  }
}
