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
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsGroupCacheTest {

    @Test
    void testGetGroupCache() {
        // Given
        AwsGroupCache awsGroupCache = new AwsGroupCache("localhost", "6379", "password", 300, 1000, "testNamespace");
        String requesterId = "test-user";

        // When
        ParentReferences result = awsGroupCache.getGroupCache(requesterId);

        // Then
        assertNull(result); // Cache will be empty in test
    }

    @Test
    void testAddAndGetGroupCache() {
        // Given
        AwsGroupCache awsGroupCache = new AwsGroupCache("localhost", "6379", "password", 300, 1000, "testNamespace");
        String requesterId = "test-user";
        ParentReferences parentReferences = new ParentReferences();

        // When
        awsGroupCache.addGroupCache(requesterId, parentReferences);
        ParentReferences result = awsGroupCache.getGroupCache(requesterId);

        // Then
        assertEquals(parentReferences, result);
    }

    @Test
    void testDeleteGroupCache() {
        // Given
        AwsGroupCache awsGroupCache = new AwsGroupCache("localhost", "6379", "password", 300, 1000, "testNamespace");
        String requesterId = "test-user";
        ParentReferences parentReferences = new ParentReferences();

        // When
        awsGroupCache.addGroupCache(requesterId, parentReferences);
        awsGroupCache.deleteGroupCache(requesterId);
        ParentReferences result = awsGroupCache.getGroupCache(requesterId);

        // Then
        assertNull(result);
    }
}
