package com.bytescheme.service.controlboard.domains;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.bytescheme.common.paths.Node;
import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.security.AuthData;
import com.bytescheme.service.controlboard.ConfigurationProvider;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Dynamo DB configuration provider.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class DynamoDBConfigurationProvider implements ConfigurationProvider {
  private static final Logger LOG = LoggerFactory.getLogger(DynamoDBConfigurationProvider.class);

  @Autowired
  private DynamoDBMapper dbMapper;

  public DynamoDBMapper getDbMapper() {
    return dbMapper;
  }

  public void setDbMapper(DynamoDBMapper dbMapper) {
    this.dbMapper = dbMapper;
  }

  @Override
  public Function<String, AuthData> getAuthenticationDataProvider() {
    return user -> {
      LOG.info("Retrieving auth data for user: {}", user);
      UserRoles entity = dbMapper.load(UserRoles.class, user);
      if (entity == null) {
        return null;
      }
      // TODO decryption. Password is dummy for now because Google manages it.
      AuthData authData = new AuthData();
      authData.setPassword(entity.getPassword());
      Set<String> roles = Sets.newHashSet();
      for (String token : entity.getRoles().split(",")) {
        String role = token.trim();
        if (!Strings.isNullOrEmpty(role)) {
          roles.add(role);
        }
      }
      authData.setRoles(roles);
      return authData;
    };
  }

  @Override
  public Function<UUID, Node<String>> getNodeProvider() {
    return objectId -> {
      LOG.info("Retrieving object roles for object ID: {}", objectId);
      ObjectRoles hashKey = new ObjectRoles();
      hashKey.setObjectId(objectId);
      DynamoDBQueryExpression<ObjectRoles> queryExpression = new DynamoDBQueryExpression<ObjectRoles>()
          .withHashKeyValues(hashKey);
      QueryResultPage<ObjectRoles> result = dbMapper.queryPage(ObjectRoles.class, queryExpression);
      List<ObjectRoles> objectRoles = result.getResults();
      if (CollectionUtils.isEmpty(objectRoles)) {
        return null;
      }
      Map<String, Node<String>> map = Maps.newHashMap();
      objectRoles.forEach(obj -> {
        map.put(obj.getMethod(), Node.withValue(obj.getRoles()));
      });
      return Node.withMap(map);
    };
  }

  @Override
  public Function<String, Set<ObjectEndpoint>> getObjectEndpointsProvider() {
    return user -> {
      LOG.info("Retrieving endpoints for user: {}", user);
      UserObjects hashKey = new UserObjects();
      hashKey.setUser(user);
      DynamoDBQueryExpression<UserObjects> queryExpression = new DynamoDBQueryExpression<UserObjects>()
          .withHashKeyValues(hashKey);
      QueryResultPage<UserObjects> result = dbMapper.queryPage(UserObjects.class, queryExpression);
      List<UserObjects> userObjects = result.getResults();
      ImmutableSet.Builder<ObjectEndpoint> objectEndpointsBuilder = ImmutableSet.builder();
      if (CollectionUtils.isEmpty(userObjects)) {
        return objectEndpointsBuilder.build();
      }
      userObjects.forEach(obj -> {
        Endpoints endpoints = dbMapper.load(Endpoints.class, obj.getObjectId());
        objectEndpointsBuilder.add(
            new ObjectEndpoint(
                endpoints.getObjectId(),
                endpoints.getEndpoint(),
                CryptoUtils.getPublicKey(endpoints.getSshKey().trim().getBytes())));
      });
      return objectEndpointsBuilder.build();
    };
  }
}
