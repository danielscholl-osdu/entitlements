package org.opengroup.osdu.entitlements.v2.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.featureflag.FeatureFlag;
import org.opengroup.osdu.entitlements.v2.service.featureflag.PartitionFeatureFlagService;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateGroupServiceTests {

    @Mock
    private CreateGroupRepo createGroupRepo;
    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;
    @Mock
    private GroupCacheService groupCacheService;
    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private DefaultGroupsService defaultGroupsService;
    @Mock
    private PartitionFeatureFlagService partitionFeatureFlagService;

    @InjectMocks
    private CreateGroupService createGroupService;

    @Before
    public void setup() {
        Whitebox.setInternalState(createGroupService, "dataRootGroupQuota", 5000);
    }

    @Test
    public void shouldThrow412IfRequesterQuotaHit() {
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("data.x")
                .dataPartitionId("dp")
                .build();
        Set<ParentReference> parents = mock(HashSet.class);
        when(parents.size()).thenReturn(5000);
        EntityNode requesterNode = EntityNode.builder()
                .nodeId("callerdesid")
                .type(NodeType.USER)
                .name("callerdesid")
                .dataPartitionId("dp")
                .build();
        when(groupCacheService.getFromPartitionCache(requesterNode.getNodeId(), "dp")).thenReturn(parents);
        try {
            CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                    .requesterId("callerdesid")
                    .partitionDomain("dp.domain.com")
                    .partitionId("dp").build();
            createGroupService.run(groupNode, createGroupServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            assertEquals(412, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldThrow412IfUserOrDataGroupAndDataRootQuotaHit() {
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("data.x")
                .dataPartitionId("dp")
                .build();
        Set<ParentReference> parents = mock(HashSet.class);
        when(parents.size()).thenReturn(5000);
        EntityNode dataRootGroupNode = EntityNode.builder()
                .nodeId("users.data.root@dp.domain.com")
                .type(NodeType.GROUP)
                .name("users.data.root")
                .dataPartitionId("dp")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@dp.domain.com", "dp")).thenReturn(dataRootGroupNode);
        when(defaultGroupsService.isNotDefaultGroupName("data.x")).thenReturn(true);
        when(retrieveGroupRepo.loadAllParents(dataRootGroupNode)).thenReturn(ParentTreeDto.builder().parentReferences(parents).maxDepth(2).build());
        try {
            CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                    .requesterId("callerdesid")
                    .partitionDomain("dp.domain.com")
                    .partitionId("dp").build();
            createGroupService.run(groupNode, createGroupServiceDto);
            fail("should throw exception");
        } catch (AppException ex) {
            assertEquals(412, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldCallRepoAddDataRootGroupIfDataGroup() {
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("data.x")
                .dataPartitionId("dp")
                .build();
        Set<ParentReference> parents = mock(HashSet.class);
        when(parents.size()).thenReturn(4999);
        EntityNode dataRootGroupNode = EntityNode.builder()
                .nodeId("users.data.root@dp.domain.com")
                .type(NodeType.GROUP)
                .name("users.data.root")
                .dataPartitionId("dp")
                .build();
        when(retrieveGroupRepo.groupExistenceValidation("users.data.root@dp.domain.com", "dp")).thenReturn(dataRootGroupNode);
        when(retrieveGroupRepo.loadAllParents(dataRootGroupNode)).thenReturn(ParentTreeDto.builder().parentReferences(parents).maxDepth(2).build());
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionDomain("dp.domain.com")
                .partitionId("dp").build();
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester("callerdesid", "dp");
        when(groupCacheService.getFromPartitionCache(requesterNode.getNodeId(), "dp")).thenReturn(Collections.emptySet());
        ArgumentCaptor<CreateGroupRepoDto> captor = ArgumentCaptor.forClass(CreateGroupRepoDto.class);
        when(defaultGroupsService.isNotDefaultGroupName("data.x")).thenReturn(true);
        Set<String> impactedUsers = new HashSet<>(Arrays.asList("callerdesid"));
        when(createGroupRepo.createGroup(any(), any())).thenReturn(impactedUsers);
        createGroupService.run(groupNode, createGroupServiceDto);
        verify(createGroupRepo, times(1)).createGroup(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getDataRootGroupNode()).isNotNull();
        assertThat(captor.getValue().isAddDataRootGroup()).isTrue();
        verify(groupCacheService).refreshListGroupCache(impactedUsers, "dp");
    }

    @Test
    public void shouldCallRepoAddNotDataRootGroupIfDataGroupButDisableFeatureFlagIsEnabled() {
        EntityNode groupNode = EntityNode.builder()
                .nodeId("data.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("data.x")
                .dataPartitionId("dp")
                .build();
        Set<ParentReference> parents = mock(HashSet.class);
        EntityNode dataRootGroupNode = EntityNode.builder()
                .nodeId("users.data.root@dp.domain.com")
                .type(NodeType.GROUP)
                .name("users.data.root")
                .dataPartitionId("dp")
                .build();
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionDomain("dp.domain.com")
                .partitionId("dp").build();
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester("callerdesid", "dp");
        when(groupCacheService.getFromPartitionCache(requesterNode.getNodeId(), "dp")).thenReturn(Collections.emptySet());
        ArgumentCaptor<CreateGroupRepoDto> captor = ArgumentCaptor.forClass(CreateGroupRepoDto.class);
        Set<String> impactedUsers = new HashSet<>(Arrays.asList("callerdesid"));
        when(createGroupRepo.createGroup(any(), any())).thenReturn(impactedUsers);
        when(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).thenReturn(true);
        createGroupService.run(groupNode, createGroupServiceDto);
        verify(createGroupRepo, times(1)).createGroup(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getDataRootGroupNode()).isNull();
        assertThat(captor.getValue().isAddDataRootGroup()).isFalse();
        verify(groupCacheService).refreshListGroupCache(impactedUsers, "dp");
    }

    @Test
    public void shouldCallRepoAndNotAddDataRootGroupIfNotDataGroup() {
        EntityNode groupNode = EntityNode.builder()
                .nodeId("service.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("service.x")
                .dataPartitionId("dp")
                .build();
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionDomain("dp.domain.com")
                .partitionId("dp").build();
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester("callerdesid", "dp");
        when(groupCacheService.getFromPartitionCache(requesterNode.getNodeId(), "dp")).thenReturn(Collections.emptySet());
        ArgumentCaptor<CreateGroupRepoDto> captor = ArgumentCaptor.forClass(CreateGroupRepoDto.class);
        Set<String> impactedUsers = new HashSet<>(Arrays.asList("callerdesid"));
        when(createGroupRepo.createGroup(any(), any())).thenReturn(impactedUsers);
        createGroupService.run(groupNode, createGroupServiceDto);
        verify(createGroupRepo, times(1)).createGroup(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getDataRootGroupNode()).isNull();
        assertThat(captor.getValue().isAddDataRootGroup()).isFalse();
        verify(groupCacheService).refreshListGroupCache(impactedUsers, "dp");
    }

    @Test
    public void shouldCallRepoAndNotAddDataRootGroupIfDefaultDataGroup() {
        EntityNode groupNode = EntityNode.builder()
                .nodeId("service.x@dp.domain.com")
                .type(NodeType.GROUP)
                .name("data.x")
                .dataPartitionId("dp")
                .build();
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId("callerdesid")
                .partitionDomain("dp.domain.com")
                .partitionId("dp").build();
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester("callerdesid", "dp");
        when(groupCacheService.getFromPartitionCache(requesterNode.getNodeId(), "dp")).thenReturn(Collections.emptySet());
        ArgumentCaptor<CreateGroupRepoDto> captor = ArgumentCaptor.forClass(CreateGroupRepoDto.class);
        when(defaultGroupsService.isNotDefaultGroupName("data.x")).thenReturn(false);
        Set<String> impactedUsers = new HashSet<>(Arrays.asList("callerdesid"));
        when(createGroupRepo.createGroup(any(), any())).thenReturn(impactedUsers);
        createGroupService.run(groupNode, createGroupServiceDto);
        verify(createGroupRepo, times(1)).createGroup(eq(groupNode), captor.capture());
        assertThat(captor.getValue().getDataRootGroupNode()).isNull();
        assertThat(captor.getValue().isAddDataRootGroup()).isFalse();
        verify(groupCacheService).refreshListGroupCache(impactedUsers, "dp");
    }

    @Test
    public void shouldThrowConflictIfGroupIsAlreadyParentOfRequester() {
        String groupNodeId = "data.x@dp.domain.com";
        String requesterId = "callerdesid";
        String partitionId = "dp";

        // The group we are trying to create
        EntityNode groupNode = EntityNode.builder()
                .nodeId(groupNodeId)
                .type(NodeType.GROUP)
                .name("data.x")
                .dataPartitionId(partitionId)
                .build();

        // Simulate that this group is already a parent of the requester
        ParentReference matchingParent = ParentReference.builder()
                .id(groupNodeId)
                .build();

        Set<ParentReference> parents = new HashSet<>(Collections.singletonList(matchingParent));
        // Mock the cache to return the parent group
        when(groupCacheService.getFromPartitionCache(requesterId, partitionId)).thenReturn(parents);

        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId(requesterId)
                .partitionDomain("dp.domain.com")
                .partitionId(partitionId)
                .build();

        boolean exceptionThrown = false;
        try {
            createGroupService.run(groupNode, createGroupServiceDto);
        } catch (AppException ex) {
            exceptionThrown = true;
            assertEquals(409, ex.getError().getCode());
            assertTrue(ex.getError().getMessage().contains("This group already exists"));
        }

        assertTrue("Expected AppException to be thrown", exceptionThrown);
        verify(createGroupRepo, times(0)).createGroup(any(), any());
        verify(groupCacheService, times(0)).refreshListGroupCache(any(), any());
    }
}