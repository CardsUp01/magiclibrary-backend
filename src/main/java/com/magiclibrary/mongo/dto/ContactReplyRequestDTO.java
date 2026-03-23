package com.magiclibrary.mongo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * =============================================================================
 * DTO : ContactReplyRequestDTO (ADMIN)
 * =============================================================================
 * DTO de saisie utilisé par un administrateur pour répondre à un message
 * de contact (US-13).
 *
 * Rôle :
 *      - transporter le contenu de la réponse côté backend
 *      - validation de la présence du texte via Jakarta Validation
 *      - utilisé par ContactServiceImpl.replyToContact
 *
 * Remarques :
 *      - aucune logique métier ici
 *      - toutes les dates et statuts sont gérés côté backend
 *      - conçu pour la couche REST / JSON
 */
public class ContactReplyRequestDTO {

    // -------------------------------------------------------------------------
    // CONTENU DE LA RÉPONSE (OBLIGATOIRE)
    // -------------------------------------------------------------------------
    /** Texte de la réponse envoyé par l’administrateur */
    @NotBlank(message = "responseContent est obligatoire.")
    private String responseContent;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------
    /** Constructeur par défaut requis par Spring */
    public ContactReplyRequestDTO() {
    }

    /**
     * Constructeur complet.
     *
     * @param responseContent contenu de la réponse
     */
    public ContactReplyRequestDTO(String responseContent) {
        this.responseContent = responseContent;
    }

    // -------------------------------------------------------------------------
    // GETTER / SETTER
    // -------------------------------------------------------------------------
    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }
}