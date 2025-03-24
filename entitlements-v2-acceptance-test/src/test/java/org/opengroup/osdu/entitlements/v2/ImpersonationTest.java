/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.model.MemberItem;
import org.opengroup.osdu.entitlements.v2.model.request.AddMemberRequestData;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.util.*;

@Slf4j
public class ImpersonationTest extends AcceptanceBaseTest {

  private final Gson gson = new Gson();
  private final PartitionService partitionService;

  public ImpersonationTest() {
    super(new CommonConfigurationService());
    this.partitionService = new org.opengroup.osdu.entitlements.v2.util.PartitionService(new CommonConfigurationService());
  }

  @BeforeEach
  @Override
  public void setupTest() throws Exception {
    this.testUtils = new TokenTestUtils();
  }

  @AfterEach
  @Override
  public void tearTestDown() throws Exception {
    this.testUtils = null;
  }

  @Test
  public void shouldGetOkImpersonatedGet() throws Exception {
    String memberEmail = "impersonatetestmember@test.com";
    String impersonationGroup =
        "users.datalake.impersonation@" + configurationService.getTenantId() + "."
            + configurationService.getDomain();
    String dataGroup =
        "users@" + configurationService.getTenantId() + "." + configurationService.getDomain();
    String userGroup = "service.entitlements.user@" + configurationService.getTenantId() + "."
        + configurationService.getDomain();

    //check if member in data group, add if not
    List<String> members = entitlementsV2Service.getMembers(dataGroup, testUtils.getToken()).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(dataGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, testUtils.getToken());
    }

    //check if member in impersonation group, add if not
    members = entitlementsV2Service.getMembers(impersonationGroup, testUtils.getToken()).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(impersonationGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, testUtils.getToken());
    }

    //check if member in user group, add if not
    members = entitlementsV2Service.getMembers(userGroup, testUtils.getToken()).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(userGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, testUtils.getToken());
    }

    //create control random group and add member
    String groupName = "groupname-" + System.currentTimeMillis();
    GroupItem groupItem = entitlementsV2Service.createGroup(groupName, testUtils.getToken());
    AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
        .groupEmail(groupItem.getEmail()).role("MEMBER").memberEmail(memberEmail).build();
    entitlementsV2Service.addMember(addGroupMemberRequestData, testUtils.getToken());

    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", memberEmail, testUtils.getToken());

    assertEquals(200, response.getCode());
    String getGroupsResponseBody = new String(response.getEntity().getContent().readAllBytes());
    ListGroupResponse listGroupResponse = gson.fromJson(getGroupsResponseBody,
        ListGroupResponse.class);
    Optional<String> delegationGroup = listGroupResponse.getGroups().stream()
        .map(GroupItem::getName).filter(groupName::equalsIgnoreCase).findFirst();
    assertEquals(memberEmail, listGroupResponse.getMemberEmail());
    assertEquals(memberEmail, listGroupResponse.getDesId());
    assertTrue(delegationGroup.isPresent());
    assertEquals(groupName, delegationGroup.get());

    entitlementsV2Service.removeMember(groupItem.getEmail(), memberEmail, testUtils.getToken());
    entitlementsV2Service.deleteGroup(groupItem.getEmail(), testUtils.getToken());
  }

  @Test
  public void shouldFailIfNoImpersonationGroup() throws Exception {
    String memberEmail = "impersonationtestmemberwithoutgroup@test.com";
    String dataGroup =
        "users@" + configurationService.getTenantId() + "." + configurationService.getDomain();
    String impersonationGroup =
        "users.datalake.impersonation@" + configurationService.getTenantId() + "."
            + configurationService.getDomain();

    //check if member in data group, add if not
    List<String> members = entitlementsV2Service.getMembers(dataGroup, testUtils.getToken()).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (!members.contains(memberEmail)) {
      AddMemberRequestData addGroupMemberRequestData = AddMemberRequestData.builder()
          .groupEmail(dataGroup).role("MEMBER").memberEmail(memberEmail).build();
      entitlementsV2Service.addMember(addGroupMemberRequestData, testUtils.getToken());
    }

    //check if member in impersonation group, remove if yes
    members = entitlementsV2Service.getMembers(impersonationGroup, testUtils.getToken()).getMembers().stream()
        .map(MemberItem::getEmail).collect(Collectors.toList());
    if (members.contains(memberEmail)) {
      entitlementsV2Service.removeMember(impersonationGroup, memberEmail, testUtils.getToken());
    }

    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", memberEmail, testUtils.getToken());

    assertEquals(403, response.getCode());
  }

  @Test
  public void shouldFailIfTryToImpersonateTenantServiceAccount() throws Exception {
    String tenantServiceAccount = partitionService.getPartitionProperty("serviceAccount");
    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", tenantServiceAccount, testUtils.getToken());

    assertEquals(403, response.getCode());
  }

  @Test
  public void shouldFailIfNoDelegationGroup() throws Exception {

    CloseableHttpResponse response = sendWithOnBehalfOfHeader("GET", "no.matter@user.id",
            testUtils.getNoDataAccessToken());

    assertEquals(403, response.getCode());
  }

  @Override
  protected RequestData getRequestDataForNoTokenTest() {
    return RequestData.builder()
        .method("GET").dataPartitionId(configurationService.getTenantId())
        .relativePath("groups")
        .build();
  }

  private CloseableHttpResponse sendWithOnBehalfOfHeader(String method, String onBehalfOf,
      String token)
      throws Exception {
    String resourceUrl = new URL(configurationService.getServiceUrl() + "groups").toString();
    log.info("Sending request to URL: {}", resourceUrl);

    RequestData requestData = RequestData.builder()
        .relativePath("groups")
        .method(method)
        .dataPartitionId(configurationService.getTenantId())
        .token(token)
        .additionalHeaders(Collections.singletonMap("on-behalf-of", onBehalfOf))
        .build();

    return httpClientService.send(requestData);
  }

}
