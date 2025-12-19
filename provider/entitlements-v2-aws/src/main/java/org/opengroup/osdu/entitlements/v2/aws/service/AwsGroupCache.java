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

package org.opengroup.osdu.entitlements.v2.aws.service;

import org.opengroup.osdu.core.aws.cache.CacheParameters;
import org.opengroup.osdu.core.aws.cache.NameSpacedCache;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AwsGroupCache {
    private final ICache<String, ParentReferences> cache;

    public AwsGroupCache(
        @Value("${aws.elasticache.cluster.endpoint:null}") String redisEndpoint,
        @Value("${aws.elasticache.cluster.port:null}") String redisPort,
        @Value("${aws.elasticache.cluster.key:null}") String redisPassword,
        @Value("${aws.group.cache.expiration.seconds:300}") int expTimeSeconds,
        @Value("${aws.group.cache.max.size:1000}") int maxSize,
        @Value("${aws.group.cache.namespace:groupCache}") String keyNamespace
    ) {
        CacheParameters<String, ParentReferences> cacheParams = CacheParameters.<String, ParentReferences>builder()
                                                                .expTimeSeconds(expTimeSeconds)
                                                                .maxSize(maxSize)
                                                                .defaultHost(redisEndpoint)
                                                                .defaultPort(redisPort)
                                                                .defaultPassword(redisPassword)
                                                                .keyNamespace(keyNamespace)
                                                                .build()
                                                                .initFromLocalParameters(String.class, ParentReferences.class);
        this.cache = new NameSpacedCache<>(cacheParams);
    }

    public ParentReferences getGroupCache(String requesterId) {
        return this.cache.get(requesterId);
    }

    public void addGroupCache(String requesterId, ParentReferences parentReferences) {
        this.cache.put(requesterId, parentReferences);
    }

    public void deleteGroupCache(String requesterId) {
        this.cache.delete(requesterId);
    }
}
