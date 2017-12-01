package com.bytescheme.service.controlboard;

import java.util.Set;

import com.bytescheme.common.paths.DefaultNodeProvider;
import com.bytescheme.common.paths.Node;
import com.bytescheme.common.properties.FilePropertyChangePublisher;
import com.bytescheme.common.properties.PropertyChangePublisher;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.rpc.security.DefaultAuthenticationDataProvider;
import com.bytescheme.service.controlboard.domains.ObjectEndpoint;
import com.bytescheme.service.controlboard.remoteobjects.DefaultObjectEndpointsProvider;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;

/**
 * Default file based configuration provider.
 * @author Naorem Khogendro Singh
 *
 */
public class DefaultConfigurationProvider implements ConfigurationProvider {

  private final DefaultAuthenticationDataProvider authDataProvider;
  private final PropertyChangePublisher<AuthData> authenticationPublisher;

  private final DefaultNodeProvider nodeProvider;
  private final PropertyChangePublisher<Object> authorizationPublisher;

  private final DefaultObjectEndpointsProvider objectEndpointsProvider;
  private final PropertyChangePublisher<String> objectsChangePublisher;
  private final PropertyChangePublisher<String> endpointsChangePublisher;

  public DefaultConfigurationProvider(ServiceProperties serviceProperties) {
    Preconditions.checkNotNull(serviceProperties, "Invalid service properties");
    this.authenticationPublisher = new FilePropertyChangePublisher<AuthData>(
        serviceProperties.getBaseDir() + serviceProperties.getAuthenticationJsonFile(),
        AuthData.class);
    this.authDataProvider = new DefaultAuthenticationDataProvider();
    this.authenticationPublisher.registerListener(authDataProvider);

    this.authorizationPublisher = new FilePropertyChangePublisher<Object>(
        serviceProperties.getBaseDir() + serviceProperties.getAuthorizationJsonFile(),
        Object.class);
    this.nodeProvider = new DefaultNodeProvider();
    this.authorizationPublisher.registerListener(nodeProvider);

    this.objectEndpointsProvider = new DefaultObjectEndpointsProvider(
        serviceProperties.getBaseDir() + serviceProperties.getSshKeysDir());
    this.objectsChangePublisher = new FilePropertyChangePublisher<String>(
        serviceProperties.getBaseDir() + serviceProperties.getObjectsJsonFile(),
        String.class);
    this.endpointsChangePublisher = new FilePropertyChangePublisher<String>(
        serviceProperties.getBaseDir() + serviceProperties.getEndpointsJsonFile(),
        String.class);
    this.objectsChangePublisher.registerListener((updated, all) -> {
      this.objectEndpointsProvider.updateObjectIds(all);
    });
    this.endpointsChangePublisher.registerListener((updated, all) -> {
      this.objectEndpointsProvider.updateEndpoints(all);
    });
  }

  @Override
  public Function<String, AuthData> getAuthenticationDataProvider() {
    return authDataProvider;
  }

  @Override
  public Function<String, Node<String>> getNodeProvider() {
    return nodeProvider;
  }

  @Override
  public Function<String, Set<ObjectEndpoint>> getObjectEndpointsProvider() {
    return objectEndpointsProvider;
  }

}
