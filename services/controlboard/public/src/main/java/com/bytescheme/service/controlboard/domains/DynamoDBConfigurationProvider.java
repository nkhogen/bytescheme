package com.bytescheme.service.controlboard.domains;

import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.bytescheme.common.paths.Node;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.service.controlboard.remoteobjects.ObjectEndpoint;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;

/**
 * Dynamo DB configuration provider.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DynamoDBConfigurationProvider implements ConfigurationProvider {
  private final DynamoDBMapper dbMapper;

  public DynamoDBConfigurationProvider(DynamoDBMapper dbMapper) {
    this.dbMapper = Preconditions.checkNotNull(dbMapper, "Invalid Dynamo DB mapper");
  }

  @Override
  public Function<String, AuthData> getAuthenticationDataProvider() {
    return user -> {
      UserRoles entity = dbMapper.load(UserRoles.class, user);
      if (entity == null) {
        return null;
      }
      // TODO encryption
      AuthData authData = new AuthData();
      authData.setPassword(entity.getPassword());
      authData.setRoles(entity.getRoles());
      return authData;
    };
  }

  @Override
  public Function<String, Node<String>> getNodeProvider() {
    return objectId -> {
      DynamoDBQueryExpression<ObjectRoles> queryExpression = new DynamoDBQueryExpression<ObjectRoles>();
      // dbMapper.query(clazz, queryExpression)
      // TODO
      return null;
    };
  }

  @Override
  public Function<String, Set<ObjectEndpoint>> getObjectEndpointsProvider() {
    return user -> {
      // TODO
      return null;
    };
  }
}
