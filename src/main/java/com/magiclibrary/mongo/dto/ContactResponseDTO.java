package com.magiclibrary.mongo.dto;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * DTO : ContactResponseDTO
 * =============================================================================
 *
 * DTO de réponse représentant un message de contact.
 *
 * Cette classe regroupe les informations nécessaires :
 *
 * - à la consultation du message ;
 * - au suivi de son statut métier ;
 * - à l'affichage de la réponse administrative ;
 * - à l'identification de l'administrateur ayant répondu ;
 * - à l'archivage logique indépendant côté membre et côté administration.
 *
 * IMPORTANT :
 *
 * Les états d'archivage MEMBRE et ADMIN sont totalement indépendants.
 *
 * Exemple :
 *
 * - archivedByMember = true
 * - archivedByAdmin = false
 *
 * signifie que :
 *
 * - le membre a rangé le message dans ses archives ;
 * - l'administration continue à le voir dans ses messages actifs.
 *
 * Aucun de ces champs ne provoque de suppression physique du document MongoDB.
 * =============================================================================
 */
public class ContactResponseDTO {

    // -------------------------------------------------------------------------
    // IDENTIFICATION
    // -------------------------------------------------------------------------

    private String id;
    private Integer idUser;

    // -------------------------------------------------------------------------
    // MESSAGE
    // -------------------------------------------------------------------------

    private String name;
    private String email;
    private String subject;
    private String content;
    private String origin;

    // -------------------------------------------------------------------------
    // STATUT MÉTIER
    // -------------------------------------------------------------------------

    private String status;
    private String statusLabel;
    private boolean answered;
    private String statusBadgeClass;
    private String senderRoleLabel;

    // -------------------------------------------------------------------------
    // DATES / RÉPONSE ADMINISTRATIVE
    // -------------------------------------------------------------------------

    private LocalDateTime date;

    private boolean responseSent;
    private String responseContent;

    private Integer answeredByUserId;
    private String answeredByAdminLabel;

    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // ARCHIVAGE CÔTÉ MEMBRE
    // -------------------------------------------------------------------------

    /**
     * Indique si le message a été archivé dans l'espace personnel
     * de son auteur.
     *
     * Cet état n'a aucun effet sur l'espace ADMIN.
     */
    private boolean archivedByMember;

    /**
     * Date de l'archivage effectué par le membre.
     *
     * Null lorsque le message est actif côté membre.
     */
    private LocalDateTime archivedAtByMember;

    // -------------------------------------------------------------------------
    // ARCHIVAGE CÔTÉ ADMINISTRATION
    // -------------------------------------------------------------------------

    /**
     * Indique si le message a été archivé dans l'espace administratif.
     *
     * Cet état n'a aucun effet sur l'espace du membre.
     */
    private boolean archivedByAdmin;

    /**
     * Date de l'archivage effectué côté administration.
     *
     * Null lorsque le message est actif côté ADMIN.
     */
    private LocalDateTime archivedAtByAdmin;

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR PAR DÉFAUT
    // -------------------------------------------------------------------------

    public ContactResponseDTO() {
    }

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR HISTORIQUE
    // -------------------------------------------------------------------------

    /**
     * Constructeur historique conservé temporairement pour compatibilité
     * avec les appels existants.
     *
     * Les informations d'archivage prennent alors leurs valeurs par défaut :
     *
     * - archivedByMember = false ;
     * - archivedAtByMember = null ;
     * - archivedByAdmin = false ;
     * - archivedAtByAdmin = null.
     *
     * Le service sera ensuite migré vers le constructeur complet.
     */
    public ContactResponseDTO(
            String id,
            Integer idUser,
            String name,
            String email,
            String subject,
            String content,
            String origin,
            String status,
            String statusLabel,
            boolean answered,
            String statusBadgeClass,
            String senderRoleLabel,
            LocalDateTime date,
            boolean responseSent,
            String responseContent,
            Integer answeredByUserId,
            String answeredByAdminLabel,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.idUser = idUser;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.content = content;
        this.origin = origin;
        this.status = status;
        this.statusLabel = statusLabel;
        this.answered = answered;
        this.statusBadgeClass = statusBadgeClass;
        this.senderRoleLabel = senderRoleLabel;
        this.date = date;
        this.responseSent = responseSent;
        this.responseContent = responseContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredByAdminLabel = answeredByAdminLabel;
        this.updatedAt = updatedAt;

        this.archivedByMember = false;
        this.archivedAtByMember = null;

        this.archivedByAdmin = false;
        this.archivedAtByAdmin = null;
    }

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR COMPLET
    // -------------------------------------------------------------------------

    /**
     * Constructeur complet utilisé par la nouvelle gestion
     * Actifs / Archivés.
     */
    public ContactResponseDTO(
            String id,
            Integer idUser,
            String name,
            String email,
            String subject,
            String content,
            String origin,
            String status,
            String statusLabel,
            boolean answered,
            String statusBadgeClass,
            String senderRoleLabel,
            LocalDateTime date,
            boolean responseSent,
            String responseContent,
            Integer answeredByUserId,
            String answeredByAdminLabel,
            LocalDateTime updatedAt,
            boolean archivedByMember,
            LocalDateTime archivedAtByMember,
            boolean archivedByAdmin,
            LocalDateTime archivedAtByAdmin
    ) {
        this.id = id;
        this.idUser = idUser;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.content = content;
        this.origin = origin;
        this.status = status;
        this.statusLabel = statusLabel;
        this.answered = answered;
        this.statusBadgeClass = statusBadgeClass;
        this.senderRoleLabel = senderRoleLabel;
        this.date = date;
        this.responseSent = responseSent;
        this.responseContent = responseContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredByAdminLabel = answeredByAdminLabel;
        this.updatedAt = updatedAt;

        this.archivedByMember = archivedByMember;
        this.archivedAtByMember = archivedAtByMember;

        this.archivedByAdmin = archivedByAdmin;
        this.archivedAtByAdmin = archivedAtByAdmin;
    }

    // -------------------------------------------------------------------------
    // GETTERS HISTORIQUES
    // -------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getOrigin() {
        return origin;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public boolean isAnswered() {
        return answered;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public String getSenderRoleLabel() {
        return senderRoleLabel;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public boolean isResponseSent() {
        return responseSent;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public Integer getAnsweredByUserId() {
        return answeredByUserId;
    }

    public String getAnsweredByAdminLabel() {
        return answeredByAdminLabel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // -------------------------------------------------------------------------
    // GETTERS D'ARCHIVAGE MEMBRE
    // -------------------------------------------------------------------------

    public boolean isArchivedByMember() {
        return archivedByMember;
    }

    public LocalDateTime getArchivedAtByMember() {
        return archivedAtByMember;
    }

    // -------------------------------------------------------------------------
    // GETTERS D'ARCHIVAGE ADMIN
    // -------------------------------------------------------------------------

    public boolean isArchivedByAdmin() {
        return archivedByAdmin;
    }

    public LocalDateTime getArchivedAtByAdmin() {
        return archivedAtByAdmin;
    }
}