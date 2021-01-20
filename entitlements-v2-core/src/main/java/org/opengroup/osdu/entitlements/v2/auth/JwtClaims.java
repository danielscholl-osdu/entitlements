package org.opengroup.osdu.entitlements.v2.auth;

import lombok.Value;

@Value
public class JwtClaims {
    String userId;
    String appId;
}
