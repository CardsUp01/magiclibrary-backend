package com.magiclibrary.dto.auth;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

/* =============================================================================
   DTO : LoginResponseDTO
   -----------------------------------------------------------------------------
   Description :
       Représente la réponse renvoyée au client après une authentification réussie
       (US-01) dans MagicLibrary. Aligné strictement avec le MVP et le dictionnaire.

   Contenu :
       - jeton JWT signé (token)
       - date/heure d’expiration du token
       - identifiant utilisateur
       - rôle de l’utilisateur

   Règles principales :
       - aucun mot de passe ou information sensible interne
       - camelCase strict côté JSON
       - JWT jamais loggé
       - compatible front-end / Postman / API clients
   =============================================================================
*/
@Schema(description = "Réponse envoyée après une authentification réussie (US-01).")
public class LoginResponseDTO {

    // -------------------------------------------------------------------------
    // TOKEN JWT
    // -------------------------------------------------------------------------
    /**
     * Jeton d'authentification signé via JwtUtil (HS256).
     *
     * Usage front-end :
     *      - autoriser les requêtes ultérieures,
     *      - maintenir la session active.
     *
     * Sécurité :
     *      - ne jamais logguer ce token
     *      - ne pas inclure dans equals/hashCode/toString
     */
    @Schema(
            description = "Jeton JWT signé permettant l’accès aux endpoints sécurisés.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0..."
    )
    private String token;

    // -------------------------------------------------------------------------
    // DATE D’EXPIRATION DU TOKEN
    // -------------------------------------------------------------------------
    @Schema(
            description = "Date et heure d’expiration du token JWT.",
            example = "2025-12-10T21:45:00"
    )
    private LocalDateTime expiresAt;

    // -------------------------------------------------------------------------
    // INFORMATIONS UTILISATEUR
    // -------------------------------------------------------------------------
    @Schema(
            description = "Identifiant unique de l’utilisateur authentifié.",
            example = "42"
    )
    private Integer idUser;

    /**
     * Rôle applicatif de l’utilisateur (ADMIN, MEMBRE, INVITE).
     * Permet au front-end d’activer ou désactiver certaines fonctionnalités.
     * Aligné avec Role.labelRole et ERole enum.
     */
    @Schema(
            description = "Rôle applicatif de l’utilisateur.",
            example = "MEMBRE"
    )
    private String role;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------
    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, LocalDateTime expiresAt, Integer idUser, String role) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.idUser = idUser;
        this.role = role;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // -------------------------------------------------------------------------
    // UTILITAIRES : equals, hashCode, toString
    // -------------------------------------------------------------------------
    // Règles sécurité :
    // - ne jamais inclure le JWT (token)
    // - protège contre fuites en logs, comparaisons involontaires, caches
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginResponseDTO)) return false;
        LoginResponseDTO that = (LoginResponseDTO) o;
        return Objects.equals(expiresAt, that.expiresAt)
                && Objects.equals(idUser, that.idUser)
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiresAt, idUser, role);
    }

    @Override
    public String toString() {
        return "LoginResponseDTO{" +
                "expiresAt=" + expiresAt +
                ", idUser=" + idUser +
                ", role='" + role + '\'' +
                '}';
    }
}