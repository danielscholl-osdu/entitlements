package org.opengroup.osdu.entitlements.v2.acceptance;

import com.google.gson.Gson;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ErrorResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListMemberResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.util.HashMap;
import java.util.Map;

public abstract class AddMemberTest extends AcceptanceBaseTest {

    public AddMemberTest(ConfigurationService configurationService, TokenService tokenService) {
        super(configurationService, tokenService);
    }

    @Test
    public void shouldAddMemberSuccessfully() throws Exception {
        String groupName = "groupName-" + currentTime;
        String childGroupName = "child-groupName-" + currentTime;
        String memberEmail = this.configurationService.getMemberMailId();
        String ownerMemberEmail = this.configurationService.getOwnerMailId();
        Token token = tokenService.getToken();

        GroupItem groupItem = entitlementsV2Service.createGroup(groupName, token.getValue());

        AddMemberRequestData addOwnerMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("OWNER").memberEmail(ownerMemberEmail).build();
        entitlementsV2Service.addMember(addOwnerMemberRequestData, token.getValue());

        GroupItem childGroupItem = entitlementsV2Service.createGroup(childGroupName, token.getValue());
        AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(childGroupItem.getEmail()).build();
        entitlementsV2Service.addMember(addGroupMemberRequestData, token.getValue());

        AddMemberRequestData addMemberRequestData = AddMemberRequestData.builder()
                .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(memberEmail).build();
        entitlementsV2Service.addMember(addMemberRequestData, token.getValue());
        verifyConflictError(addMemberRequestData, token.getValue());

        ListMemberResponse listMemberResponse = entitlementsV2Service.getMembers(groupItem.getEmail(), token.getValue());

        Assert.assertEquals(4, listMemberResponse.getMembers().size());
        verifyMemberInResponse(listMemberResponse, "MEMBER", childGroupItem.getEmail());
        verifyMemberInResponse(listMemberResponse, "MEMBER", memberEmail.toLowerCase());
        verifyMemberInResponse(listMemberResponse, "OWNER", ownerMemberEmail.toLowerCase());
        verifyMemberInResponse(listMemberResponse, "OWNER", token.getUserId());
    }

    @Override
    protected void cleanup() throws Exception {
        Token token = tokenService.getToken();
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("child-groupName-" + currentTime), token.getValue());
        entitlementsV2Service.deleteGroup(configurationService.getIdOfGroup("groupName-" + currentTime), token.getValue());
//        TODO add revoke member logic for memberEmail and ownerMemberEmail
    }

    @Override
    protected RequestData getRequestDataForNoTokenTest() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", "MEMBER");
        requestBody.put("email", this.configurationService.getMemberMailId());
        return RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", configurationService.getIdOfGroup("users")))
                .body(new Gson().toJson(requestBody))
                .build();
    }

    private void verifyMemberInResponse(ListMemberResponse listMemberResponse, String role, String memberEmail) {
        boolean isMemberCreated = listMemberResponse.getMembers().stream()
                .filter(memberItem -> memberEmail.equals(memberItem.getEmail()))
                .anyMatch(memberItem -> memberItem.getRole().equals(role));
        Assert.assertTrue(isMemberCreated);
    }

    private void verifyConflictError(AddMemberRequestData addMemberRequestData, String token) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", addMemberRequestData.getRole().toUpperCase());
        requestBody.put("email", addMemberRequestData.getMemberEmail());
        RequestData requestData = RequestData.builder()
                .method("POST").dataPartitionId(configurationService.getTenantId())
                .relativePath(String.format("groups/%s/members", addMemberRequestData.getGroupEmail()))
                .token(token)
                .body(new Gson().toJson(requestBody)).build();
        CloseableHttpResponse response = httpClientService.send(requestData);
        Assert.assertEquals(409, response.getCode());
        String errorMessage = String.format("%s is already a member of group %s",
                addMemberRequestData.getMemberEmail().toLowerCase(), addMemberRequestData.getGroupEmail());
        ErrorResponse expectedConflictResponse = ErrorResponse.builder().code(409).reason("Conflict")
                .message(errorMessage).build();
        Assert.assertEquals(expectedConflictResponse, new Gson().fromJson(EntityUtils.toString(response.getEntity()), ErrorResponse.class));
    }
}
