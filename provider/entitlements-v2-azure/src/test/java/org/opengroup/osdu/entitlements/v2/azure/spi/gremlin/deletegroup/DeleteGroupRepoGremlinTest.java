package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.deletegroup;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember.AddMemberRepoGremlin;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.deletegroup.DeleteGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DeleteGroupRepoGremlinTest {

    @Autowired
    private DeleteGroupRepo deleteGroupRepo;

    @Autowired
    private GremlinConnector gremlinConnector;

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private AddMemberRepoGremlin addMemberRepoGremlin;

    @MockBean
    private AuditLogger auditLogger;

    @MockBean
    private CacheConfig cacheConfig;

    @After
    public void cleanup() {
        gremlinConnector.getGraphTraversalSource().V().drop().iterate();
    }

    @Test
    public void shouldSuccessfullyDeleteGroup() {
        createGroup("groupId");
        createGroup("groupMemberId");
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").dataPartitionId("dp").type(NodeType.GROUP).build();
        EntityNode groupMemberNode = EntityNode.builder().nodeId("groupMemberId").dataPartitionId("dp").type(NodeType.GROUP).build();
        AddMemberRepoDto addGroupMemberRepoDto = AddMemberRepoDto.builder().memberNode(groupMemberNode).role(Role.MEMBER).partitionId("dp").build();
        addMemberRepoGremlin.addMember(groupNode, addGroupMemberRepoDto);
        EntityNode userNode = EntityNode.builder().nodeId("userId").dataPartitionId("dp").type(NodeType.USER).build();
        AddMemberRepoDto addUserMemberRepoDto = AddMemberRepoDto.builder().memberNode(userNode).role(Role.MEMBER).partitionId("dp").build();
        addMemberRepoGremlin.addMember(groupMemberNode, addUserMemberRepoDto);

        Set<String> impactedUsers = deleteGroupRepo.deleteGroup(groupMemberNode);

        Assert.assertFalse(gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "groupMemberId").hasNext());
        assertTrue(gremlinConnector.getGraphTraversalSource().E().toList().isEmpty());
        verify(auditLogger).deleteGroup(AuditStatus.SUCCESS, "groupMemberId");
        Assert.assertEquals(1, impactedUsers.size());
        assertTrue(impactedUsers.contains("userId"));
    }

    @Test
    public void shouldReturnEmptySet() {
        Deque<Operation> executedCommandsDeque = new LinkedList<>();
        EntityNode groupMemberNode = EntityNode.builder().name("testGroup").nodeId("testNodeId").build();

        Set<String> impactedUsers = deleteGroupRepo.deleteGroup(executedCommandsDeque, groupMemberNode);

        assertTrue(impactedUsers.isEmpty());
    }

    private void createGroup(String nodeId) {
        gremlinConnector.getGraphTraversalSource().addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, nodeId)
                .property(VertexPropertyNames.NAME, "")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp").next();
    }
}
