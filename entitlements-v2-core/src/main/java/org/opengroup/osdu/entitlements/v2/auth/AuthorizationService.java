package org.opengroup.osdu.entitlements.v2.auth;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;

public interface AuthorizationService {
    boolean isCurrentUserAuthorized(DpsHeaders headers, String... roles);

    boolean isGivenUserAuthorized(String userId, String partitionId, String... roles);
}
