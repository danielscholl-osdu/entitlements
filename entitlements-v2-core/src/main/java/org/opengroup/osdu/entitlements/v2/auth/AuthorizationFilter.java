package org.opengroup.osdu.entitlements.v2.auth;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationFilter {
    @Autowired
    private AuthorizationService authService;
    @Autowired
    private RequestInfo requestInfo;
    @Autowired
    private JaxRsDpsLog log;
    @Autowired
    private RequestInfoUtilService requestInfoUtilService;

    public boolean hasAnyPermission(String... requiredRoles) {
        log.info(String.format("authorizeAny timestamp: %d", System.currentTimeMillis()));
        DpsHeaders headers = requestInfo.getHeaders();
        if (StringUtils.isBlank(headers.getAuthorization())) {
            throw AppException.createUnauthorized("No credentials sent on request.");
        }
        String user = requestInfoUtilService.getUserId(headers);
        requestInfo.getHeaders().put(DpsHeaders.USER_EMAIL, user);
        if (user.equalsIgnoreCase(requestInfo.getTenantInfo().getServiceAccount())) {
            return true;
        }
        if (requiredRoles.length <= 0) {
            throw AppException.createUnauthorized("The user is not authorized to perform this action");
        }
        return authService.isAuthorized(headers, requiredRoles);
    }
}
