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

package org.opengroup.osdu.entitlements.v2.jdbc.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Data
public class EntConfigProperties {

    private String authenticationMode;
    private String userIdentityHeaderName;

    private String redisUserInfoHost;
    private Integer redisUserInfoPort;
    private String redisUserInfoPassword;
    private Integer redisUserInfoExpiration = 30;
    private Boolean redisUserInfoWithSsl = false;

    private String redisUserGroupsHost;
    private Integer redisUserGroupsPort;
    private String redisUserGroupsPassword;
    private Integer redisUserGroupsExpiration = 30;
    private Boolean redisUserGroupsWithSsl = false;

    private int partitionInfoVmCacheExpTime = 60;
    private int partitionInfoVmCacheSize = 100;

    private String systemTenant;
    private String partitionPropertiesPrefix;
}
