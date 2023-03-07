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

package org.opengroup.osdu.entitlements.v2.jdbc.service;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.util.PartitionIndexerServiceAccUtil;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceJdbc implements GroupCacheService {

    private static final String CAN_IMPERSONATE = "users.datalake.delegation";

    private static final String CAN_BE_IMPERSONATED = "users.datalake.impersonation";

    private final JdbcAppProperties config;

    private final ICache<String, ParentReferences> entityGroupsCache;

    private final RetrieveGroupRepo retrieveGroupRepo;

    private final RequestInfo requestInfo;

    private final JaxRsDpsLog log;

    private final PartitionIndexerServiceAccUtil indexerServiceAccProvider;

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
      EntityNode entityNode = getNodeByNodeType(requesterId, partitionId);
      ParentReferences parentReferences = getFromCacheOrLoadParentReferences(entityNode);

      String beneficialId = requestInfo.getHeaders().getOnBehalfOf();
      if (!Strings.isNullOrEmpty(beneficialId)) {
        return verifyAndGetBeneficialGroups(requesterId, partitionId, parentReferences,
            beneficialId).getParentReferencesOfUser();
      }

      Set<ParentReference> dataReferences = getDataGroupsIfTenantIndexerAcc(
          requesterId,
          partitionId);
      if (!dataReferences.isEmpty()) {
        parentReferences.getParentReferencesOfUser().addAll(dataReferences);
      }

      return parentReferences.getParentReferencesOfUser();
    }

    private Set<ParentReference> getDataGroupsIfTenantIndexerAcc(String requesterId,
        String partitionId) {
      Set<ParentReference> parentReferences = Collections.emptySet();
      String tenantIndexerServiceAccount = indexerServiceAccProvider.getTenantIndexerServiceAccount();
      if (requesterId.equals(tenantIndexerServiceAccount)) {
        String indexerDataGroupsKey = indexerServiceAccProvider.getTenantIndexerServiceAccCacheKey(
            tenantIndexerServiceAccount, partitionId);
        ParentReferences dataReferences = entityGroupsCache.get(indexerDataGroupsKey);
        if (dataReferences == null) {
          int cursor = 0;
          int limit = 5000;
          ArrayList<ParentReference> references = new ArrayList<>();
          getTenantDataGroups(references, partitionId, String.valueOf(cursor), limit);
          dataReferences = new ParentReferences();
          dataReferences.setParentReferencesOfUser(new HashSet<>(references));
          entityGroupsCache.put(indexerDataGroupsKey, dataReferences);
        }
        return dataReferences.getParentReferencesOfUser();
      }
      return parentReferences;
    }

    private void getTenantDataGroups(List<ParentReference> parentReferences, String partitionId, String cursor, int limit) {
        ListGroupsOfPartitionDto groupsInPartition = retrieveGroupRepo.getGroupsInPartition(
            partitionId,
            GroupType.DATA,
            String.valueOf(cursor),
            limit
        );
        parentReferences.addAll(groupsInPartition.getGroups());
        if (parentReferences.size() < groupsInPartition.getTotalCount()) {
            getTenantDataGroups(
                parentReferences,
                partitionId,
                groupsInPartition.getCursor(),
                limit
            );
        }
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
        for (String userId : userIds) {
            flushListGroupCacheForUser(userId, partitionId);
        }
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        EntityNode node = getNodeByNodeType(userId, partitionId);
        entityGroupsCache.delete(node.getUniqueIdentifier());
    }

    private EntityNode getNodeByNodeType(String memberId, String partitionId) {
        return memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))
            ? EntityNode.createNodeFromGroupEmail(memberId)
            : EntityNode.createMemberNodeForNewUser(memberId, partitionId);
    }

    private ParentReferences getFromCacheOrLoadParentReferences(EntityNode entityNode) {
        ParentReferences parentReferences = this.entityGroupsCache.get(entityNode.getUniqueIdentifier());
        if (parentReferences == null) {
            Set<ParentReference> parentReferenceSet = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            parentReferences = new ParentReferences();
            parentReferences.setParentReferencesOfUser(parentReferenceSet);
            entityGroupsCache.put(entityNode.getUniqueIdentifier(), parentReferences);
        }
        return parentReferences;
    }

    private ParentReferences verifyAndGetBeneficialGroups(String requesterId, String partitionId, ParentReferences requesterGroups,
        String beneficialId) {
        if (requesterGroups.getParentReferencesOfUser().stream().map(ParentReference::getName).collect(Collectors.toList()).contains(CAN_IMPERSONATE)) {
            EntityNode beneficialNode = getNodeByNodeType(beneficialId, partitionId);
            ParentReferences beneficialGroups = getFromCacheOrLoadParentReferences(beneficialNode);
            Optional<ParentReference> group = beneficialGroups.getParentReferencesOfUser().stream()
                .filter(item -> item.getName().equals(CAN_BE_IMPERSONATED))
                .findFirst();
            if (!group.isPresent()) {
                log.error("Impersonation group not found");
                throw new AppException(HttpStatus.FORBIDDEN.value(),
                    HttpStatus.FORBIDDEN.getReasonPhrase(),
                    "Impersonation not allowed for " + beneficialId);
            } else {
                return beneficialGroups;
            }
        } else {
            log.error("Delegation group not found");
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Impersonation not allowed for " + requesterId);
        }
    }
}
