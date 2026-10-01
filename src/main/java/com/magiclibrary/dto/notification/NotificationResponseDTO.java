package com.magiclibrary.dto.notification;

import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;

import java.time.LocalDateTime;

/*
 * =============================================================================
 * DTO : NotificationResponseDTO
 * =============================================================================
 *
 * Description :
 *     Représente une notification renvoyée au front-end MagicLibrary.
 *
 * Le DTO contient désormais les informations nécessaires aux trois vues
 * fonctionnelles du module Notifications :
 *
 *     - Reçues ;
 *     - Envoyées ;
 *     - Archivées.
 *
 * Il expose notamment :
 *
 *     - l'identité du destinataire ;
 *     - l'identité éventuelle de l'expéditeur humain ;
 *     - le contenu de la notification ;
 *     - l'état lu / non lu ;
 *     - l'état d'archivage du destinataire ;
 *     - les dates associées.
 *
 * IMPORTANT :
 *     sentByUserId et les informations d'expéditeur peuvent être null lorsque :
 *
 *     - la notification est automatique ;
 *     - la notification est historique et antérieure à l'ajout
 *       de la traçabilité des envois administratifs.
 *
 * =============================================================================
 */
public class NotificationResponseDTO {

    // =========================================================================
    // IDENTIFIANT DE LA NOTIFICATION
    // =========================================================================

    /**
     * Identifiant unique de la notification.
     */
    private Integer idNotification;

    // =========================================================================
    // DESTINATAIRE
    // =========================================================================

    /**
     * Identifiant technique de l'utilisateur destinataire.
     *
     * Champ historique conservé pour compatibilité avec les écrans,
     * recherches et contrôleurs existants.
     */
    private Integer idUser;

    /**
     * Prénom du destinataire.
     */
    private String recipientFirstName;

    /**
     * Nom du destinataire.
     */
    private String recipientLastName;

    /**
     * Adresse email du destinataire.
     *
     * Cette information permet notamment de distinguer proprement
     * plusieurs utilisateurs portant un nom identique.
     */
    private String recipientEmail;

    // =========================================================================
    // EXPÉDITEUR HUMAIN ÉVENTUEL
    // =========================================================================

    /**
     * Identifiant de l'utilisateur ayant envoyé manuellement
     * la notification.
     *
     * Valeur null pour :
     *
     * - les notifications automatiques ;
     * - les anciennes notifications sans traçabilité d'expéditeur.
     */
    private Integer sentByUserId;

    /**
     * Prénom de l'expéditeur humain éventuel.
     */
    private String senderFirstName;

    /**
     * Nom de l'expéditeur humain éventuel.
     */
    private String senderLastName;

    /**
     * Adresse email de l'expéditeur humain éventuel.
     */
    private String senderEmail;

    // =========================================================================
    // CONTENU
    // =========================================================================

    /**
     * Titre affiché de la notification.
     */
    private String titleNotification;

    /**
     * Message principal de la notification.
     */
    private String messageNotification;

    /**
     * Lien cible associé à la notification.
     *
     * Facultatif.
     */
    private String targetLinkNotification;

    // =========================================================================
    // ÉTAT DE LECTURE
    // =========================================================================

    /**
     * Indique si la notification a été lue par son destinataire.
     *
     * Cet état appartient fonctionnellement au destinataire.
     *
     * Dans la vue "Envoyées", l'administration peut consulter cette
     * information mais ne doit pas pouvoir la modifier.
     */
    private Boolean readNotification;

    // =========================================================================
    // DATE DE CRÉATION / ENVOI
    // =========================================================================

    /**
     * Date et heure de création de la notification.
     *
     * Pour une notification manuelle, cette date correspond également
     * à la date d'envoi.
     */
    private LocalDateTime dateNotification;

    // =========================================================================
    // ARCHIVAGE CÔTÉ DESTINATAIRE
    // =========================================================================

    /**
     * Indique si le destinataire a archivé la notification.
     *
     * true :
     *     notification visible dans "Archivées".
     *
     * false :
     *     notification visible dans "Reçues".
     *
     * Cet état n'a aucun effet sur l'historique administratif "Envoyées".
     */
    private Boolean archivedByRecipient;

    /**
     * Date et heure du dernier archivage par le destinataire.
     *
     * Valeur null lorsque la notification n'est pas archivée.
     */
    private LocalDateTime archivedAtByRecipient;

    // =========================================================================
    // TYPE / CATÉGORIE / PRIORITÉ
    // =========================================================================

    /**
     * Type de notification :
     * OVERDUE, REMINDER, RETURN, SYSTEM, CONTACT...
     */
    private NotificationType typeNotification;

    /**
     * Catégorie métier de la notification.
     */
    private NotificationCategory categoryNotification;

    /**
     * Niveau de priorité éventuel.
     *
     * Exemples :
     * HIGH, MEDIUM, LOW.
     */
    private String priorityNotification;

    // =========================================================================
    // GETTERS / SETTERS
    // =========================================================================

    public Integer getIdNotification() {
        return idNotification;
    }

    public void setIdNotification(
            Integer idNotification
    ) {
        this.idNotification = idNotification;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(
            Integer idUser
    ) {
        this.idUser = idUser;
    }

    public String getRecipientFirstName() {
        return recipientFirstName;
    }

    public void setRecipientFirstName(
            String recipientFirstName
    ) {
        this.recipientFirstName = recipientFirstName;
    }

    public String getRecipientLastName() {
        return recipientLastName;
    }

    public void setRecipientLastName(
            String recipientLastName
    ) {
        this.recipientLastName = recipientLastName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(
            String recipientEmail
    ) {
        this.recipientEmail = recipientEmail;
    }

    public Integer getSentByUserId() {
        return sentByUserId;
    }

    public void setSentByUserId(
            Integer sentByUserId
    ) {
        this.sentByUserId = sentByUserId;
    }

    public String getSenderFirstName() {
        return senderFirstName;
    }

    public void setSenderFirstName(
            String senderFirstName
    ) {
        this.senderFirstName = senderFirstName;
    }

    public String getSenderLastName() {
        return senderLastName;
    }

    public void setSenderLastName(
            String senderLastName
    ) {
        this.senderLastName = senderLastName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(
            String senderEmail
    ) {
        this.senderEmail = senderEmail;
    }

    public String getTitleNotification() {
        return titleNotification;
    }

    public void setTitleNotification(
            String titleNotification
    ) {
        this.titleNotification = titleNotification;
    }

    public String getMessageNotification() {
        return messageNotification;
    }

    public void setMessageNotification(
            String messageNotification
    ) {
        this.messageNotification = messageNotification;
    }

    public String getTargetLinkNotification() {
        return targetLinkNotification;
    }

    public void setTargetLinkNotification(
            String targetLinkNotification
    ) {
        this.targetLinkNotification = targetLinkNotification;
    }

    public Boolean getReadNotification() {
        return readNotification;
    }

    public void setReadNotification(
            Boolean readNotification
    ) {
        this.readNotification = readNotification;
    }

    public LocalDateTime getDateNotification() {
        return dateNotification;
    }

    public void setDateNotification(
            LocalDateTime dateNotification
    ) {
        this.dateNotification = dateNotification;
    }

    public Boolean getArchivedByRecipient() {
        return archivedByRecipient;
    }

    public void setArchivedByRecipient(
            Boolean archivedByRecipient
    ) {
        this.archivedByRecipient = archivedByRecipient;
    }

    public LocalDateTime getArchivedAtByRecipient() {
        return archivedAtByRecipient;
    }

    public void setArchivedAtByRecipient(
            LocalDateTime archivedAtByRecipient
    ) {
        this.archivedAtByRecipient = archivedAtByRecipient;
    }

    public NotificationType getTypeNotification() {
        return typeNotification;
    }

    public void setTypeNotification(
            NotificationType typeNotification
    ) {
        this.typeNotification = typeNotification;
    }

    public NotificationCategory getCategoryNotification() {
        return categoryNotification;
    }

    public void setCategoryNotification(
            NotificationCategory categoryNotification
    ) {
        this.categoryNotification = categoryNotification;
    }

    public String getPriorityNotification() {
        return priorityNotification;
    }

    public void setPriorityNotification(
            String priorityNotification
    ) {
        this.priorityNotification = priorityNotification;
    }
}