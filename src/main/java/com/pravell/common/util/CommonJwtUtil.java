package com.pravell.common.util;

import com.pravell.common.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommonJwtUtil {

    private final SecretKey secretKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;

    public CommonJwtUtil(
            @Value("${jwt.secret-key}") String base64Secret,
            @Value("${jwt.access-token-expiration}") Duration accessTtl,
            @Value("${jwt.refresh-token-expiration}") Duration refreshTtl,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.issuer = issuer;
    }

    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            return "refresh".equals(claims.get("typ", String.class))
                    && issuer.equals(claims.getIssuer());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
        }

        String accessToken = authorizationHeader.replace("Bearer ", "").trim();

        if (!isValidAccessToken(accessToken)) {
            throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
        }

        Claims claims = getClaims(accessToken);
        return UUID.fromString(claims.get("userId", String.class));
    }

    private boolean isValidAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = getClaims(token);
            return "access".equals(claims.get("typ", String.class))
                    && issuer.equals(claims.getIssuer());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
