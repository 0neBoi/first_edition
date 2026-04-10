package com.studyhelper.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMs;

    public JwtUtil(
            @Value("${jwt.secret:study-helper-jwt-secret}") String secret,
            @Value("${jwt.expire-hours:168}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireHours * 60 * 60 * 1000L;
    }

    public String createToken(Long userId, String username) {
        return createToken(userId, username, null);
    }

    public String createToken(Long userId, String username, String role) {
        String r = (role == null || role.isBlank()) ? "STUDENT" : role.trim().toUpperCase();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", r)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(key)
                .compact();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object r = claims.get("role");
        return r != null ? r.toString().toUpperCase() : "STUDENT";
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
