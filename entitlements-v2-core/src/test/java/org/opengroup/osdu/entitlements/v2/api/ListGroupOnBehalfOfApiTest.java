package org.opengroup.osdu.entitlements.v2.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupOnBehalfOfServiceDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.service.ListGroupOnBehalfOfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ListGroupOnBehalfOfApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class ListGroupOnBehalfOfApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ListGroupOnBehalfOfService service; //service used by api controller
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void setup() {
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("dp");
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.getTenantInfo("dp")).thenReturn(tenantInfo);
        when(authService.isCurrentUserAuthorized(any(), any())).thenReturn(true);
    }

    @Test
    public void should_matchExpectedHttpRequest() throws Exception {
        String memberId = "member@domain.com";
        Set<ParentReference> output = new HashSet<>();
        output.add(ParentReference.builder().name("data.x").id("data.x@dp.domain.com")
                .description("a data group").dataPartitionId("dp").build());
        ListGroupResponseDto filteredGroups = new ListGroupResponseDto();
        filteredGroups.setGroups(new ArrayList<>(output));
        filteredGroups.setDesId(memberId);
        filteredGroups.setMemberEmail(memberId);
        when(service.getGroupsOnBehalfOfMember(any(ListGroupOnBehalfOfServiceDto.class))).thenReturn(filteredGroups);

        performListGroupRequest(memberId, GroupType.DATA).andExpect(status().isOk());
    }

    @Test
    public void should_SerializeOutput() throws Exception {
        String memberId = "member@domain.com";
        ParentReference g1 = ParentReference.builder().name("data.x").id("data.x@dp.domain.com").description("a data group").build();
        List<ParentReference> groups = Arrays.asList(g1);
        ListGroupResponseDto filteredGroups = new ListGroupResponseDto();
        filteredGroups.setGroups(groups);
        filteredGroups.setDesId(memberId);
        filteredGroups.setMemberEmail(memberId);
        when(service.getGroupsOnBehalfOfMember(any(ListGroupOnBehalfOfServiceDto.class))).thenReturn(filteredGroups);

        ListGroupResponseDto expectedResult = new ListGroupResponseDto();
        expectedResult.setGroups(groups);
        expectedResult.setDesId(memberId);
        expectedResult.setMemberEmail(memberId);

        String result = performListGroupRequest(memberId, GroupType.DATA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void should_callService_withExpectedInputs() throws Exception {
        String memberId = "MEMBER@domain.com";
        ArgumentCaptor<ListGroupOnBehalfOfServiceDto> captor = ArgumentCaptor.forClass(ListGroupOnBehalfOfServiceDto.class);

        performListGroupRequest(memberId, GroupType.DATA).andExpect(status().isOk());

        verify(service, times(1)).getGroupsOnBehalfOfMember(captor.capture());

        assertThat(captor.getValue().getMemberId()).isEqualTo("member@domain.com");
    }

    @Test
    public void should_return_givenGroupTypeIsData() throws Exception {
        String memberId = "member@domain.com";
        ParentReference g1 = ParentReference.builder().name("data.x").id("data.x@dp.domain.com").description("a data group").build();
        List<ParentReference> dataGroups = Arrays.asList(g1);
        ListGroupResponseDto filteredGroups = new ListGroupResponseDto();
        filteredGroups.setGroups(dataGroups);
        filteredGroups.setDesId(memberId);
        filteredGroups.setMemberEmail(memberId);
        when(service.getGroupsOnBehalfOfMember(any(ListGroupOnBehalfOfServiceDto.class))).thenReturn(filteredGroups);

        ListGroupResponseDto expectedResult = new ListGroupResponseDto();
        expectedResult.setGroups(dataGroups);
        expectedResult.setDesId(memberId);
        expectedResult.setMemberEmail(memberId);

        String result = performListGroupRequest(memberId, GroupType.DATA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void should_return_givenGroupTypeIsUser() throws Exception {
        String memberId = "member@domain.com";
        ParentReference g1 = ParentReference.builder().name("users.x").id("users.x@dp.domain.com").description("a user group").build();
        List<ParentReference> groups = Arrays.asList(g1);
        ListGroupResponseDto filteredGroups = new ListGroupResponseDto();
        filteredGroups.setGroups(groups);
        filteredGroups.setDesId(memberId);
        filteredGroups.setMemberEmail(memberId);
        when(service.getGroupsOnBehalfOfMember(any(ListGroupOnBehalfOfServiceDto.class))).thenReturn(filteredGroups);

        ListGroupResponseDto expectedResult = new ListGroupResponseDto();
        expectedResult.setGroups(groups);
        expectedResult.setDesId(memberId);
        expectedResult.setMemberEmail(memberId);

        String result = performListGroupRequest(memberId, GroupType.USER).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void should_return_givenGroupTypeIsService() throws Exception {
        String memberId = "member@domain.com";
        ParentReference g1 = ParentReference.builder().name("service.x").id("service.x@dp.domain.com").description("a service group").build();
        List<ParentReference> groups = Arrays.asList(g1);
        ListGroupResponseDto filteredGroups = new ListGroupResponseDto();
        filteredGroups.setGroups(groups);
        filteredGroups.setDesId(memberId);
        filteredGroups.setMemberEmail(memberId);
        when(service.getGroupsOnBehalfOfMember(any(ListGroupOnBehalfOfServiceDto.class))).thenReturn(filteredGroups);

        ListGroupResponseDto expectedResult = new ListGroupResponseDto();
        expectedResult.setGroups(groups);
        expectedResult.setDesId(memberId);
        expectedResult.setMemberEmail(memberId);

        String result = performListGroupRequest(memberId, GroupType.SERVICE).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void should_return_400givenGroupTypeIsServiceAndQueryParamKeyIsNotOneOfGroupTypeValues() throws Exception {

        String memberId = "member@domain.com";

        ResultActions resultActions = mockMvc.perform(get("/members/{member_email}/groups", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "dp")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .queryParam("type", "domain"))
                .andDo(print());

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_400givenGroupTypeIsServiceAndQueryParamKeyIsEmpty() throws Exception {

        String memberId = "member@domain.com";

        ResultActions resultActions = mockMvc.perform(get("/members/{member_email}/groups", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "dp")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .queryParam("type", ""))
                .andDo(print());

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void should_return400_givenGroupTypeIsServiceAndQueryParamKeyIsCaseSensitive() throws Exception {
        String memberId = "member@domain.com";

        ResultActions resultActions = mockMvc.perform(get("/members/{member_email}/groups", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "dp")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .queryParam("Type", GroupType.SERVICE.toString()))
                .andDo(print());

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_givenGroupTypeIsServiceAndQueryParamValueIsLowercase() throws Exception {
        String memberId = "member@domain.com";
        ParentReference g1 = ParentReference.builder().name("service.x").id("service.x@dp.domain.com").description("a service group").build();
        List<ParentReference> groups = Arrays.asList(g1);
        ListGroupResponseDto filteredGroups = new ListGroupResponseDto();
        filteredGroups.setGroups(groups);
        filteredGroups.setDesId(memberId);
        filteredGroups.setMemberEmail(memberId);

        when(service.getGroupsOnBehalfOfMember(any(ListGroupOnBehalfOfServiceDto.class))).thenReturn(filteredGroups);

        ListGroupResponseDto expectedResult = new ListGroupResponseDto();
        expectedResult.setGroups(groups);
        expectedResult.setDesId(memberId);
        expectedResult.setMemberEmail(memberId);

        ResultActions resultActions = mockMvc.perform(get("/members/{member_email}/groups", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "dp")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .queryParam("type", GroupType.SERVICE.toString().toLowerCase()))
                .andDo(print());

        String result = resultActions.andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    private ResultActions performListGroupRequest(String memberId, GroupType groupType) throws Exception {
        return mockMvc.perform(get("/members/{member_email}/groups", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "dp")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .queryParam("type", groupType.toString()))
                .andDo(print());
    }
}
