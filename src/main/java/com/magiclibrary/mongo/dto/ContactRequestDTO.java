package com.magiclibrary.mongo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * =============================================================================
 * DTO : ContactRequestDTO (PUBLIC)
 * =============================================================================
 * DTO de saisie pour la création d’un message de contact via le front
 * (formulaire web / US-12).
 *
 * Rôle :
 *      - transport des données du formulaire côté backend
 *      - validation des contraintes via annotations Jakarta Validation
 *      - utilisé par ContactServiceImpl pour créer un document ContactDocument
 *
 * Remarques :
 *      - aucun traitement métier ici
 *      - identifiant utilisateur optionnel (null si invité)
 *      - toutes les validations et longueurs respectent le dictionnaire de données
 */
public class ContactRequestDTO {

    // -------------------------------------------------------------------------
    // IDENTIFIANT UTILISATEUR (OPTIONNEL)
    // -------------------------------------------------------------------------
    /** ID de l’utilisateur ayant envoyé le message, null si invité */
    private Integer idUser;

    // -------------------------------------------------------------------------
    // NOM DE L’EXPÉDITEUR (OPTIONNEL)
    // -------------------------------------------------------------------------
    /** Nom de l’expéditeur (max 120 caractères) */
    @Size(max = 120, message = "name ne doit pas dépasser 120 caractères.")
    private String name;

    // -------------------------------------------------------------------------
    // EMAIL DE L’EXPÉDITEUR (OBLIGATOIRE)
    // -------------------------------------------------------------------------
    /** Email de l’expéditeur (obligatoire, format valide, max 150 caractères) */
    @NotBlank(message = "email est obligatoire.")
    @Email(message = "email doit être une adresse valide.")
    @Size(max = 150, message = "email ne doit pas dépasser 150 caractères.")
    private String email;

    // -------------------------------------------------------------------------
    // SUJET DU MESSAGE (OBLIGATOIRE)
    // -------------------------------------------------------------------------
    /** Sujet du message (obligatoire, max 150 caractères) */
    @NotBlank(message = "subject est obligatoire.")
    @Size(max = 150, message = "subject ne doit pas dépasser 150 caractères.")
    private String subject;

    // -------------------------------------------------------------------------
    // CONTENU DU MESSAGE (OBLIGATOIRE)
    // -------------------------------------------------------------------------
    /** Contenu du message (obligatoire, max 2000 caractères) */
    @NotBlank(message = "message est obligatoire.")
    @Size(max = 2000, message = "message ne doit pas dépasser 2000 caractères.")
    private String message;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------
    /** Constructeur par défaut requis par Spring */
    public ContactRequestDTO() {
    }

    /**
     * Constructeur complet.
     *
     * @param idUser identifiant utilisateur (optionnel)
     * @param name nom de l’expéditeur (optionnel)
     * @param email email de l’expéditeur
     * @param subject sujet du message
     * @param message contenu du message
     */
    public ContactRequestDTO(Integer idUser, String name, String email, String subject, String message) {
        this.idUser = idUser;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
    }

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------
    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}