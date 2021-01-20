package org.opengroup.osdu.entitlements.v2.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class JwtClaimExtractor {

    /**
     * We currently category SAuth and Google token as OTHER, since they have the same set of claims we care about
     */
    protected enum TokenType {
        AAD,
        OTHER
    }

    private static final String BEARER = "Bearer ";
    private static final String AZURE_TOKEN_ISS_PREFIX = "https://sts.windows.net/";
    private static final String EMAIL_CLAIM = "email";
    private static final String AUD_CLAIM = "aud";
    private static final String UPN_CLAIM = "upn";
    private static final String UNIQUE_NAME_CLAIM = "unique_name";
    private static final String APP_ID_CLAIM = "appid";

    /**
     * extract the necessary claims from different types of tokens
     *
     * @param jwt
     * @return
     */
    public JwtClaims extract(String jwt) {
        if (Strings.isNullOrEmpty(jwt)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Authorization value not provided in expected format.");
        }
        jwt = jwt.replaceFirst(BEARER, "").trim();
        try {
            DecodedJWT decodedJwt = JWT.decode(jwt);
            TokenType tokenType = getTokenType(decodedJwt);
            if (tokenType == TokenType.AAD) {
                return extractJwtClaimsAAD(decodedJwt);
            } else if (tokenType == TokenType.OTHER) {
                String email = decodedJwt.getClaim(EMAIL_CLAIM).asString();
                if (Strings.isNullOrEmpty(email)) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Missing email claim on provided jwt.");
                }
                return new JwtClaims(email, decodedJwt.getClaim(AUD_CLAIM).asString());
            }
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Token type is not supported");
        } catch (JWTDecodeException exception) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Authorization value not provided in expected format.");
        }
    }

    private TokenType getTokenType(DecodedJWT decodedJWT) {
        String iss = decodedJWT.getIssuer();
        if (Strings.isNullOrEmpty(iss)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Invalid token.");
        }
        if (iss.startsWith(AZURE_TOKEN_ISS_PREFIX)) {
            return TokenType.AAD;
        }
        return TokenType.OTHER;
    }

    private JwtClaims extractJwtClaimsAAD(DecodedJWT decodedJWT) {
        Map<String, Claim> claims = decodedJWT.getClaims();
        if (claims.containsKey(UPN_CLAIM)) {
            return new JwtClaims(claims.get(UPN_CLAIM).asString(), claims.get(AUD_CLAIM).asString());
        } else if (claims.containsKey(UNIQUE_NAME_CLAIM)) {
            return new JwtClaims(claims.get(UNIQUE_NAME_CLAIM).asString(), claims.get(AUD_CLAIM).asString());
        } else if (claims.containsKey(APP_ID_CLAIM)) {
            return new JwtClaims(claims.get(APP_ID_CLAIM).asString(), claims.get(AUD_CLAIM).asString());
        }
        throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Invalid AAD token.");
    }
}

