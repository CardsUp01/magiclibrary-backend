package com.magiclibrary.entities;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS JPA (explicites, jamais de wildcard)
// -----------------------------------------------------------------------------
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION (Bean Validation Jakarta)
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS JACKSON / SWAGGER FIX
// -----------------------------------------------------------------------------
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * =============================================================================
 *  ENTITY : USER
 * =============================================================================
 *  Cette entité représente un utilisateur de l'application MagicLibrary.
 *
 *  Elle centralise l'ensemble des informations nécessaires à :
 *      - l’identification de l’utilisateur ;
 *      - la gestion des droits via son rôle (ADMIN, MEMBRE, INVITE) ;
 *      - la sécurité (authentification, vérification email, réinitialisation) ;
 *      - la gestion associative (cotisation, caution, FFAP) ;
 *      - la personnalisation du profil ;
 *      - le suivi des activités (connexion, mise à jour, inscription).
 *
 *  Cette classe est directement conforme :
 *      - au dictionnaire de données USER du MVP ;
 *      - au MCD / MLD / MPD validés ;
 *      - aux contraintes de validation applicative (Bean Validation) ;
 *      - aux pratiques professionnelles Spring Boot & JPA.
 *
 *  Aucune supposition n’a été effectuée : tous les champs, types et règles
 *  proviennent strictement des documents fournis.
 *
 *  Cette entité constitue un pivot essentiel du modèle MagicLibrary : elle
 *  alimente les services applicatifs, les contrôleurs REST, la sécurité JWT,
 *  ainsi que les futures évolutions (statistiques, notifications avancées).
 * =============================================================================
 */
@Entity
@Table(name = "user")
@Schema(description = "Entité représentant un utilisateur du système MagicLibrary.")
public class User {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE (PRIMARY KEY)
    // -------------------------------------------------------------------------

    /**
     * Identifiant unique de l'utilisateur.
     * Généré automatiquement par la base de données.
     * Correspond à la clé primaire id_user (INT AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user", nullable = false)
    private Integer idUser;

    // -------------------------------------------------------------------------
    // RELATION OBLIGATOIRE AVEC ROLE (FK id_role)
    // -------------------------------------------------------------------------

    /**
     * Rôle applicatif associé à l'utilisateur.
     * Relation Many-To-One obligatoire vers l'entité Role.
     * Clé étrangère : id_role → ROLE(id_role), NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", nullable = false)
    @NotNull(message = "Le rôle est obligatoire.")
    @JsonIgnore // FIX Swagger + Jackson : empêche les boucles et l’erreur /v3/api-docs
    private Role role;

    // -------------------------------------------------------------------------
    // INFORMATIONS D’IDENTITÉ
    // -------------------------------------------------------------------------

    @Column(name = "civility_user", length = 20, nullable = false)
    @NotBlank(message = "La civilité est obligatoire.")
    @Size(max = 20, message = "La civilité ne doit pas dépasser 20 caractères.")
    private String civilityUser;

    /**
     * Prénom de l'utilisateur.
     * IMPORTANT : exposé côté API en camelCase via le champ Java firstNameUser.
     * La base de données reste inchangée : colonne fname_user.
     */
    @Column(name = "fname_user", length = 150, nullable = false)
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(min = 2, max = 150, message = "Le prénom doit contenir entre 2 et 150 caractères.")
    private String firstNameUser;

    /**
     * Nom de l'utilisateur.
     * IMPORTANT : exposé côté API en camelCase via le champ Java lastNameUser.
     * La base de données reste inchangée : colonne lname_user.
     */
    @Column(name = "lname_user", length = 150, nullable = false)
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères.")
    private String lastNameUser;

    // -------------------------------------------------------------------------
    // COORDONNÉES DE CONTACT
    // -------------------------------------------------------------------------

    @Column(name = "email_user", length = 255, nullable = false, unique = true)
    @NotBlank(message = "L'adresse email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    @Size(min = 5, max = 255, message = "L'email doit contenir entre 5 et 255 caractères.")
    private String emailUser;

    @Column(name = "email_verified_user", nullable = false)
    @NotNull(message = "Le statut de vérification email est obligatoire.")
    private Boolean emailVerifiedUser;

    @Column(name = "email_verification_token_user", length = 255)
    @Size(min = 2, max = 255, message = "Le jeton de vérification email doit contenir entre 2 et 255 caractères.")
    private String emailVerificationTokenUser;

    @Column(name = "phone_user", length = 20)
    @Size(min = 10, max = 20, message = "Le numéro de téléphone doit contenir entre 10 et 20 caractères.")
    private String phoneUser;

    @Column(name = "address_user", length = 255)
    @Size(min = 5, max = 255, message = "L'adresse doit contenir entre 5 et 255 caractères.")
    private String addressUser;

    // -------------------------------------------------------------------------
    // INFORMATIONS ASSOCIATIVES / FFAP
    // -------------------------------------------------------------------------

    @Column(name = "ffap_member_user")
    private Boolean ffapMemberUser;

    @Column(name = "ffap_number_user", length = 20)
    @Size(min = 1, max = 20, message = "Le numéro FFAP doit contenir entre 1 et 20 caractères.")
    private String ffapNumberUser;

    @Column(name = "association_join_date_user")
    private LocalDate associationJoinDateUser;

    // -------------------------------------------------------------------------
    // SÉCURITÉ : MOT DE PASSE & RÉINITIALISATION
    // -------------------------------------------------------------------------

    @Column(name = "password_user", length = 255, nullable = false)
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, max = 150, message = "Le mot de passe doit contenir entre 8 et 150 caractères.")
    private String passwordUser;

    @Column(name = "reset_token_user", length = 255)
    @Size(min = 2, max = 255, message = "Le jeton de réinitialisation doit contenir entre 2 et 255 caractères.")
    private String resetTokenUser;

    @Column(name = "reset_token_expire_user")
    private LocalDateTime resetTokenExpireUser;

    // -------------------------------------------------------------------------
    // STATUT DU COMPTE, COTISATION ET CAUTION
    // -------------------------------------------------------------------------

    @Column(name = "active_user", nullable = false)
    @NotNull(message = "Le statut actif est obligatoire.")
    private Boolean activeUser;

    @Column(name = "subscription_user", nullable = false)
    @NotNull(message = "Le statut de cotisation est obligatoire.")
    private Boolean subscriptionUser;

    @Column(name = "deposit_user")
    private Boolean depositUser;

    // -------------------------------------------------------------------------
    // SUIVI DU COMPTE : DATES CLÉS
    // -------------------------------------------------------------------------

    @Column(name = "signup_date_user", nullable = false)
    @NotNull(message = "La date d'inscription est obligatoire.")
    @PastOrPresent(message = "La date d'inscription ne peut pas être future.")
    private LocalDateTime signupDateUser;

    @Column(name = "last_login_user")
    @PastOrPresent(message = "La dernière connexion ne peut pas être future.")
    private LocalDateTime lastLoginUser;

    @Column(name = "updated_at_user")
    @PastOrPresent(message = "La date de mise à jour ne peut pas être future.")
    private LocalDateTime updatedAtUser;

    // -------------------------------------------------------------------------
    // PERSONNALISATION DU PROFIL
    // -------------------------------------------------------------------------

    @Column(name = "avatar_user", length = 255)
    @Size(max = 255, message = "L'URL de l'avatar ne doit pas dépasser 255 caractères.")
    private String avatarUser;

    @Column(name = "bio_user", columnDefinition = "TEXT")
    @Size(min = 2, max = 10_000, message = "La biographie doit contenir entre 2 et 10 000 caractères.")
    private String bioUser;

    @Column(name = "notes_user", columnDefinition = "TEXT")
    @Size(min = 2, max = 10_000, message = "Les notes doivent contenir entre 2 et 10 000 caractères.")
    private String notesUser;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    public User() {}

    public User(
            Role role,
            String civilityUser,
            String firstNameUser,
            String lastNameUser,
            String emailUser,
            String passwordUser,
            Boolean activeUser,
            Boolean subscriptionUser,
            LocalDateTime signupDateUser
    ) {
        this.role = role;
        this.civilityUser = civilityUser;
        this.firstNameUser = firstNameUser;
        this.lastNameUser = lastNameUser;
        this.emailUser = emailUser;
        this.passwordUser = passwordUser;
        this.activeUser = activeUser;
        this.subscriptionUser = subscriptionUser;
        this.signupDateUser = signupDateUser;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    public Integer getIdUser() { return idUser; }
    public void setIdUser(Integer idUser) { this.idUser = idUser; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getCivilityUser() { return civilityUser; }
    public void setCivilityUser(String civilityUser) { this.civilityUser = civilityUser; }

    public String getFirstNameUser() { return firstNameUser; }
    public void setFirstNameUser(String firstNameUser) { this.firstNameUser = firstNameUser; }

    public String getLastNameUser() { return lastNameUser; }
    public void setLastNameUser(String lastNameUser) { this.lastNameUser = lastNameUser; }

    public String getEmailUser() { return emailUser; }
    public void setEmailUser(String emailUser) { this.emailUser = emailUser; }

    public Boolean getEmailVerifiedUser() { return emailVerifiedUser; }
    public void setEmailVerifiedUser(Boolean emailVerifiedUser) { this.emailVerifiedUser = emailVerifiedUser; }

    public String getEmailVerificationTokenUser() { return emailVerificationTokenUser; }
    public void setEmailVerificationTokenUser(String emailVerificationTokenUser) { this.emailVerificationTokenUser = emailVerificationTokenUser; }

    public String getPhoneUser() { return phoneUser; }
    public void setPhoneUser(String phoneUser) { this.phoneUser = phoneUser; }

    public String getAddressUser() { return addressUser; }
    public void setAddressUser(String addressUser) { this.addressUser = addressUser; }

    public Boolean getFfapMemberUser() { return ffapMemberUser; }
    public void setFfapMemberUser(Boolean ffapMemberUser) { this.ffapMemberUser = ffapMemberUser; }

    public String getFfapNumberUser() { return ffapNumberUser; }
    public void setFfapNumberUser(String ffapNumberUser) { this.ffapNumberUser = ffapNumberUser; }

    public LocalDate getAssociationJoinDateUser() { return associationJoinDateUser; }
    public void setAssociationJoinDateUser(LocalDate associationJoinDateUser) { this.associationJoinDateUser = associationJoinDateUser; }

    public String getPasswordUser() { return passwordUser; }
    public void setPasswordUser(String passwordUser) { this.passwordUser = passwordUser; }

    public String getResetTokenUser() { return resetTokenUser; }
    public void setResetTokenUser(String resetTokenUser) { this.resetTokenUser = resetTokenUser; }

    public LocalDateTime getResetTokenExpireUser() { return resetTokenExpireUser; }
    public void setResetTokenExpireUser(LocalDateTime resetTokenExpireUser) { this.resetTokenExpireUser = resetTokenExpireUser; }

    public Boolean getActiveUser() { return activeUser; }
    public void setActiveUser(Boolean activeUser) { this.activeUser = activeUser; }

    public Boolean getSubscriptionUser() { return subscriptionUser; }
    public void setSubscriptionUser(Boolean subscriptionUser) { this.subscriptionUser = subscriptionUser; }

    public Boolean getDepositUser() { return depositUser; }
    public void setDepositUser(Boolean depositUser) { this.depositUser = depositUser; }

    public LocalDateTime getSignupDateUser() { return signupDateUser; }
    public void setSignupDateUser(LocalDateTime signupDateUser) { this.signupDateUser = signupDateUser; }

    public LocalDateTime getLastLoginUser() { return lastLoginUser; }
    public void setLastLoginUser(LocalDateTime lastLoginUser) { this.lastLoginUser = lastLoginUser; }

    public LocalDateTime getUpdatedAtUser() { return updatedAtUser; }
    public void setUpdatedAtUser(LocalDateTime updatedAtUser) { this.updatedAtUser = updatedAtUser; }

    public String getAvatarUser() { return avatarUser; }
    public void setAvatarUser(String avatarUser) { this.avatarUser = avatarUser; }

    public String getBioUser() { return bioUser; }
    public void setBioUser(String bioUser) { this.bioUser = bioUser; }

    public String getNotesUser() { return notesUser; }
    public void setNotesUser(String notesUser) { this.notesUser = notesUser; }

    // -------------------------------------------------------------------------
    // MÉTHODES UTILITAIRES
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(idUser, user.idUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUser);
    }

    @Override
    public String toString() {
        return "User{" +
                "idUser=" + idUser +
                ", emailUser='" + emailUser + '\'' +
                '}';
    }
}