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

/**
 * Utilitaire chargé de générer, valider et lire les jetons JWT
 * utilisés par l'authentification de l'application.
 *
 * Cette classe centralise la signature des jetons, leur durée de validité
 * et l'extraction des informations nécessaires à Spring Security.
 */
@Component
public class JwtUtil {

    @Value("${spring.app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.app.jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${spring.app.jwt.expirationRememberMe}")
    private long jwtExpirationRememberMeInMs;

    private static final ZoneId JWT_ZONE = ZoneId.of("UTC");

    /*
     * Génère un jeton JWT avec la durée d'expiration standard.
     */
    public String generateToken(Integer userId, String email, String role) {
        return generateTokenInternal(userId, email, role, jwtExpirationInMs);
    }

    /*
     * Génère un jeton JWT en tenant compte de l'option Remember-Me.
     */
    public String generateToken(Integer userId, String email, String role, boolean rememberMe) {
        long expiration = rememberMe ? jwtExpirationRememberMeInMs : jwtExpirationInMs;
        return generateTokenInternal(userId, email, role, expiration);
    }

    /*
     * Vérifie qu'un jeton est lisible, signé correctement et non invalide.
     */
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

    /*
     * Retourne la date d'expiration du jeton en heure UTC.
     */
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

    /*
     * Indique si le jeton est expiré ou impossible à lire.
     */
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

    /*
     * Construit le jeton JWT avec les claims applicatifs nécessaires :
     * identifiant utilisateur, email, rôle, date d'émission et expiration.
     */
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

    /*
     * Extrait l'ensemble des claims après validation de la signature du jeton.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /*
     * Construit la clé de signature HMAC à partir du secret configuré.
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}