package com.bytescheme.service.controlboard;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.bytescheme.common.utils.JsonUtils;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.rpc.security.RSAAuthenticationProvider;
import com.bytescheme.rpc.security.SecurityProvider;
import com.bytescheme.service.controlboard.common.remoteobjects.MockControlBoardImpl;
import com.bytescheme.service.controlboard.remoteobjects.TargetControlBoardImpl;
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
  public RemoteObjectServer remoteObjectServer() throws Exception {
    Map<String, AuthData> authDataMap = JsonUtils.mapFromJsonFile(
        serviceProperties.getBaseDir() + serviceProperties.getAuthenticationJsonFile(),
        AuthData.class);
    RSAAuthenticationProvider rsaAuthenticationProvider = new RSAAuthenticationProvider(
        serviceProperties.getBaseDir() + serviceProperties.getSshKeysDir(), authDataMap);
    SecurityProvider securityProvider = new SecurityProvider(rsaAuthenticationProvider);
    RemoteObjectServer server = new RemoteObjectServer(true, securityProvider);
    Map<Integer, String> map = new HashMap<>();
    for (Map.Entry<String, String> entry : serviceProperties.getDevices().entrySet()) {
      map.put(Integer.parseInt(entry.getKey()), entry.getValue());
    }
    if (serviceProperties.isEnableMock()) {
      server.register(new MockControlBoardImpl(serviceProperties.getObjectId()) {
        private static final long serialVersionUID = 1L;

        @Override
        public String getVideoUrl() {
          String secret = VideoBroadcastHandler.getInstance().generateSecret();
          return String.format(serviceProperties.getVideoUrlFormat(), secret);
        }
      });
    } else {
      server.register(new TargetControlBoardImpl(serviceProperties.getObjectId(), map,
          serviceProperties.getVideoUrlFormat()));
    }
    return server;
  }

  @Bean
  public VideoBroadcastHandler broadcaster() {
    return VideoBroadcastHandler.getInstance();
  }

  @Bean
  public ServerEndpointExporter serverEndpointExporter() {
    ServerEndpointExporter endpointExporter = new ServerEndpointExporter();
    endpointExporter.setAnnotatedEndpointClasses(VideoServer.class);
    return endpointExporter;
  }
}
