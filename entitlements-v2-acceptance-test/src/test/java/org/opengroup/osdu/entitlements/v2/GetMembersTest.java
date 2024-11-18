package org.opengroup.osdu.entitlements.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.Token;
import org.opengroup.osdu.entitlements.v2.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.util.CommonConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.OpenIDTokenProvider;
import org.springframework.http.HttpStatus;
import java.io.IOException;

public class GetMembersTest extends AcceptanceBaseTest {

    private final Token token = tokenService.getToken();

    public GetMembersTest() {
        super(new CommonConfigurationService(), new OpenIDTokenProvider());
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        final String groupEmail = configurationService.getIdOfGroup("group");
        return RequestData.builder()
                .method("GET").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", groupEmail))
                .build();
    }

    /**
     * 1) create group
     * 2) create child group
     * 3) add child group 1 to group as member
     * 4) add child group 2 to group as member
     * 5) add child group 3 to group as member
     * 6) List members of the group and check child group 1,2,3 are a member of group
     * 7) delete child group 1
     * 8) delete child group 2
     * 9) delete child group 3
     * 10) delete group
     */
    @Test
    public void shouldSuccessfullyListMember() throws Exception {
        String groupName = "group-" + currentTime;
        String childGroupName1 = "child-group-name-1" + currentTime;
        String childGroupName2 = "child-group-name-2" + currentTime;
        String childGroupName3 = "child-group-name-3" + currentTime;
        Token token = tokenService.getToken();
        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());
        GroupItem childGroupItem1 = entitlementsV2Service.createGroup(childGroupName1, token.getValue());
        GroupItem childGroupItem2 = entitlementsV2Service.createGroup(childGroupName2, token.getValue());
        GroupItem childGroupItem3 = entitlementsV2Service.createGroup(childGroupName3, token.getValue());

        AddMemberRequestData addMemberRequestData1 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem1.getEmail()).build();
        AddMemberRequestData addMemberRequestData2 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem2.getEmail()).build();
        AddMemberRequestData addMemberRequestData3 = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem3.getEmail()).build();
        entitlementsV2Service.addMember(addMemberRequestData1, token.getValue());
        entitlementsV2Service.addMember(addMemberRequestData2, token.getValue());
        entitlementsV2Service.addMember(addMemberRequestData3, token.getValue());

        ListMemberResponse listMemberResponse1 = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroup1AMember = listMemberResponse1.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem1.getEmail()));
        assertTrue(isChildGroup1AMember);
        ListMemberResponse listMemberResponse2 = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroup2AMember = listMemberResponse2.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem2.getEmail()));
        assertTrue(isChildGroup2AMember);
        ListMemberResponse listMemberResponse3 = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());
        boolean isChildGroup3AMember = listMemberResponse3.getMembers()
                .stream().anyMatch(memberItem -> memberItem.getEmail().equals(childGroupItem3.getEmail()));
        assertTrue(isChildGroup3AMember);
    }

    @Test
    public void shouldReturnBadRequestWhenMakingHttpRequestWithInvalidUrl() throws IOException {
        RequestData requestData = RequestData.builder()
                .method("GET")
                .relativePath("groups/%3B/members")
                .dataPartitionId(configurationService.getTenantId())
                .token(token.getValue())
                .build();

        CloseableHttpResponse closeableHttpResponse = httpClientService.send(requestData);

        assertEquals(HttpStatus.BAD_REQUEST.value(), closeableHttpResponse.getCode());
    }

    @Override
    protected void cleanup() throws Exception {
        String tokenValue = tokenService.getToken().getValue();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-1" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-2" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-group-name-3" + currentTime), tokenValue);
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("group-" + currentTime), tokenValue);
    }
}
