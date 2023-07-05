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

package org.opengroup.osdu.entitlements.v2.api;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupItem;
import org.opengroup.osdu.entitlements.v2.acceptance.model.GroupType;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.GetGroupsRequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.model.response.ListGroupResponse;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.EntitlementsV2Service;
import org.opengroup.osdu.entitlements.v2.acceptance.util.HttpClientService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;
import org.opengroup.osdu.entitlements.v2.util.AnthosConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.OpenIDTokenProvider;

@Slf4j
public class GetDataGroupsIndexerServiceAccJdbcTest {

  private final String baseUrl;
  private final Client client;
  private final TokenService tokenService =  new OpenIDTokenProvider();
  private final ConfigurationService configurationService = new AnthosConfigurationService();
  private final EntitlementsV2Service entitlementsV2Service;
  private final String partitionServiceAccountEmail;
  private final Gson gson = new Gson();

  public GetDataGroupsIndexerServiceAccJdbcTest() {
    this.baseUrl = configurationService.getServiceUrl();
    this.client = getClient();
    this.entitlementsV2Service = new EntitlementsV2Service(configurationService, new HttpClientService(configurationService));
    this.partitionServiceAccountEmail = System.getProperty("INDEXER_SERVICE_ACCOUNT_EMAIL", System.getenv("INDEXER_SERVICE_ACCOUNT_EMAIL"));
  }

  @Test
  public void shouldReturnCreatedDataGroupForIndexerServiceAcc() throws Exception {
    String dataGroupName = "data.indexer.test.group";
    String dataGroupEmail = dataGroupName + "@" + configurationService.getTenantId() + "." + configurationService.getDomain();
    String token = tokenService.getToken().getValue();

    if (isNotDataGroupExist(dataGroupEmail, token)) {
      entitlementsV2Service.createGroup(dataGroupName, token);
    }

    ClientResponse successfulResponse = sendGetParentGroupsRequest(token, partitionServiceAccountEmail);
    Assert.assertEquals(200, successfulResponse.getStatus());
    String successfulResponseBody = successfulResponse.getEntity(String.class);
    ListGroupResponse successfulGroupResponse = gson.fromJson(successfulResponseBody, ListGroupResponse.class);
    Assert.assertTrue(successfulGroupResponse.getGroups().stream().map(GroupItem::getEmail)
        .anyMatch(email1 -> email1.equals(dataGroupEmail)));

    entitlementsV2Service.deleteGroup(dataGroupEmail, token);
    ClientResponse notFoundResponse = sendGetParentGroupsRequest(token, partitionServiceAccountEmail);
    Assert.assertEquals(200, notFoundResponse.getStatus());
    String notFoundResponseBody = notFoundResponse.getEntity(String.class);
    ListGroupResponse notFoundGroupResponse = gson.fromJson(notFoundResponseBody, ListGroupResponse.class);
    Assert.assertFalse(notFoundGroupResponse.getGroups().stream().map(GroupItem::getEmail)
        .noneMatch(email -> email.equals(dataGroupEmail)));
  }

  private boolean isNotDataGroupExist(String dataGroupEmail, String token) throws MalformedURLException {
    ClientResponse response = sendGetGroupsRequest(token);
    if (response == null || response.getStatus() != 200) {
      Assert.fail("Get groups request failed");
    }
    String body = response.getEntity(String.class);
    ListGroupResponse groupResponse = gson.fromJson(body, ListGroupResponse.class);
    return groupResponse.getGroups().stream().noneMatch(group -> group.getEmail().equals(dataGroupEmail));
  }

  private ClientResponse sendGetGroupsRequest(String token) throws MalformedURLException {
    String resourceUrl = new URL(baseUrl + "groups").toString();
    log.info("Sending request to URL: {}", resourceUrl);
    WebResource webResource = client.resource(resourceUrl);
    return webResource.getRequestBuilder()
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .header("data-partition-id", configurationService.getTenantId())
        .method("GET", ClientResponse.class);
  }

  private ClientResponse sendGetParentGroupsRequest(String token, String partitionServiceAccountEmail)
      throws MalformedURLException {
    String resourceUrl = new URL(baseUrl + "members/" + partitionServiceAccountEmail + "/groups/?type=data").toString();
    log.info("Sending request to URL: {}", resourceUrl);
    WebResource webResource = client.resource(resourceUrl);
    return webResource.getRequestBuilder()
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .header("data-partition-id", configurationService.getTenantId())
        .method("GET", ClientResponse.class);
  }

  private Client getClient() {
    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    }};
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {/*do nothing*/}
    return Client.create();
  }
}
