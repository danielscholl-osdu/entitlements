package org.opengroup.osdu.entitlements.v2.azure.service;

import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReferences;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberCacheServiceAzureTest {

    @InjectMocks
    private MemberCacheServiceAzure memberCacheService;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;

    @Mock
    private RedisAzureCache<String, ChildrenReferences> redisMemberCache;

    @Mock
    private HitsNMissesMetricService metricService;

    @Mock
    private Retry retry;

    @Mock
    private RLock rLock;

    private static final String GROUP_ID = "group@example.com";
    private static final String PARTITION_ID = "partition-1";
    private static final String CACHE_KEY = GROUP_ID + "-" + PARTITION_ID;
    private static final String LOCK_KEY = "lock-" + CACHE_KEY;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(memberCacheService, "redissonLockAcquisitionTimeOut", 5000);
        ReflectionTestUtils.setField(memberCacheService, "redissonLockExpiration", 10000);
        ReflectionTestUtils.setField(memberCacheService, "cacheTtl", 3600);
        ReflectionTestUtils.setField(memberCacheService, "cacheFlushTtlBase", 10000L);
        ReflectionTestUtils.setField(memberCacheService, "cacheFlushTtlJitter", 5000L);
    }

    @Test
    void getFromPartitionCache_shouldReturnFromCache_whenCacheHit() {
        ChildrenReferences cachedReferences = createChildrenReferences();
        when(redisMemberCache.get(CACHE_KEY)).thenReturn(cachedReferences);

        List<ChildrenReference> result = memberCacheService.getFromPartitionCache(GROUP_ID, PARTITION_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(metricService, times(1)).sendHitsMetric();
        verify(metricService, never()).sendMissesMetric();
        verify(redisMemberCache, never()).put(anyString(), anyLong(), any());
    }

    @Test
    void getFromPartitionCache_shouldRebuildCache_whenCacheMiss() throws InterruptedException {
        when(redisMemberCache.get(CACHE_KEY)).thenReturn(null);
        when(redisMemberCache.getLock(LOCK_KEY)).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<ChildrenReference> directChildren = createChildrenReferenceList();
        when(retrieveGroupRepo.loadDirectChildren(PARTITION_ID, GROUP_ID)).thenReturn(directChildren);

        List<ChildrenReference> result = memberCacheService.getFromPartitionCache(GROUP_ID, PARTITION_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(metricService, times(1)).sendMissesMetric();
        verify(metricService, never()).sendHitsMetric();
        verify(redisMemberCache, times(1)).put(eq(CACHE_KEY), anyLong(), any(ChildrenReferences.class));
        verify(rLock, times(1)).unlock();
    }

    @Test
    void getFromPartitionCache_shouldReturnCachedNullChildren() {
        ChildrenReferences cachedReferences = new ChildrenReferences();
        cachedReferences.setChildReferencesOfGroup(null);
        when(redisMemberCache.get(CACHE_KEY)).thenReturn(cachedReferences);
        when(redisMemberCache.getLock(LOCK_KEY)).thenReturn(rLock);

        try {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            List<ChildrenReference> directChildren = createChildrenReferenceList();
            when(retrieveGroupRepo.loadDirectChildren(PARTITION_ID, GROUP_ID)).thenReturn(directChildren);

            List<ChildrenReference> result = memberCacheService.getFromPartitionCache(GROUP_ID, PARTITION_ID);

            assertNotNull(result);
            verify(redisMemberCache, times(1)).put(eq(CACHE_KEY), anyLong(), any(ChildrenReferences.class));
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        }
    }

    @Test
    void getFromPartitionCache_shouldHandleNullLock() {
        when(redisMemberCache.get(CACHE_KEY)).thenReturn(null);
        when(redisMemberCache.getLock(LOCK_KEY)).thenReturn(null);

        List<ChildrenReference> directChildren = createChildrenReferenceList();
        when(retrieveGroupRepo.loadDirectChildren(PARTITION_ID, GROUP_ID)).thenReturn(directChildren);

        List<ChildrenReference> result = memberCacheService.getFromPartitionCache(GROUP_ID, PARTITION_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(log, times(1)).info(contains("Redis secrets"));
        verify(rLock, never()).unlock();
    }

    @Test
    void getFromPartitionCache_shouldHandleInterruptedException() throws InterruptedException {
        when(redisMemberCache.get(CACHE_KEY)).thenReturn(null);
        when(redisMemberCache.getLock(LOCK_KEY)).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("Test interrupt"));

        assertThrows(AppException.class, () -> 
            memberCacheService.getFromPartitionCache(GROUP_ID, PARTITION_ID)
        );

        verify(log, times(1)).error(contains("InterruptedException"));
    }

    @Test
    void getFromPartitionCache_shouldHandleUnlockException() throws InterruptedException {
        when(redisMemberCache.get(CACHE_KEY)).thenReturn(null);
        when(redisMemberCache.getLock(LOCK_KEY)).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        doThrow(new RuntimeException("Unlock failed")).when(rLock).unlock();

        List<ChildrenReference> directChildren = createChildrenReferenceList();
        when(retrieveGroupRepo.loadDirectChildren(PARTITION_ID, GROUP_ID)).thenReturn(directChildren);

        List<ChildrenReference> result = memberCacheService.getFromPartitionCache(GROUP_ID, PARTITION_ID);

        assertNotNull(result);
        verify(log, times(1)).warning(contains("unlock exception"));
    }

    @Test
    void flushListMemberCacheForGroup_shouldUpdateTtl_whenTtlAboveThreshold() {
        long currentTtl = 20000L; // Above base + jitter
        when(redisMemberCache.getTtl(CACHE_KEY)).thenReturn(currentTtl);

        memberCacheService.flushListMemberCacheForGroup(GROUP_ID, PARTITION_ID);

        verify(redisMemberCache, times(1)).updateTtl(eq(CACHE_KEY), anyLong());
    }

    @Test
    void flushListMemberCacheForGroup_shouldNotUpdateTtl_whenTtlBelowThreshold() {
        long currentTtl = 5000L; // Below base + jitter
        when(redisMemberCache.getTtl(CACHE_KEY)).thenReturn(currentTtl);

        memberCacheService.flushListMemberCacheForGroup(GROUP_ID, PARTITION_ID);

        verify(redisMemberCache, never()).updateTtl(anyString(), anyLong());
    }

    @Test
    void flushListMemberCacheForGroup_shouldNotUpdateTtl_whenTtlEqualsThreshold() {
        long currentTtl = 15000L; // Exactly base + jitter
        when(redisMemberCache.getTtl(CACHE_KEY)).thenReturn(currentTtl);

        memberCacheService.flushListMemberCacheForGroup(GROUP_ID, PARTITION_ID);

        verify(redisMemberCache, never()).updateTtl(anyString(), anyLong());
    }

    private ChildrenReferences createChildrenReferences() {
        ChildrenReferences references = new ChildrenReferences();
        references.setChildReferencesOfGroup(createChildrenReferenceList());
        return references;
    }

    private List<ChildrenReference> createChildrenReferenceList() {
        ChildrenReference child1 = ChildrenReference.builder()
                .id("user1@example.com")
                .type(NodeType.USER)
                .role(Role.MEMBER)
                .dataPartitionId(PARTITION_ID)
                .build();

        ChildrenReference child2 = ChildrenReference.builder()
                .id("user2@example.com")
                .type(NodeType.USER)
                .role(Role.OWNER)
                .dataPartitionId(PARTITION_ID)
                .build();

        return Arrays.asList(child1, child2);
    }
}
