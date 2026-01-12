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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupCacheServiceAwsTest {

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;

    @Mock
    private AwsGroupCache awsGroupCache;

    private GroupCacheServiceAws groupCacheService;

    @BeforeEach
    void setUp() {
        groupCacheService = new GroupCacheServiceAws(retrieveGroupRepo, awsGroupCache);
        // Use reflection to set private field for testing
        try {
            var field = GroupCacheServiceAws.class.getDeclaredField("domain");
            field.setAccessible(true);
            field.set(groupCacheService, "example.com");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetFromPartitionCache_CacheHit() {
        // Given
        String requesterId = "user@test.example.com";
        String partitionId = "test-partition";
        String expectedKey = "user@test.example.com-test-partition";
        
        ParentReferences cachedReferences = new ParentReferences();
        Set<ParentReference> expectedParents = Set.of(new ParentReference());
        cachedReferences.setParentReferencesOfUser(expectedParents);
        
        when(awsGroupCache.getGroupCache(expectedKey)).thenReturn(cachedReferences);

        // When
        Set<ParentReference> result = groupCacheService.getFromPartitionCache(requesterId, partitionId);

        // Then
        assertEquals(expectedParents, result);
        verify(awsGroupCache).getGroupCache(expectedKey);
        verify(retrieveGroupRepo, never()).loadAllParents(any());
    }

    @Test
    void testGetFromPartitionCache_CacheMiss() {
        // Given
        String requesterId = "user@test.example.com";
        String partitionId = "test-partition";
        String expectedKey = "user@test.example.com-test-partition";
        
        Set<ParentReference> expectedParents = Set.of(new ParentReference());
        ParentTreeDto parentTreeDto = ParentTreeDto.builder()
                .parentReferences(expectedParents)
                .maxDepth(1)
                .build();
        
        when(awsGroupCache.getGroupCache(expectedKey)).thenReturn(null);
        when(retrieveGroupRepo.loadAllParents(any())).thenReturn(parentTreeDto);

        // When
        Set<ParentReference> result = groupCacheService.getFromPartitionCache(requesterId, partitionId);

        // Then
        assertEquals(expectedParents, result);
        verify(awsGroupCache).getGroupCache(expectedKey);
        verify(retrieveGroupRepo).loadAllParents(any());
        verify(awsGroupCache).addGroupCache(eq(expectedKey), any(ParentReferences.class));
    }

    @Test
    void testRefreshListGroupCache() {
        // Given
        Set<String> userIds = Set.of("user1@test.com", "user2@test.com");
        String partitionId = "test-partition";

        // When
        groupCacheService.refreshListGroupCache(userIds, partitionId);

        // Then
        verify(awsGroupCache).deleteGroupCache("user1@test.com-test-partition");
        verify(awsGroupCache).deleteGroupCache("user2@test.com-test-partition");
    }

    @Test
    void testFlushListGroupCacheForUser() {
        // Given
        String userId = "user@test.com";
        String partitionId = "test-partition";
        String expectedKey = "user@test.com-test-partition";

        // When
        groupCacheService.flushListGroupCacheForUser(userId, partitionId);

        // Then
        verify(awsGroupCache).deleteGroupCache(expectedKey);
    }
}
