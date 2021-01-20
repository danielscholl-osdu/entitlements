package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.renamegroup;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RenameGroupRepoGremlinTest {
    private static final String TEST_PARTITION_ID = "dp";
    private static final String TEST_DOMAIN = TEST_PARTITION_ID + ".contoso.com";
    private static final String TEST_APP_IDS = "[App1,App2]";

    @Autowired
    private RenameGroupRepo renameGroupRepo;
    @Autowired
    private GremlinConnector gremlinConnector;
    @Autowired
    private VertexUtilService vertexUtilService;
    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldRenameGroupSuccessfully1() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        createTestVertex("users.data.root", NodeType.GROUP);
        createTestVertex("users.x", NodeType.GROUP);
        createTestVertex("data.w", NodeType.GROUP);
        createTestVertex("data.x", NodeType.GROUP);
        createTestVertex("data.y", NodeType.GROUP);
        createTestVertex("data.z", NodeType.GROUP);
        createTestVertex("users.owner", NodeType.USER);
        createTestVertex("users.member", NodeType.USER);
        addTestEdgeAsMember("data.x", "users.data.root");
        addTestEdgeAsMember("data.y", "users.data.root");
        addTestEdgeAsMember("data.z", "users.data.root");
        addTestEdgeAsMember("data.w", "users.data.root");
        addTestEdgeAsMember("users.x", "users.data.root");
        addTestEdgeAsMember("users.member", "users.data.root");
        addTestEdgeAsMember("users.owner", "users.data.root");
        addTestEdgeAsMember("users.x", "data.x");
        addTestEdgeAsMember("users.x", "data.y");
        addTestEdgeAsMember("users.x", "data.z");
        addTestEdgeAsMember("data.w", "users.x");
        addTestEdgeAsOwner("users.owner", "users.x");
        addTestEdgeAsMember("users.member", "users.x");
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users.x" + "@" + TEST_DOMAIN)
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId(TEST_PARTITION_ID)
                .description("")
                .build();

        Set<String> impactedUsers = renameGroupRepo.run(groupNode, "users.y");
        // TODO: 589276 Check impacted users when the logic will be implemented.
        // Assert.assertEquals(new HashSet<>(), impactedUsers);
        Assert.assertFalse(graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.x" + "@" + TEST_DOMAIN)
                .has(VertexPropertyNames.DATA_PARTITION_ID, TEST_PARTITION_ID)
                .hasNext());
        Assert.assertEquals("users.y", graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.y@dp.contoso.com")
                .has(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next()
                .value(VertexPropertyNames.NAME));
        List<Vertex> usersYMembers = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "users.y@dp.contoso.com")
                .outE(EdgePropertyNames.EDGE_LB)
                .has(EdgePropertyNames.ROLE, Role.MEMBER.getValue())
                .inV()
                .toList();
        Assert.assertEquals(2, usersYMembers.size());
        List<Vertex> usersYAllMembers = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "users.y@dp.contoso.com")
                .outE(EdgePropertyNames.EDGE_LB)
                .inV()
                .toList();
        Assert.assertEquals(3, usersYAllMembers.size());
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.y@dp.contoso.com")
                .emit(__.hasLabel(NodeType.GROUP.toString()))
                .repeat(__.in());

        Assert.assertEquals(new HashSet<>(Arrays.asList(
                "users.y@dp.contoso.com",
                "data.x@dp.contoso.com",
                "data.y@dp.contoso.com",
                "data.z@dp.contoso.com",
                "users.data.root@dp.contoso.com"
        )), gremlinConnector.getVertices(traversal).stream()
                .map(vertexUtilService::createParentReference)
                .map(ParentReference::getId)
                .collect(Collectors.toSet()));

    }

    private void addTestEdgeAsOwner(String childName, String parentName) {
        String childNodeId = childName + "@" + TEST_DOMAIN;
        String parentNodeId = parentName + "@" + TEST_DOMAIN;
        graphTraversalSourceUtilService.addEdgeAsOwner(childNodeId, parentNodeId);
    }

    private void addTestEdgeAsMember(String childName, String parentName) {
        String childNodeId = childName + "@" + TEST_DOMAIN;
        String parentNodeId = parentName + "@" + TEST_DOMAIN;
        graphTraversalSourceUtilService.addEdgeAsMember(childNodeId, parentNodeId);
    }

    private void createTestVertex(String name, NodeType nodeType) {
        gremlinConnector.getGraphTraversalSource().addV(nodeType.toString())
                .property(VertexPropertyNames.NODE_ID, name + "@" + TEST_DOMAIN)
                .property(VertexPropertyNames.NAME, name)
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, TEST_PARTITION_ID)
                .property(VertexPropertyNames.APP_IDS, TEST_APP_IDS)
                .next();
    }

    @Test
    public void shouldRenameGroupSuccessfully2() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        createTestVertex("users.data.root", NodeType.GROUP);
        createTestVertex("users.x", NodeType.GROUP);
        createTestVertex("users.y", NodeType.GROUP);
        graphTraversalSource.addV(NodeType.USER.toString())
                .property(VertexPropertyNames.NODE_ID, "member@xxx.com")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .next();
        graphTraversalSourceUtilService.addEdgeAsOwner("member@xxx.com", "users.x@dp.contoso.com");
        addTestEdgeAsMember("users.x", "users.y");
        addTestEdgeAsMember("users.x", "users.data.root");
        addTestEdgeAsMember("users.y", "users.data.root");
        EntityNode groupNode = EntityNode.builder()
                .nodeId("users.x@dp.contoso.com")
                .name("users.x")
                .type(NodeType.GROUP)
                .dataPartitionId("dp")
                .description("")
                .build();
        Set<String> impactedUsers = renameGroupRepo.run(groupNode, "users.z");
        // TODO: 589276 Check impacted users when the logic will be implemented.
        // Assert.assertEquals(new HashSet<>(), impactedUsers);
        Assert.assertFalse(graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.x@dp.contoso.com")
                .has(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .hasNext());
        List<Vertex> members = gremlinConnector.getGraphTraversalSource().V().has(VertexPropertyNames.NODE_ID, "users.z@dp.contoso.com")
                .outE(EdgePropertyNames.EDGE_LB)
                .inV()
                .toList();
        Assert.assertEquals(1, members.size());
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "users.z@dp.contoso.com")
                .emit(__.hasLabel(NodeType.GROUP.toString()))
                .repeat(__.in());
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                "users.y@dp.contoso.com",
                "users.z@dp.contoso.com",
                "users.data.root@dp.contoso.com"
        )), gremlinConnector.getVertices(traversal).stream()
                .map(vertexUtilService::createParentReference)
                .map(ParentReference::getId)
                .collect(Collectors.toSet()));
    }
}
