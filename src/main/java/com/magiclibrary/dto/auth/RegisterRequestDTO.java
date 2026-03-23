package com.magiclibrary.dto.auth;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION (Bean Validation Jakarta)
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

/* =============================================================================
   DTO : RegisterRequestDTO
   -----------------------------------------------------------------------------
   Description :
       Représente les données nécessaires pour créer un compte utilisateur
       via l’API publique (US-02). Aligné avec le MVP MagicLibrary.

   Règles principales :
       - aucun rôle transmis par le client (défini côté serveur)
       - mot de passe brut à l’entrée uniquement
       - validations Bean Validation exécutées avant toute logique métier
       - civilité et statut cotisation gérés côté serveur (valeurs par défaut)
       - exposition exclusive camelCase côté JSON
       - sécurité : mot de passe jamais loggé ni inclus dans equals/toString/hashCode
   =============================================================================
*/
@Schema(description = "Données nécessaires à la création d’un compte utilisateur (US-02 — PUBLIC).")
public class RegisterRequestDTO {

    // -------------------------------------------------------------------------
    // PRÉNOM (CAMELCASE API)
    // -------------------------------------------------------------------------
    @Schema(
            example = "Jean",
            description = "Prénom de l’utilisateur.",
            minLength = 2,
            maxLength = 150,
            nullable = false
    )
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(min = 2, max = 150, message = "Le prénom doit contenir entre 2 et 150 caractères.")
    private String firstName;

    // -------------------------------------------------------------------------
    // NOM (CAMELCASE API)
    // -------------------------------------------------------------------------
    @Schema(
            example = "Dupont",
            description = "Nom de l’utilisateur.",
            minLength = 2,
            maxLength = 150,
            nullable = false
    )
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères.")
    private String lastName;

    // -------------------------------------------------------------------------
    // EMAIL
    // -------------------------------------------------------------------------
    @Schema(
            example = "membre@example.com",
            description = "Adresse email unique de l’utilisateur.",
            maxLength = 255,
            nullable = false
    )
    @NotBlank(message = "L'adresse email est obligatoire.")
    @Email(message = "Le format de l'adresse email est invalide.")
    @Size(max = 255, message = "L'adresse email ne doit pas dépasser 255 caractères.")
    private String email;

    // -------------------------------------------------------------------------
    // MOT DE PASSE
    // -------------------------------------------------------------------------
    @Schema(
            example = "MotDePasse123!",
            description = "Mot de passe brut. Jamais renvoyé ni loggé. Hashé côté serveur (BCrypt).",
            minLength = 8,
            maxLength = 150,
            nullable = false
    )
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, max = 150, message = "Le mot de passe doit contenir entre 8 et 150 caractères.")
    private String password;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------
    public RegisterRequestDTO() {}

    public RegisterRequestDTO(
            String firstName,
            String lastName,
            String email,
            String password
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Le mot de passe ne doit jamais être loggé (toString / logs). */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // -------------------------------------------------------------------------
    // MÉTHODES UTILITAIRES : equals, hashCode, toString
    // -------------------------------------------------------------------------
    // Règles sécurité : ne jamais inclure le mot de passe
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegisterRequestDTO)) return false;
        RegisterRequestDTO that = (RegisterRequestDTO) o;
        return Objects.equals(firstName, that.firstName)
                && Objects.equals(lastName, that.lastName)
                && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email);
    }

    @Override
    public String toString() {
        return "RegisterRequestDTO{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}