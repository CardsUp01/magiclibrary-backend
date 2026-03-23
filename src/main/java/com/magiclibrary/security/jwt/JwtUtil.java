package com.magiclibrary.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${spring.app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.app.jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${spring.app.jwt.expirationRememberMe}")
    private long jwtExpirationRememberMeInMs;

    private static final ZoneId JWT_ZONE = ZoneId.of("UTC");

    public String generateToken(Integer userId, String email, String role) {
        return generateTokenInternal(userId, email, role, jwtExpirationInMs);
    }

    public String generateToken(Integer userId, String email, String role, boolean rememberMe) {
        long expiration = rememberMe ? jwtExpirationRememberMeInMs : jwtExpirationInMs;
        return generateTokenInternal(userId, email, role, expiration);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractEmail(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return extractAllClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public Integer extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return extractAllClaims(token).get("id", Integer.class);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public String extractRole(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return extractAllClaims(token).get("role", String.class);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public LocalDateTime getExpirationDate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        Claims claims = extractAllClaims(token);
        Date exp = claims.getExpiration();
        if (exp == null) {
            return null;
        }

        return LocalDateTime.ofInstant(exp.toInstant(), JWT_ZONE);
    }

    public boolean isTokenExpired(String token) {
        try {
            LocalDateTime expiration = getExpirationDate(token);
            if (expiration == null) {
                return true;
            }
            return expiration.isBefore(LocalDateTime.now(JWT_ZONE));
        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    private String generateTokenInternal(Integer userId, String email, String role, long expirationInMs) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationInMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("id", userId)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}