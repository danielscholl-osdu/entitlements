package org.opengroup.osdu.entitlements.v2.acceptance;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupType;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.GetGroupsRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public abstract class ListGroupOnBehalfOfTest extends AcceptanceBaseTest {
    private final List<String> groupsForFurtherDeletion;
    private final Token token;

    public ListGroupOnBehalfOfTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
        groupsForFurtherDeletion = new ArrayList<>();
        token = tokenService.getToken();
    }

    @Override
    protected void cleanup() throws Exception {
        for (String groupName : groupsForFurtherDeletion) {
            entitlementsV2Service.deleteGroup(groupName, token.getValue());
        }
    }

    @Test
    public void shouldReturnAllGroupsThatGivenMemberBelongsTo() throws Exception {
        String memberEmail = "testMember@test.com";

        List<GroupItem> createdGroups = setup(memberEmail);

        GetGroupsRequestData getGroupsRequestData = GetGroupsRequestData.builder()
                .memberEmail(memberEmail)
                .type(GroupType.NONE)
                .build();
        ListGroupResponse groups = entitlementsV2Service.getGroups(getGroupsRequestData, token.getValue());

        assertEquals(memberEmail.toLowerCase(), groups.getDesId());
        assertEquals(memberEmail.toLowerCase(), groups.getMemberEmail());
        List<String> foundGroups = groups.getGroups().stream()
                .filter(group -> group.getEmail().equals(createdGroups.get(0).getEmail())
                        || group.getEmail().equals(createdGroups.get(1).getEmail())
                        || group.getEmail().equals(createdGroups.get(2).getEmail()))
                .map(GroupItem::getEmail)
                .sorted(String::compareTo)
                .collect(Collectors.toList());
        assertEquals(3, foundGroups.size());
        assertEquals(createdGroups.get(0).getEmail(), foundGroups.get(0));
        assertEquals(createdGroups.get(1).getEmail(), foundGroups.get(1));
        assertEquals(createdGroups.get(2).getEmail(), foundGroups.get(2));
    }

    @Test
    public void shouldReturn400WhenGroupsTypeIsMissed() throws Exception {
        RequestData requestData = RequestData.builder()
                .method("GET")
                .relativePath("members/testMember@test.com/groups")
                .dataPartitionId(configurationService.getTenantId())
                .token(token.getValue())
                .build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        assertEquals(400, response.getCode());
    }

    @Test
    public void shouldReturn400WhenGroupsTypeIsUnknown() throws Exception {
        RequestData requestData = RequestData.builder()
                .method("GET")
                .relativePath("members/testMember@test.com/groups")
                .queryParams(Collections.singletonMap("type", "test"))
                .dataPartitionId(configurationService.getTenantId())
                .token(token.getValue())
                .build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        assertEquals(400, response.getCode());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        return RequestData.builder()
                .method("GET")
                .relativePath("members/testMember@test.com/groups")
                .queryParams(Collections.singletonMap("type", GroupType.NONE.toString()))
                .dataPartitionId(configurationService.getTenantId())
                .build();
    }

    private List<GroupItem> setup(String memberEmail) throws Exception {
        List<GroupItem> groups = new ArrayList<>();

        String group1Name = "group1-" + currentTime;
        String group2Name = "group2-" + currentTime;
        String group3Name = "group3-" + currentTime;

        GroupItem group1Item = entitlementsV2Service.createGroup(group1Name, token.getValue());
        groupsForFurtherDeletion.add(group1Item.getEmail());
        groups.add(group1Item);

        GroupItem group2Item = entitlementsV2Service.createGroup(group2Name, token.getValue());
        groupsForFurtherDeletion.add(group2Item.getEmail());
        groups.add(group2Item);

        GroupItem group3Item = entitlementsV2Service.createGroup(group3Name, token.getValue());
        groupsForFurtherDeletion.add(group3Item.getEmail());
        groups.add(group3Item);

        addMember(group1Item.getEmail(), group2Item.getEmail());
        addMember(group2Item.getEmail(), memberEmail);
        addMember(group3Item.getEmail(), memberEmail);

        return groups;
    }

    private void addMember(String groupEMail, String memberEmail) throws Exception {
        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupEMail)
                .role("MEMBER")
                .memberEmail(memberEmail)
                .build();
        entitlementsV2Service.addMember(addMemberRequestData, token.getValue());
    }
}
