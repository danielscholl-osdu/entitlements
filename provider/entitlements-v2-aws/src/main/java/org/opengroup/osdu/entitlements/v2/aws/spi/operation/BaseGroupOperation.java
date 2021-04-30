// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.entitlements.v2.aws.spi.operation;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

@SuperBuilder
public abstract class BaseGroupOperation implements Operation {

    protected RedisConnector redisConnector;
    protected AwsAppProperties config;
    protected JaxRsDpsLog log;
    protected EntityNode groupNode;

    protected void createGroupTransaction(EntityNode groupNode) {
        RedisConnectionPool connectionPool = redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            createGroup(commands, groupNode);
            updateAppIds(commands, groupNode);
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }


    /*Noticed that multi is not used here. Group entity, parent reference and child are not deleted in a transaction
    since each of these exist in different databases
    Potential data integrity issues could arise here.
     */
    protected void deleteGroupTransaction(EntityNode groupNode) {
        RedisConnectionPool connectionPool = redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionEntityNode());
            commands.del(groupNode.getNodeId());
            commands.select(config.getPartitionParentRef());
            commands.del(groupNode.getNodeId());
            commands.select(config.getPartitionChildrenRef());
            commands.del(groupNode.getNodeId());
            commands.select(config.getPartitionAppId());
            final Set<String> appIds = getAppIdsForUpdate(groupNode.getAppIds());
            for (String appId : appIds) {
                commands.srem(appId, groupNode.getNodeId());
            }
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    private void createGroup(RedisCommands<String, String> commands, EntityNode group) {
        commands.select(config.getPartitionEntityNode());
        if (Boolean.FALSE.equals(commands.setnx(group.getNodeId(), JsonConverter.toJson(group)))) {
            //throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("This group already exists. " + group.getNodeId()));
            //Integration test checks for this specific error text
            throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), String.format("This group already exists"));
        }
    }

    /**
     * Update redis <app-id, group-email>
     */
    private void updateAppIds(RedisCommands<String, String> commands, EntityNode group) {
        final Set<String> appIds = getAppIdsForUpdate(group.getAppIds());
        commands.select(config.getPartitionAppId());
        for (String appId : appIds) {
            commands.sadd(appId, group.getNodeId());
        }
    }

    private Set<String> getAppIdsForUpdate(final Set<String> appIdsOfGroup) {
        final Set<String> appIds = new HashSet<>();
        if (appIdsOfGroup.isEmpty()) {
            appIds.add(AwsAppProperties.DEFAULT_APPID_KEY);
        } else {
            appIds.addAll(appIdsOfGroup);
        }
        return appIds;
    }
}
