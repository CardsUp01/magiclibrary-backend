package com.magiclibrary.security.jwt;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// -----------------------------------------------------------------------------
// IMPORTS JWT
// -----------------------------------------------------------------------------
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * =============================================================================
 *  UTILITAIRE JWT - GÉNÉRATION, VALIDATION ET LECTURE DES TOKENS
 * =============================================================================
 *
 *  🔐 OBJECTIF :
 *  -----------------------------------------------------------------------------
 *  Cette classe centralise toutes les opérations liées aux jetons JWT utilisés
 *  par l'authentification REST de MagicLibrary.
 *
 *  Elle permet de :
 *      - générer un token JWT signé ;
 *      - valider techniquement un token reçu ;
 *      - extraire l'email utilisateur ;
 *      - extraire l'identifiant utilisateur ;
 *      - extraire le rôle applicatif ;
 *      - lire la date d'expiration du token.
 *
 * =============================================================================
 *
 *  🧠 LOGIQUE MÉTIER :
 *  -----------------------------------------------------------------------------
 *  Le token JWT contient les informations minimales nécessaires à
 *  l'authentification stateless :
 *
 *      - subject : email utilisateur
 *      - claim id : identifiant utilisateur
 *      - claim role : rôle applicatif
 *      - issuedAt : date d'émission
 *      - expiration : date d'expiration
 *
 *  Le token ne stocke jamais de mot de passe ni de donnée sensible inutile.
 *
 * =============================================================================
 *
 *  ☁️ CONTEXTE PRODUCTION / RAILWAY :
 *  -----------------------------------------------------------------------------
 *  Le secret JWT est injecté via variable d'environnement :
 *
 *      JWT_SECRET
 *
 *  Dans application-prod.properties, cette valeur est référencée via :
 *
 *      spring.app.jwt.secret=${JWT_SECRET}
 *
 *  Le secret ne doit jamais être présent en clair dans le code source.
 *
 * =============================================================================
 *
 *  🔒 SÉCURITÉ :
 *  -----------------------------------------------------------------------------
 *  - Signature HMAC SHA-256
 *  - Secret externalisé
 *  - Vérification explicite de la longueur minimale du secret
 *  - Claims strictement limités aux informations nécessaires
 *  - Validation défensive des tokens null, vides, invalides ou expirés
 *
 *  Pour HS256, la clé doit contenir au minimum 256 bits, soit 32 caractères
 *  ASCII/UTF-8 simples. Cette classe vérifie donc explicitement que le secret
 *  configuré respecte cette exigence minimale.
 *
 * =============================================================================
 */
@Component
public class JwtUtil {

    private static final ZoneId JWT_ZONE = ZoneId.of("UTC");

    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${spring.app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.app.jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${spring.app.jwt.expirationRememberMe}")
    private long jwtExpirationRememberMeInMs;

    // -------------------------------------------------------------------------
    // GÉNÉRATION JWT - EXPIRATION STANDARD
    // -------------------------------------------------------------------------

    /*
     * Génère un jeton JWT avec la durée d'expiration standard.
     */
    public String generateToken(Integer userId, String email, String role) {
        return generateTokenInternal(userId, email, role, jwtExpirationInMs);
    }

    // -------------------------------------------------------------------------
    // GÉNÉRATION JWT - OPTION REMEMBER ME
    // -------------------------------------------------------------------------

    /*
     * Génère un jeton JWT en tenant compte de l'option Remember-Me.
     */
    public String generateToken(Integer userId, String email, String role, boolean rememberMe) {
        long expiration = rememberMe ? jwtExpirationRememberMeInMs : jwtExpirationInMs;
        return generateTokenInternal(userId, email, role, expiration);
    }

    // -------------------------------------------------------------------------
    // VALIDATION TECHNIQUE DU TOKEN
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // EXTRACTION EMAIL
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // EXTRACTION ID UTILISATEUR
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // EXTRACTION RÔLE
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // DATE D'EXPIRATION
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // CHECK EXPIRATION
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // CONSTRUCTION INTERNE DU TOKEN
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // EXTRACTION INTERNE DES CLAIMS
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // CLÉ DE SIGNATURE JWT
    // -------------------------------------------------------------------------

    /*
     * Construit la clé de signature HMAC à partir du secret configuré.
     *
     * Une validation explicite est effectuée afin d'obtenir une erreur claire
     * si la variable JWT_SECRET est absente ou trop courte.
     */
    private Key getSigningKey() {
        validateJwtSecret();

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // -------------------------------------------------------------------------
    // VALIDATION DU SECRET JWT
    // -------------------------------------------------------------------------

    /*
     * Vérifie que le secret JWT est présent et suffisamment long pour HS256.
     */
    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET est manquant. Impossible de signer ou valider les tokens JWT."
            );
        }

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court. Il doit contenir au minimum 32 caractères pour HS256."
            );
        }
    }
}