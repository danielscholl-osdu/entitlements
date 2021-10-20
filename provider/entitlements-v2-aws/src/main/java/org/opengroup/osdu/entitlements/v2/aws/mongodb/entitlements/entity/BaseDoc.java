// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity;


import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.springframework.data.annotation.Id;

import java.util.HashSet;
import java.util.Set;

public class BaseDoc {

    @Id
    private IdDoc id;

    private Set<NodeRelationDoc> directParents = new HashSet<>();

    public IdDoc getId() {
        return id;
    }

    public void setId(IdDoc id) {
        this.id = id;
    }

    public Set<NodeRelationDoc> getDirectParents() {
        return directParents;
    }

    public void setDirectParents(Set<NodeRelationDoc> directParents) {
        this.directParents = directParents;
    }
}
