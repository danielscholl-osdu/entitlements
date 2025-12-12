/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.opengroup.osdu.entitlements.v2.aws.acceptance;

import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.DeleteGroupTest;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupType;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.GetGroupsRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.AwsConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.AwsTokenService;

import static org.junit.Assert.assertTrue;

public class DeleteGroupAwsTest extends DeleteGroupTest {

    public DeleteGroupAwsTest() {
        super(new AwsConfigurationService(), AwsTokenService.getInstance());
    }

    @Test
    public void testServicePrincipalCacheInvalidationOnDataGroupDelete() throws Exception {
        String servicePrincipalEmail = System.getenv("SERVICE_PRINCIPAL_EMAIL");
        String domain = System.getenv("DOMAIN");
        if (servicePrincipalEmail == null || servicePrincipalEmail.isEmpty()) {
            servicePrincipalEmail = "users.data.root@" + configurationService.getTenantId() + "." + (domain != null ? domain : "example.com");
        }

        // Use admin user token for group operations
        AwsTokenService awsTokenService = (AwsTokenService) tokenService;
        Token adminToken = awsTokenService.getAdminToken();
        Token spToken = tokenService.getToken(); // Service principal token for checking groups
        
        String groupName = "data.cache-delete-test-" + currentTime;
        String expectedGroupEmail = configurationService.getIdOfGroup(groupName);

        // Create the data group first using admin user
        GroupItem createdGroup = entitlementsV2Service.createGroup(groupName, adminToken.getValue());

        // Verify service principal can see the group
        GetGroupsRequestData getGroupsRequest = GetGroupsRequestData.builder()
                .memberEmail(servicePrincipalEmail)
                .type(GroupType.DATA)
                .build();
        ListGroupResponse groupsBefore = entitlementsV2Service.getGroups(getGroupsRequest, spToken.getValue());
        boolean groupExistsBeforeDelete = groupsBefore.getGroups().stream()
                .anyMatch(group -> group.getEmail().equals(expectedGroupEmail));

        // Delete the data group using admin user
        entitlementsV2Service.deleteGroup(expectedGroupEmail, adminToken.getValue());

        // Immediately check if service principal can no longer see the group (cache should be invalidated)
        ListGroupResponse groupsAfter = entitlementsV2Service.getGroups(getGroupsRequest, spToken.getValue());
        boolean groupExistsAfterDelete = groupsAfter.getGroups().stream()
                .anyMatch(group -> group.getEmail().equals(expectedGroupEmail));

        // Assertions
        assertTrue("Service principal should see the data group before deletion", groupExistsBeforeDelete);
        assertTrue("Service principal should immediately not see the deleted data group (cache invalidated)", !groupExistsAfterDelete);
    }
}
