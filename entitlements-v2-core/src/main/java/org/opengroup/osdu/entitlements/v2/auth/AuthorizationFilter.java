package org.opengroup.osdu.entitlements.v2.auth;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
        log.debug(String.format("authorizeAny timestamp: %d", System.currentTimeMillis()));
        DpsHeaders headers = requestInfo.getHeaders();
        if (StringUtils.isBlank(headers.getAuthorization())) {
            throw AppException.createUnauthorized("No credentials sent on request.");
        }
        String user = requestInfoUtilService.getUserId(headers);
        if (user == null ){
            throw AppException.createUnauthorized("No User Id header provided");
        }
        requestInfo.getHeaders().put(DpsHeaders.USER_EMAIL, user);
        TenantInfo tenantInfo = requestInfo.getTenantInfo();
        if (tenantInfo == null) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Invalid data partition id");
        }
        if (user.equalsIgnoreCase(tenantInfo.getServiceAccount())) {
            return true;
        }
        if (requiredRoles.length <= 0) {
            throw AppException.createUnauthorized("The user is not authorized to perform this action");
        }
        return authService.isCurrentUserAuthorized(headers, requiredRoles);
    }

    public boolean requesterHasImpersonationPermission(String role) {
        DpsHeaders headers = requestInfo.getHeaders();
        String userId = requestInfoUtilService.getUserId(headers);
        requestInfo.getTenantInfo();
        if (!authService.isGivenUserAuthorized(userId, headers.getPartitionId(), role)) {
            log.error("Delegation group not found");
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Impersonation not allowed for " + userId);
        } else {
            return true;
        }
    }

    public boolean targetCanBeImpersonated(String role) {
        DpsHeaders headers = requestInfo.getHeaders();
        TenantInfo tenantInfo = requestInfo.getTenantInfo();
        String impersonationTarget = requestInfoUtilService.getImpersonationTarget(headers);
        if (impersonationTarget.equalsIgnoreCase(tenantInfo.getServiceAccount())) {
            log.error("Impersonation attempt of a tenant service account.");
            throw AppException.createForbidden(
                "Tenant service account impersonation is not allowed.");
        }
        if (!authService.isGivenUserAuthorized(impersonationTarget, headers.getPartitionId(), role)) {
            log.error("Impersonation group not found");
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Impersonation not allowed for " + impersonationTarget);
        } else {
            return true;
        }
    }
}
