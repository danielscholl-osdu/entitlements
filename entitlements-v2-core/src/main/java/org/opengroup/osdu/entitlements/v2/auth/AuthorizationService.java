package org.opengroup.osdu.entitlements.v2.auth;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;

public interface AuthorizationService {
    boolean isAuthorized(DpsHeaders headers, String... roles);
}
