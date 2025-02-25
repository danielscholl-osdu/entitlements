package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.retrievegroup;

import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.EdgePropertyNames;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.StepLabel;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.model.*;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RetrieveGroupRepoGremlin implements RetrieveGroupRepo {
    private final GremlinConnector gremlinConnector;
    private final VertexUtilService vertexUtilService;

    @Override
    public EntityNode groupExistenceValidation(String groupId, String partitionId) {
        return getEntityNode(groupId, partitionId).orElseThrow(() ->
                new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), ""));
    }

    @Override
    public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {
        GraphTraversalSource graphTraversalSource = gremlinConnector.getGraphTraversalSource();
        Traversal<Vertex, Vertex> traversal = graphTraversalSource.V()
                .has(VertexPropertyNames.NODE_ID, entityEmail)
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId);
        return gremlinConnector.getVertex(traversal).map(vertexUtilService::createMemberNode);
    }

    @Override
    public EntityNode getMemberNodeForRemovalFromGroup(String memberId, String partitionId) {
        return getEntityNode(memberId, partitionId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        String.format("Not found entity node by email: %s and partitionId: %s", memberId, partitionId)));
    }

    @Override
    public Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds) {
        return new HashSet<>();
    }

    @Override
    public Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds) {
        return new HashMap<>();
    }

    @Override
    public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain) {
        return new HashSet<>();
    }

    @Override
    public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.NODE_ID, groupNode.getNodeId())
                .has(VertexPropertyNames.DATA_PARTITION_ID, groupNode.getDataPartitionId())
                .outE(EdgePropertyNames.CHILD_EDGE_LB)
                .has(EdgePropertyNames.ROLE, childrenReference.getRole().getValue())
                .inV()
                .has(VertexPropertyNames.NODE_ID, childrenReference.getId())
                .hasLabel(childrenReference.getType().toString())
                .has(VertexPropertyNames.DATA_PARTITION_ID, childrenReference.getDataPartitionId());
        return gremlinConnector.hasVertex(traversal);
    }

    @Override
    public List<ParentReference> loadDirectParents(String partitionId, String... nodeIds) {
        final List<ParentReference> resultList = new ArrayList<>();
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId)
                .or(buildOrTraversalsByNodeIds(nodeIds))
                .outE(EdgePropertyNames.PARENT_EDGE_LB)
                .inV()
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId);
        gremlinConnector.getVertices(traversal)
                .forEach(v -> resultList.add(vertexUtilService.createParentReference(v)));
        return resultList;
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode) {
        return loadAllParents(memberNode, false);
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode, Boolean roleRequired ) {
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, memberNode.getDataPartitionId())
                .has(VertexPropertyNames.NODE_ID, memberNode.getNodeId())
                .emit(__.hasLabel(NodeType.GROUP.toString()))
                .repeat(__.outE(EdgePropertyNames.PARENT_EDGE_LB).inV());

        List<NodeVertex> vertices = gremlinConnector.getVertices(traversal);

        Set<ParentReference> parentReferences = vertices.stream()
                .map(vertexUtilService::createParentReference)
                .collect(Collectors.toSet());

        if(Boolean.TRUE == roleRequired)
        {
            parentReferences.parallelStream().forEach(parentReference -> {
                GraphTraversal<Vertex, List<Edge>> traversalEdge = gremlinConnector.getGraphTraversalSource().V()
                        .has(VertexPropertyNames.DATA_PARTITION_ID, memberNode.getDataPartitionId())
                        .has(VertexPropertyNames.NODE_ID, parentReference.getId())
                        .outE(EdgePropertyNames.CHILD_EDGE_LB)
                        .as(StepLabel.EDGE)
                        .inV()
                        .has(VertexPropertyNames.NODE_ID, memberNode.getNodeId())
                        .select(StepLabel.EDGE);

                //this is like from parent->child edge by traversing from parent to all child groups and recursively looking for user, that will give us the edge connected to parent and from there properties should contain edge.
                List<NodeEdge> edges = gremlinConnector.getEdge(traversalEdge);

                if(edges.size()>0)
                    parentReference.setRole(edges.get(0).getRole());
                else
                    parentReference.setRole("MEMBER");

            });
        }

        return ParentTreeDto.builder()
                .parentReferences(parentReferences)
                .maxDepth(calculateMaxDepth())
                .build();
    }

    @Override
    public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeIds) {
        final List<ChildrenReference> resultList = new ArrayList<>();
        Traversal<Vertex, Map<String, Object>> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId)
                .or(buildOrTraversalsByNodeIds(nodeIds))
                .outE(EdgePropertyNames.CHILD_EDGE_LB)
                .as(StepLabel.EDGE)
                .inV()
                .as(StepLabel.VERTEX)
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId)
                .select(StepLabel.EDGE, StepLabel.VERTEX);
        gremlinConnector.getVerticesAndEdges(traversal)
                .forEach(kv -> resultList.add(vertexUtilService.createChildReference(kv)));
        return resultList;
    }

    @Override
    public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
        if (node.isUser()) {
            return ChildrenTreeDto.builder().childrenUserIds(Collections.singletonList(node.getNodeId())).build();
        }
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, node.getDataPartitionId())
                .has(VertexPropertyNames.NODE_ID, node.getNodeId())
                .emit(__.hasLabel(NodeType.USER.toString()))
                .repeat(__.outE(EdgePropertyNames.CHILD_EDGE_LB).inV());
        List<NodeVertex> vertices = gremlinConnector.getVertices(traversal);
        return ChildrenTreeDto.builder()
                .childrenUserIds(vertices.stream().map(NodeVertex::getNodeId).distinct().collect(Collectors.toList())).build();
    }

    public Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences, String partitionId, String appId) {
        return parentReferences.stream()
                .filter(pr -> pr.getAppIds().isEmpty() || pr.getAppIds().contains(appId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getGroupOwners(String partitionId, String nodeId) {
        return new HashSet<>();
    }

    @Override
    public Map<String, Integer> getAssociationCount(List<String> userIds) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Integer> getAllUserPartitionAssociations() {
        return new HashMap<>();
    }

    @Override
    public ListGroupsOfPartitionDto getGroupsInPartition(String dataPartitionId, GroupType groupType, String cursor, Integer limit) {

        int offsetValue = 0;
        if (Objects.nonNull(cursor) && !cursor.isEmpty()) {
            try {
                offsetValue = Integer.parseInt(cursor);
            } catch (NumberFormatException e) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Malformed cursor, must be integer value");
            }
        }

        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, dataPartitionId)
                .hasLabel(NodeType.GROUP.toString())
                .order()
                .by(VertexPropertyNames.NAME);
        List<NodeVertex> vertices = gremlinConnector.getVertices(traversal);

        List<ParentReference> parentReferencesByGroupType = vertices.stream()
                .map(vertexUtilService::createParentReference)
                .filter(group -> groupType.equals(GroupType.NONE) || group.isMatchGroupType(groupType))
                .toList();
        List<ParentReference> parentReferencesByLimit = parentReferencesByGroupType.stream()
                .skip(offsetValue)
                .limit(limit)
                .toList();

        int totalCount = parentReferencesByGroupType.size();
        int cursorValue = totalCount > (offsetValue + limit) ? (offsetValue + limit) : 0;

        return ListGroupsOfPartitionDto.builder()
                .groups(parentReferencesByLimit)
                .cursor(String.valueOf(cursorValue))
                .totalCount((long) totalCount)
                .build();
    }

    @Override
    public MembersCountResponseDto getMembersCount(String partitionId, String groupId, Role role) {
        GraphTraversal<Vertex, Edge> vertexEdgeGraphTraversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, partitionId)
                .and(buildOrTraversalsByNodeIds(groupId))
                .outE(EdgePropertyNames.CHILD_EDGE_LB)
                .as(StepLabel.EDGE);

        if (role != null)
            vertexEdgeGraphTraversal = vertexEdgeGraphTraversal.has(EdgePropertyNames.ROLE, role.toString());

        Traversal<Vertex, Map<String, Object>> traversal = vertexEdgeGraphTraversal.inV()
                .as(StepLabel.VERTEX).select(StepLabel.EDGE, StepLabel.VERTEX);

        return MembersCountResponseDto
                .builder()
                .membersCount(gremlinConnector.getVerticesAndEdges(traversal).size())
                .groupEmail(groupId)
                .build();
    }

    private int calculateMaxDepth() {
        // TODO: 584695 Implement this method
        return 0;
    }

    private Traversal<?, ?>[] buildOrTraversalsByNodeIds(String... nodeIds) {
        return Arrays.stream(nodeIds)
                .map(id -> __.has(VertexPropertyNames.NODE_ID, id))
                .toArray(Traversal[]::new);
    }
}
