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

package org.opengroup.osdu.entitlements.v2.aws.spi.creategroup;

import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.di.WhitelistSvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.AddMemberChildUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.AddMemberParentUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.AddUserPartitionAssociationOperationImpl;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.CreateGroupOperationImpl;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AwsCreateGroupRepo extends BaseRepo implements CreateGroupRepo {

    private final RedisConnector redisConnector;
    private final AwsAppProperties config;
    private final JaxRsDpsLog log;
    private final AuditLogger auditLogger;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final Retry retry;
    private final WhitelistSvcAccBeanConfiguration whitelistSvcAccBeanConfiguration;

    @Override
    public Set<String> createGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        log.info(String.format("Creating group %s and updating data model in redis", groupNode.getName()));
        Set<String> impactedUsers;
        try {
            impactedUsers = createGroup(executedCommands, groupNode, createGroupRepoDto);
        } catch (AppException ex) {
            rollback(ex);
            auditLogger.createGroup(AuditStatus.FAILURE, groupNode.getNodeId());
            throw ex;
        } finally {
            executedCommands.clear();
        }
        auditLogger.createGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
        return (impactedUsers == null) ? Collections.emptySet() : impactedUsers;
    }

    @Override
    public Set<String> createGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        List<String> impactedUsers = new ArrayList<>();
        impactedUsers.add(createGroupRepoDto.getRequesterNode().getNodeId());
        executedCommandsDeque.push(executeCreateGroupNode(groupNode));
        executedCommandsDeque.push(executeUpdateParent(groupNode, createGroupRepoDto.getRequesterNode(), Role.OWNER));
        executedCommandsDeque.push(executeUpdateChildren(groupNode, createGroupRepoDto.getRequesterNode()));
        executeUserPartitionAssociationUpdate(groupNode, createGroupRepoDto.getRequesterNode(), createGroupRepoDto.getPartitionId())
                .ifPresent(executedCommandsDeque::push);
        if (createGroupRepoDto.isAddDataRootGroup()) {
            executedCommandsDeque.push(executeUpdateParent(groupNode, createGroupRepoDto.getDataRootGroupNode(), Role.MEMBER));
            executedCommandsDeque.push(executeUpdateChildren(groupNode, createGroupRepoDto.getDataRootGroupNode()));
            impactedUsers.addAll(retrieveGroupRepo.loadAllChildrenUsers(createGroupRepoDto.getDataRootGroupNode()).getChildrenUserIds());
        }
        return new HashSet<>(impactedUsers);
    }

    private Operation executeCreateGroupNode(EntityNode groupNode) {
        Operation createGroupOperation = CreateGroupOperationImpl.builder().redisConnector(redisConnector)
                .log(log).config(config).groupNode(groupNode).build();
        createGroupOperation.execute();
        return createGroupOperation;
    }

    private Operation executeUpdateParent(EntityNode groupNode, EntityNode memberNode, Role role) {
        Operation updateParentOperation = AddMemberParentUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(groupNode).childrenReference(ChildrenReference.createChildrenReference(memberNode, role)).build();
        updateParentOperation.execute();
        return updateParentOperation;
    }

    private Operation executeUpdateChildren(EntityNode groupNode, EntityNode memberNode) {
        Operation updateChildrenOperation = AddMemberChildUpdateOperationImpl.builder().redisConnector(redisConnector).retry(retry)
                .log(log).config(config).groupNode(groupNode).memberId(memberNode.getNodeId()).memberPartitionId(memberNode.getDataPartitionId()).build();
        updateChildrenOperation.execute();
        return updateChildrenOperation;
    }

    private Optional<Operation> executeUserPartitionAssociationUpdate(EntityNode groupEntityNode, EntityNode memberNode, String partitionId) {
        if (groupEntityNode.isRootUsersGroup() && !memberNode.isGroup()) {
            Operation addUserPartitionAssociationOperation = AddUserPartitionAssociationOperationImpl.builder().redisConnector(redisConnector)
                    .retry(retry).config(config).log(log).userId(memberNode.getNodeId()).partitionId(partitionId)
                    .whitelistSvcAccBeanConfiguration(whitelistSvcAccBeanConfiguration).build();
            addUserPartitionAssociationOperation.execute();
            return Optional.of(addUserPartitionAssociationOperation);
        }
        return Optional.empty();
    }
}
