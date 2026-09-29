package org.opengroup.osdu.entitlements.v2.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.opengroup.osdu.azure.util.AzureServicePrincipal;
import org.opengroup.osdu.entitlements.v2.acceptance.model.Token;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AzureTokenService implements TokenService {
    private static final String ACCESS_TOKEN =
            System.getProperty("INTEGRATION_TESTER_ACCESS_TOKEN",
                    System.getenv("INTEGRATION_TESTER_ACCESS_TOKEN"));
    private static final String CLIENT_ID =
            System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
    private static final String CLIENT_SECRET =
            System.getProperty("AZURE_TESTER_SERVICEPRINCIPAL_SECRET",
                    System.getenv("AZURE_TESTER_SERVICEPRINCIPAL_SECRET"));
    private static final String TENANT_ID =
            System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
    private static final String APP_RESOURCE_ID =
            System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
    private static Token TOKEN;

    /**
     * Returns the supplied bearer when one is set, otherwise a service principal's token
     */
    @Override
    public synchronized Token getToken() {
        if (TOKEN == null) {
            TOKEN = Strings.isNullOrEmpty(ACCESS_TOKEN)
                    ? Token.builder().value(retrieveToken()).userId(CLIENT_ID).build()
                    : Token.builder().value(ACCESS_TOKEN).userId(callerId(ACCESS_TOKEN)).build();
        }
        return TOKEN;
    }

    // Must match the x-user-id the gateway projects from the same claims.
    static String callerId(String bearer) {
        JsonNode claims;
        try {
            String[] parts = bearer.split("\\.");
            claims = new ObjectMapper().readTree(
                    new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("INTEGRATION_TESTER_ACCESS_TOKEN is not a JWT", e);
        }
        boolean v2 = "2.0".equals(claims.path("ver").asText());
        if (claims.hasNonNull("unique_name")) {
            return claims.get("unique_name").asText();
        }
        if (v2 && claims.hasNonNull("oid")) {
            return claims.get("oid").asText();
        }
        if (v2 && claims.hasNonNull("azp")) {
            return claims.get("azp").asText();
        }
        if (!v2 && claims.hasNonNull("oid") && claims.hasNonNull("appid")) {
            return claims.get("appid").asText();
        }
        if (!v2 && claims.hasNonNull("upn")) {
            return claims.get("upn").asText();
        }
        throw new IllegalStateException("INTEGRATION_TESTER_ACCESS_TOKEN names no caller");
    }

    private String retrieveToken() {
        try {
            return new AzureServicePrincipal().getIdToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID, APP_RESOURCE_ID);
        } catch (Exception e) {
            throw new RuntimeException("Cannot retrieve token", e);
        }
    }
}
