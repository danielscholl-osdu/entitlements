/**
* Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.aws.util.ExceptionGenerator;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.creategroup.CreateGroupRepo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;


@Component
public class CreateGroupRepoMongoDB extends BasicEntitlementsHelper implements CreateGroupRepo {

    @Override
    public Set<String> createGroup(EntityNode groupNode, CreateGroupRepoDto createGroupRequest) {

        GroupDoc groupToCreate = conversionService.convert(groupNode, GroupDoc.class);
        UserDoc userInitiator = conversionService.convert(createGroupRequest.getRequesterNode(), UserDoc.class);
        userInitiator = userHelper.getOrCreate(userInitiator);

        //check add membership under root group
        if (createGroupRequest.isAddDataRootGroup()) {
            GroupDoc rootGroup = conversionService.convert(createGroupRequest.getDataRootGroupNode(), GroupDoc.class);
            if (rootGroup == null) {
                throw ExceptionGenerator.groupIsNull();
            }
            groupToCreate.getDirectParents().add(new NodeRelationDoc(rootGroup.getId(), Role.MEMBER));
        }

        try {
            groupHelper.save(groupToCreate);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new AppException(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), "This group already exists");
        }

        NodeRelationDoc directRelationForUser = new NodeRelationDoc(groupToCreate.getId(), Role.OWNER);

        Set<NodeRelationDoc> userMemberRelations = groupHelper.getAllParentRelations(groupToCreate);
        for (NodeRelationDoc relationDoc : userMemberRelations) {
            relationDoc.setRole(Role.MEMBER);
        }
        userMemberRelations.add(directRelationForUser);

        userHelper.addDirectRelation(userInitiator.getId(), directRelationForUser);
        userHelper.addMemberRelations(userInitiator.getId(), userMemberRelations);

        //return IDS then cash will work
        return new HashSet<>();
    }

    @Override
    public Set<String> createGroup(Deque<Operation> executedCommandsDeque, EntityNode groupNode, CreateGroupRepoDto createGroupRepoDto) {
        return Collections.emptySet();
    }
}
