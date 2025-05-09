package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.renamegroup;

import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.constant.VertexPropertyNames;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.renamegroup.RenameGroupRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.GroupCreationUtil;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RenameGroupRepoGremlin implements RenameGroupRepo {

    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GremlinConnector gremlinConnector;
    private final AuditLogger auditLogger;

    @Override
    public Set<String> run(EntityNode groupNode, String newGroupName) {
        Set<String> impactedUsers;
        try {
            impactedUsers = executeRenameGroupOperation(groupNode, newGroupName);
            auditLogger.updateGroup(AuditStatus.SUCCESS, groupNode.getNodeId());
            return impactedUsers;
        } catch (Exception e) {
            auditLogger.updateGroup(AuditStatus.FAILURE, groupNode.getNodeId());
            throw e;
        }
    }

    private Set<String> executeRenameGroupOperation(EntityNode groupNode, String newGroupName) {
        List<String> impactedUsers = new ArrayList<>();
        impactedUsers.addAll(retrieveGroupRepo.loadAllChildrenUsers(groupNode).getChildrenUserIds());
        String partitionDomain = groupNode.getNodeId().split("@")[1];
        String newNodeId = GroupCreationUtil.createGroupEmail(newGroupName, partitionDomain);
        Traversal<Vertex, Vertex> traversal = gremlinConnector.getGraphTraversalSource().V()
                .has(VertexPropertyNames.DATA_PARTITION_ID, groupNode.getDataPartitionId())
                .has(VertexPropertyNames.NODE_ID, groupNode.getNodeId())
                .property(VertexProperty.Cardinality.single, VertexPropertyNames.NODE_ID, newNodeId)
                .property(VertexProperty.Cardinality.single, VertexPropertyNames.NAME, newGroupName);
        gremlinConnector.updateVertex(traversal);
        return new HashSet<>(impactedUsers);
    }
}
