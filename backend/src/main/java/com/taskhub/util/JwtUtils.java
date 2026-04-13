package com.taskhub.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${task-hub.jwt.secret:TaskHubSecretKey2026PhyQxxTeamTaskTracker}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String memberId, String nickname, String role) {
        return Jwts.builder()
                .subject(memberId)
                .claims(Map.of(
                        "nickname", nickname,
                        "role", role != null ? role : "member"
                ))
                .issuedAt(new Date())
                .signWith(getSigningKey())
                .compact();
    }

    public String getMemberIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getNicknameFromToken(String token) {
        return parseClaims(token).get("nickname", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
