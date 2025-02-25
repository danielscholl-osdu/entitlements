/*
 *  Copyright 2024 Google LLC
 *  Copyright 2024 EPAM Systems, Inc
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
package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.updateappids;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataGroupNode;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup.RetrieveGroupRepoJdbc;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@RunWith(SpringRunner.class)
public class UpdateAppIdsJdbcTest {

    @MockBean
    private AuditLogger auditLogger;

    @Autowired
    private UpdateAppIdsRepoJdbc sut;

    @MockBean
    private GroupRepository groupRepository;
    @MockBean
    private JdbcTemplateRunner jdbcTemplateRunner;
    @MockBean
    private MemberRepository memberRepository;
    @MockBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private RetrieveGroupRepoJdbc retrieveGroupRepoJdbc;

    @Test
    public void should_updateAppIds() {
        EntityNode groupNode = getDataGroupNode("x");
        groupNode.setAppIds(new HashSet<>(Arrays.asList("app1", "app2")));

        GroupInfoEntity savedGroup = GroupInfoEntity.fromEntityNode(groupNode);
        savedGroup.setId(1L);

        when(groupRepository.findByEmail(any()))
            .thenReturn(List.of(savedGroup));

        //when
        sut.updateAppIds(groupNode, new HashSet<>(Arrays.asList("app1", "app2")));

        //then
        verify(groupRepository).updateAppId(1L, "app1");
        verify(groupRepository).updateAppId(1L, "app2");

        Set<ParentReference> groups = Set.of(savedGroup.toParentReference());
        Set<ParentReference> noAppIdParents = retrieveGroupRepoJdbc.filterParentsByAppId(groups,
                DATA_PARTITION_ID, "no-app-id");
        Set<ParentReference> app1Parents = retrieveGroupRepoJdbc.filterParentsByAppId(groups,
                DATA_PARTITION_ID, "app1");
        Set<ParentReference> app2Parents = retrieveGroupRepoJdbc.filterParentsByAppId(groups,
                DATA_PARTITION_ID, "app2");

        assertTrue(noAppIdParents.isEmpty());
        assertTrue(app1Parents.stream()
                .map(ParentReference::getId)
                .toList()
                .contains(groupNode.getNodeId()));
        assertTrue(app2Parents.stream()
                .map(ParentReference::getId)
                .toList()
                .contains(groupNode.getNodeId()));
    }

}
