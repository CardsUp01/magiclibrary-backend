package com.magiclibrary.dto.user;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.io.Serializable;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* =============================================================================
   DTO : USER CREATE (Entrée API — POST /users)
   ---------------------------------------------------------------------------
   Description :
       Données envoyées par l’API pour créer un utilisateur via POST /users.
       Destiné à l’administrateur (ADMIN).
       Nomenclature JSON : camelCase strict (civility / firstName / lastName / email / password).

   Objectif CDA / Jury :
       - Chaque champ documenté avec exemple et contrainte
       - Aligné sur le dictionnaire USER MVP
       - Conforme aux règles de validation Bean Validation
   =============================================================================
*/
@Schema(description = "Données nécessaires pour créer un utilisateur via POST /users (ADMIN).")
public class UserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // CIVILITÉ
    // -------------------------------------------------------------------------

    /**
     * Civilité de l’utilisateur.
     * Obligatoire.
     * Longueur maximale : 20 caractères.
     */
    @Schema(description = "Civilité de l’utilisateur à créer.", example = "Mme")
    @NotBlank(message = "La civilité est obligatoire.")
    @Size(max = 20, message = "La civilité ne doit pas dépasser 20 caractères.")
    private String civility;

    // -------------------------------------------------------------------------
    // PRÉNOM (CAMELCASE API)
    // -------------------------------------------------------------------------

    /**
     * Prénom de l’utilisateur.
     * Obligatoire.
     * Longueur : 2 à 150 caractères.
     */
    @Schema(description = "Prénom de l’utilisateur à créer.", example = "Alice")
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(min = 2, max = 150, message = "Le prénom doit contenir entre 2 et 150 caractères.")
    private String firstName;

    // -------------------------------------------------------------------------
    // NOM (CAMELCASE API)
    // -------------------------------------------------------------------------

    /**
     * Nom de l’utilisateur.
     * Obligatoire.
     * Longueur : 2 à 150 caractères.
     */
    @Schema(description = "Nom de l’utilisateur à créer.", example = "Dupont")
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères.")
    private String lastName;

    // -------------------------------------------------------------------------
    // EMAIL
    // -------------------------------------------------------------------------

    /**
     * Adresse email unique de l’utilisateur.
     * Obligatoire.
     * Format valide et longueur maximale 255 caractères.
     * Utilisé pour l’authentification.
     */
    @Schema(description = "Adresse email unique de l’utilisateur à créer.", example = "alice.dupont@example.com")
    @NotBlank(message = "L'adresse email est obligatoire.")
    @Email(message = "Le format de l'adresse email est invalide.")
    @Size(max = 255, message = "L'adresse email ne doit pas dépasser 255 caractères.")
    private String email;

    // -------------------------------------------------------------------------
    // MOT DE PASSE
    // -------------------------------------------------------------------------

    /**
     * Mot de passe brut envoyé par le client.
     * Obligatoire.
     * Longueur minimale : 8 caractères.
     * Ne doit jamais être loggé ou exposé.
     * Le hashage est effectué côté service.
     */
    @Schema(
            description = "Mot de passe brut envoyé par le client. Il sera hashé côté serveur.",
            example = "SuperSecret123!"
    )
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    private String password;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    /** Constructeur par défaut requis par Spring / Jackson */
    public UserCreateDTO() {}

    /**
     * Constructeur complet pour instanciation rapide.
     *
     * @param civility civilité de l’utilisateur
     * @param firstName prénom de l’utilisateur
     * @param lastName nom de l’utilisateur
     * @param email email de l’utilisateur
     * @param password mot de passe brut
     */
    public UserCreateDTO(
            String civility,
            String firstName,
            String lastName,
            String email,
            String password
    ) {
        this.civility = civility;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------

    public String getCivility() {
        return civility;
    }

    public void setCivility(String civility) {
        this.civility = civility;
    }

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

    /** Le mot de passe ne doit jamais être loggé */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}