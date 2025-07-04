/*
 *  Copyright 2020-2021 Google LLC
 *  Copyright 2020-2021 EPAM Systems, Inc
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

package org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.impl;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntConfigProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.OpenIdProviderProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.IAuthenticator;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.userinfo.IUserInfoProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression(value = "'${authentication-mode}' == 'INTERNAL' || '${authentication-mode}' == 'ISTIO'")
@RequiredArgsConstructor
public class InternalAuthenticator implements IAuthenticator {

    private final DpsHeaders dpsHeaders;
    private final EntConfigProperties properties;
    private final IUserInfoProvider userInfoProvider;
    private final OpenIdProviderProperties openIdProperties;

    @Override
    public boolean requestIsAuthenticated(HttpServletRequest request) {
        Optional<String> authorization = Optional.ofNullable(request.getHeader(DpsHeaders.AUTHORIZATION));
        Optional<String> userIdentity = Optional.ofNullable(request.getHeader(properties.getUserIdentityHeaderName()));
        if (!authorization.isPresent()) {
            log.warn("Request unauthenticated, token is required.");
            return false;
        }
        String userId = userInfoProvider.getUserInfoFromToken(authorization.get())
            .getStringClaim(openIdProperties.getUserIdClaimName());
        dpsHeaders.put(DpsHeaders.USER_ID, userId);
        if (userIdentity.isPresent()) {
            log.warn("Both, token and header {} are present, verify they are related to the same email.",
                properties.getUserIdentityHeaderName());
            return userIdentity.map(userId::equals).orElse(false);
        }
        return true;
    }
}
