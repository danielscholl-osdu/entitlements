package org.opengroup.osdu.entitlements.v2.spi.deletegroup;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;

import java.util.Deque;
import java.util.Set;

public interface DeleteGroupRepo {
    /**
     * In case of unexpected error all changes made are reverted.
     * <p>
     * Returns impacted users, they should be explicitly processed to refresh the cache.
     * For null-safe, never returns null, instead of it returns an empty set.
     */
    Set<String> deleteGroup(EntityNode groupNode);

    /**
     * In case of unexpected error all changes made are not reverted.
     * <p>
     * Returns impacted users, they should be explicitly processed to refresh the cache.
     * For null-safe, never returns null, instead of it returns an empty set.
     */
    Set<String> deleteGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode);
}
