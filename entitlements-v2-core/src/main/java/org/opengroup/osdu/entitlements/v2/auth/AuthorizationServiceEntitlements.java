package org.opengroup.osdu.entitlements.v2.auth;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupsProvider;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorizationServiceEntitlements implements AuthorizationService {
    private static final String UNAUTHORIZED_ERROR_MESSAGE = "The user is not authorized to perform this action";

    @Autowired
    private JaxRsDpsLog log;
    @Autowired
    private RequestInfoUtilService requestInfoUtilService;
    @Autowired
    private GroupsProvider groupsProvider;

    @Override
    public boolean isCurrentUserAuthorized(DpsHeaders headers, String... roles) {
        log.debug(String.format("authorizeAny timestamp: %d", System.currentTimeMillis()));
        String requesterId = requestInfoUtilService.getUserId(headers);
        List<String> groupNamesOriginalCaller = groupsProvider.getGroupsInContext(requesterId, headers.getPartitionId())
                .stream().map(ParentReference::getName).collect(Collectors.toList());
        if(!isValidGroups(groupNamesOriginalCaller, roles)){
            throw AppException.createUnauthorized(UNAUTHORIZED_ERROR_MESSAGE);
        }else {
            return requesterId != null;
        }
    }

    @Override
    public boolean isGivenUserAuthorized(String userId, String partitionId, String... roles) {
        List<String> groupNamesOriginalCaller = groupsProvider.getGroupsInContext(userId, partitionId)
            .stream()
            .map(ParentReference::getName)
            .collect(Collectors.toList());
        return isValidGroups(groupNamesOriginalCaller, roles);
    }

    private boolean isValidGroups(List<String> groupNames, String... roles) {
        if (!groupNames.contains("users")) {
            return false;
        }
        return !Collections.disjoint(groupNames, Arrays.asList(roles));
    }
}
