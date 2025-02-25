/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.cache;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import io.lettuce.core.ClientOptions;
import org.opengroup.osdu.core.cache.RedisCacheBuilder;
import org.opengroup.osdu.core.cache.RedisCodecFactory;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.cache.enums.CachingStrategy;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntConfigProperties;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  private final ClientOptions clientOptions =  ClientOptions.builder()
      .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
      .build();

  @Bean
  public IRedisCache<String, UserInfo> userInfoIRedisCache(
      EntConfigProperties properties,
      RedisCodecFactory<String, UserInfo> redisCodecFactory
  ) {
    RedisCacheBuilder<String, UserInfo> userInfoRedisCacheBuilder = new RedisCacheBuilder<>(
        redisCodecFactory);
    return userInfoRedisCacheBuilder.buildRedisCache(
        properties.getRedisUserInfoHost(),
        properties.getRedisUserInfoPort(),
        properties.getRedisUserInfoPassword(),
        properties.getRedisUserInfoExpiration(),
        properties.getRedisUserInfoWithSsl(),
        clientOptions,
        String.class,
        UserInfo.class
    );
  }

  @Bean
  public ICache<String, ParentReferences> userGroupsCache(
      EntConfigProperties properties,
      RedisCodecFactory<String, ParentReferences> redisCodecFactory
  ) {
    RedisCacheBuilder<String, ParentReferences> userInfoRedisCacheBuilder = new RedisCacheBuilder<>(
        redisCodecFactory);
    return userInfoRedisCacheBuilder.buildRedisCache(
        properties.getRedisUserGroupsHost(),
        properties.getRedisUserGroupsPort(),
        properties.getRedisUserGroupsPassword(),
        properties.getRedisUserGroupsExpiration(),
        properties.getRedisUserGroupsWithSsl(),
        clientOptions,
        String.class,
        ParentReferences.class
    );
  }

  @Bean
  public ICache<String, PartitionInfo> partitionInfoCache(
      EntConfigProperties properties) {
    return new VmCache<>(properties.getPartitionInfoVmCacheExpTime(),
        properties.getPartitionInfoVmCacheSize(), CachingStrategy.EXPIRE_AFTER_WRITE);
  }
}