package com.magiclibrary.dto.notification;

import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import java.time.LocalDateTime;

/* =============================================================================
   DTO : NotificationResponseDTO
   ---------------------------------------------------------------------------
   Description :
       Représente la donnée renvoyée au front-end pour les notifications
       d’un utilisateur ou après création/mise à jour.
       Contient toutes les informations nécessaires à l’affichage et au tri.

   Objectif CDA / Jury :
       - Documentation complète pour chaque champ
       - Aligné sur le dictionnaire NOTIFICATION MVP
       - Enum exposés côté JSON
       - Champs système inclus
   =============================================================================
*/
public class NotificationResponseDTO {

    // -------------------------------------------------------------------------
    // IDENTIFIANTS
    // -------------------------------------------------------------------------

    /** Identifiant unique de la notification (PK) */
    private Integer idNotification;

    /** Identifiant de l’utilisateur destinataire (FK) */
    private Integer idUser;

    // -------------------------------------------------------------------------
    // CONTENU
    // -------------------------------------------------------------------------

    /** Titre affiché de la notification (max 150 caractères) */
    private String titleNotification;

    /** Message principal de la notification (TEXT) */
    private String messageNotification;

    /** Lien cible associé à la notification (optionnel) */
    private String targetLinkNotification;

    // -------------------------------------------------------------------------
    // MÉTADONNÉES
    // -------------------------------------------------------------------------

    /** Indique si la notification a été lue par l’utilisateur */
    private Boolean readNotification;

    /** Date et heure de création de la notification */
    private LocalDateTime dateNotification;

    /** Type de notification (OVERDUE, REMINDER, RETURN, SYSTEM, CONTACT) */
    private NotificationType typeNotification;

    /** Catégorie de notification (INFO, ALERT, TASK, etc.) */
    private NotificationCategory categoryNotification;

    /** Niveau de priorité de la notification (optionnel, ex : HIGH, MEDIUM, LOW) */
    private String priorityNotification;

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------

    public Integer getIdNotification() {
        return idNotification;
    }

    public void setIdNotification(Integer idNotification) {
        this.idNotification = idNotification;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getTitleNotification() {
        return titleNotification;
    }

    public void setTitleNotification(String titleNotification) {
        this.titleNotification = titleNotification;
    }

    public String getMessageNotification() {
        return messageNotification;
    }

    public void setMessageNotification(String messageNotification) {
        this.messageNotification = messageNotification;
    }

    public String getTargetLinkNotification() {
        return targetLinkNotification;
    }

    public void setTargetLinkNotification(String targetLinkNotification) {
        this.targetLinkNotification = targetLinkNotification;
    }

    public Boolean getReadNotification() {
        return readNotification;
    }

    public void setReadNotification(Boolean readNotification) {
        this.readNotification = readNotification;
    }

    public LocalDateTime getDateNotification() {
        return dateNotification;
    }

    public void setDateNotification(LocalDateTime dateNotification) {
        this.dateNotification = dateNotification;
    }

    public NotificationType getTypeNotification() {
        return typeNotification;
    }

    public void setTypeNotification(NotificationType typeNotification) {
        this.typeNotification = typeNotification;
    }

    public NotificationCategory getCategoryNotification() {
        return categoryNotification;
    }

    public void setCategoryNotification(NotificationCategory categoryNotification) {
        this.categoryNotification = categoryNotification;
    }

    public String getPriorityNotification() {
        return priorityNotification;
    }

    public void setPriorityNotification(String priorityNotification) {
        this.priorityNotification = priorityNotification;
    }
}