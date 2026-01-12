/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.entitlements.v2.aws.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.mongodb.helper.BasicMongoDBHelper;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.UserHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper.IndexUpdater;
import org.springframework.dao.DuplicateKeyException;
import org.apache.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserHelperWithMocksTest {

    @Mock
    private BasicMongoDBHelper basicMongoDBHelper;

    @Mock
    private IndexUpdater indexUpdater;

    @InjectMocks
    private UserHelper userHelper;

    private UserDoc testUserDoc;
    private IdDoc testIdDoc;

    @BeforeEach
    void setUp() {
        testIdDoc = new IdDoc();
        testIdDoc.setDataPartitionId("test-partition");
        testIdDoc.setNodeId("test-user-id");

        testUserDoc = new UserDoc();
        testUserDoc.setId(testIdDoc);
    }

    @Test
    void testGetOrCreate_RetrySucceedsOnThirdAttempt() {
        // Given: Initial getById returns null, save throws DuplicateKeyException, first two retries fail, third succeeds
        when(basicMongoDBHelper.getById(any(IdDoc.class), eq(UserDoc.class), anyString()))
                .thenReturn(null)  // Initial check
                .thenReturn(null)  // First retry fails
                .thenReturn(null)  // Second retry fails
                .thenReturn(testUserDoc);  // Third retry succeeds

        doThrow(new DuplicateKeyException("Duplicate key"))
                .when(basicMongoDBHelper).insert(any(UserDoc.class), anyString());

        // When
        UserDoc result = userHelper.getOrCreate(testUserDoc);

        // Then
        assertNotNull(result);
        assertEquals(testUserDoc.getId().getNodeId(), result.getId().getNodeId());
        assertEquals(testUserDoc.getId().getDataPartitionId(), result.getId().getDataPartitionId());

        // Verify getById was called 4 times (initial + 3 retries)
        verify(basicMongoDBHelper, times(4)).getById(any(IdDoc.class), eq(UserDoc.class), anyString());
        verify(basicMongoDBHelper, times(1)).insert(any(UserDoc.class), anyString());
    }

    @Test
    void testGetOrCreate_AllRetriesFailThrowsAppException() {
        // Given: Initial getById returns null, save throws DuplicateKeyException, all retries fail
        when(basicMongoDBHelper.getById(any(IdDoc.class), eq(UserDoc.class), anyString()))
                .thenReturn(null);  // Always return null

        DuplicateKeyException duplicateKeyException = new DuplicateKeyException("Duplicate key");
        doThrow(duplicateKeyException)
                .when(basicMongoDBHelper).insert(any(UserDoc.class), anyString());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            userHelper.getOrCreate(testUserDoc);
        });

        // Verify the root cause is the DuplicateKeyException
        assertEquals(duplicateKeyException, exception.getCause());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getError().getCode());

        // Verify getById was called 4 times (initial + 3 retries)
        verify(basicMongoDBHelper, times(4)).getById(any(IdDoc.class), eq(UserDoc.class), anyString());
        verify(basicMongoDBHelper, times(1)).insert(any(UserDoc.class), anyString());
    }

    @Test
    void testGetOrCreate_UserExistsInitiallyNoRetryNeeded() {
        // Given: Initial getById returns user
        when(basicMongoDBHelper.getById(any(IdDoc.class), eq(UserDoc.class), anyString()))
                .thenReturn(testUserDoc);

        // When
        UserDoc result = userHelper.getOrCreate(testUserDoc);

        // Then
        assertNotNull(result);
        assertEquals(testUserDoc.getId().getNodeId(), result.getId().getNodeId());
        assertEquals(testUserDoc.getId().getDataPartitionId(), result.getId().getDataPartitionId());

        // Verify getById was called only once and insert was never called
        verify(basicMongoDBHelper, times(1)).getById(any(IdDoc.class), eq(UserDoc.class), anyString());
        verify(basicMongoDBHelper, never()).insert(any(UserDoc.class), anyString());
    }

    @Test
    void testGetOrCreate_SaveSucceedsNoRetryNeeded() {
        // Given: Initial getById returns null, save succeeds
        when(basicMongoDBHelper.getById(any(IdDoc.class), eq(UserDoc.class), anyString()))
                .thenReturn(null);

        doNothing().when(basicMongoDBHelper).insert(any(UserDoc.class), anyString());

        // When
        UserDoc result = userHelper.getOrCreate(testUserDoc);

        // Then
        assertNotNull(result);
        assertEquals(testUserDoc.getId().getNodeId(), result.getId().getNodeId());
        assertEquals(testUserDoc.getId().getDataPartitionId(), result.getId().getDataPartitionId());

        // Verify getById was called only once and insert was called once
        verify(basicMongoDBHelper, times(1)).getById(any(IdDoc.class), eq(UserDoc.class), anyString());
        verify(basicMongoDBHelper, times(1)).insert(any(UserDoc.class), anyString());
    }

    @Test
    void testGetOrCreate_InterruptedDuringRetry() {
        // Given: Initial getById returns null, save throws DuplicateKeyException, retries return null
        when(basicMongoDBHelper.getById(any(IdDoc.class), eq(UserDoc.class), anyString()))
                .thenReturn(null);

        DuplicateKeyException duplicateKeyException = new DuplicateKeyException("Duplicate key");
        doThrow(duplicateKeyException)
                .when(basicMongoDBHelper).insert(any(UserDoc.class), anyString());

        // Interrupt the current thread to simulate interruption during sleep
        Thread.currentThread().interrupt();

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            userHelper.getOrCreate(testUserDoc);
        });

        // Verify the root cause is the DuplicateKeyException
        assertEquals(duplicateKeyException, exception.getCause());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getError().getCode());
        assertTrue(Thread.currentThread().isInterrupted());

        // Clear the interrupt flag for other tests
        Thread.interrupted();
    }
}
