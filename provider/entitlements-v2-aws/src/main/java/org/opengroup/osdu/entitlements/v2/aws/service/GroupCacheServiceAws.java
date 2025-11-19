/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceAws implements GroupCacheService {

    private static final String CACHE_KEY_FORMAT = "%s-%s";

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final AwsGroupCache awsGroupCache;

    @Value("${app.domain}")
    private String domain;

    private EntityNode createEntityNode(String requesterId, String partitionId) {
        Pattern domainPattern = Pattern.compile(String.format(".+@%s\\.%s", Pattern.quote(partitionId), Pattern.quote(domain)));
        if (domainPattern.matcher(requesterId).matches()) {
            return EntityNode.createNodeFromGroupEmail(requesterId);
        } else {
            return EntityNode.createMemberNodeForNewUser(requesterId, partitionId);
        }
    }

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        String key = String.format(CACHE_KEY_FORMAT, requesterId, partitionId);
        ParentReferences parentReferences = awsGroupCache.getGroupCache(key);
        if (parentReferences == null) {
            EntityNode entityNode = createEntityNode(requesterId, partitionId);
            Set<ParentReference> allParents = retrieveGroupRepo.loadAllParents(entityNode).getParentReferences();
            parentReferences = new ParentReferences();
            parentReferences.setParentReferencesOfUser(allParents);
            awsGroupCache.addGroupCache(key, parentReferences);
        }
        return parentReferences.getParentReferencesOfUser();
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
        for (String userId : userIds) {
            String key = String.format(CACHE_KEY_FORMAT, userId, partitionId);
            // Force cache invalidation - delete the entry completely
            awsGroupCache.deleteGroupCache(key);
        }
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        String key = String.format(CACHE_KEY_FORMAT, userId, partitionId);
        // Force cache invalidation - delete the entry completely  
        awsGroupCache.deleteGroupCache(key);
    }
}
