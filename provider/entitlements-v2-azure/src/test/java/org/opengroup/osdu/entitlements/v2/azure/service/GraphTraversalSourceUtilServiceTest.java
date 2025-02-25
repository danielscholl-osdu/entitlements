package org.opengroup.osdu.entitlements.v2.azure.service;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GraphTraversalSourceUtilServiceTest {
    @Autowired
    private GremlinConnector gremlinConnector;
    @Autowired
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;
    @MockBean
    private CacheConfig cacheConfig;

    @After
    public void cleanup() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        graphTraversalSource.V().drop().iterate();
        graphTraversalSource.E().drop().iterate();
    }

    @Test
    public void shouldAddEdgeAsOwnerSuccessfullyWhenTwoChildNodesWithSameNodeId() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "member1@domain.com")
                .property(VertexPropertyNames.NAME, "member1")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp1")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "member1@domain.com")
                .property(VertexPropertyNames.NAME, "member1")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp2")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();


        AddEdgeDto addEdgeDto = AddEdgeDto.builder()
                .toNodeId("member1@domain.com")
                .fromNodeId("data.x@dp.domain.com")
                .dpOfToNodeId("dp2")
                .dpOfFromNodeId("dp")
                .edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.OWNER.getValue()))
                .edgeLabel("edge-label")
                .build();
        graphTraversalSourceUtilService.addEdge(addEdgeDto);

        Vertex vertex = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, addEdgeDto.getToNodeId())
                .has(VertexPropertyNames.DATA_PARTITION_ID, addEdgeDto.getDpOfToNodeId())
                .next();

        Edge edge = vertex.edges(Direction.IN).next();
        Assert.assertEquals("data.x@dp.domain.com", edge.outVertex().value(VertexPropertyNames.NODE_ID));
        Assert.assertEquals(Role.OWNER.getValue(), edge.value(EdgePropertyNames.ROLE));
    }

    @Test
    public void shouldAddEdgeAsOwnerSuccessfully() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.y")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();
        AddEdgeDto addEdgeDto = AddEdgeDto.builder()
                .toNodeId("data.y@dp.domain.com")
                .fromNodeId("data.x@dp.domain.com")
                .dpOfToNodeId("dp")
                .dpOfFromNodeId("dp")
                .edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.OWNER.getValue()))
                .edgeLabel("edge-label")
                .build();
        graphTraversalSourceUtilService.addEdge(addEdgeDto);

        Vertex vertex = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com").next();

        Edge edge = vertex.edges(Direction.IN).next();
        Assert.assertEquals("data.x@dp.domain.com", edge.outVertex().value(VertexPropertyNames.NODE_ID));
        Assert.assertEquals(Role.OWNER.getValue(), edge.value(EdgePropertyNames.ROLE));
    }

    @Test
    public void shouldAddEdgeAsMemberSuccessfully() {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.x@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.x")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        graphTraversalSource.addV(NodeType.GROUP.toString())
                .property(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com")
                .property(VertexPropertyNames.NAME, "data.y")
                .property(VertexPropertyNames.DESCRIPTION, "")
                .property(VertexPropertyNames.DATA_PARTITION_ID, "dp")
                .property(VertexPropertyNames.APP_ID, "App1")
                .property(VertexPropertyNames.APP_ID, "App2")
                .next();

        AddEdgeDto addEdgeDto = AddEdgeDto.builder()
                .toNodeId("data.y@dp.domain.com")
                .fromNodeId("data.x@dp.domain.com")
                .dpOfToNodeId("dp")
                .dpOfFromNodeId("dp")
                .edgeProperties(Collections.singletonMap(EdgePropertyNames.ROLE, Role.MEMBER.getValue()))
                .edgeLabel("edge-label")
                .build();
        graphTraversalSourceUtilService.addEdge(addEdgeDto);

        Vertex vertex = graphTraversalSource.V().has(VertexPropertyNames.NODE_ID, "data.y@dp.domain.com").next();

        Edge edge = vertex.edges(Direction.IN).next();
        Assert.assertEquals("data.x@dp.domain.com", edge.outVertex().value(VertexPropertyNames.NODE_ID));
        Assert.assertEquals(Role.MEMBER.getValue(), edge.value(EdgePropertyNames.ROLE));
    }

    @Test
    public void shouldCreateVertexSuccessfully() {
        EntityNode entityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();

        NodeVertex vertex = graphTraversalSourceUtilService.createGroupVertexFromEntityNode(entityNode);

        Assert.assertEquals("groupId", vertex.getNodeId());
        Assert.assertEquals("name", vertex.getName());
        Assert.assertEquals("desc", vertex.getDescription());
        Assert.assertEquals("dp", vertex.getDataPartitionId());
        Assert.assertEquals(new HashSet<>(Arrays.asList("App1", "App2")), vertex.getAppIds());
    }

    @Test
    public void shouldNotThrowErrorIfVertexExistsIdempotent() {
        EntityNode entityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();

        graphTraversalSourceUtilService.createGroupVertexFromEntityNode(entityNode);
        NodeVertex vertex = graphTraversalSourceUtilService.createVertexFromEntityNodeIdempotent(entityNode);

        Assert.assertEquals("groupId", vertex.getNodeId());
        Assert.assertEquals("name", vertex.getName());
        Assert.assertEquals("desc", vertex.getDescription());
        Assert.assertEquals("dp", vertex.getDataPartitionId());
        Assert.assertEquals(new HashSet<>(Arrays.asList("App1", "App2")), vertex.getAppIds());
    }

    @Test
    public void shouldThrowConflictErrorIfVertexExistsWhenCreateGroup() {
        EntityNode entityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();

        graphTraversalSourceUtilService.createGroupVertexFromEntityNode(entityNode);

        try {
            graphTraversalSourceUtilService.createGroupVertex(entityNode);
            fail("should throw exception");
        } catch (AppException ex) {
            assertEquals(409, ex.getError().getCode());
        } catch (Exception ex) {
            fail(String.format("should not throw exception: %s", ex.getMessage()));
        }
    }

    @Test
    public void shouldNotThrowConflictErrorIfVertexExistsInOtherDp() {
        EntityNode entityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();

        graphTraversalSourceUtilService.createGroupVertexFromEntityNode(entityNode);

        EntityNode otherEntityNode = EntityNode.builder()
                .nodeId("groupId").name("name").description("desc").type(NodeType.GROUP)
                .dataPartitionId("dp2").appIds(new HashSet<>(Arrays.asList("App1", "App2"))).build();


        graphTraversalSourceUtilService.createGroupVertex(otherEntityNode);
    }
}
