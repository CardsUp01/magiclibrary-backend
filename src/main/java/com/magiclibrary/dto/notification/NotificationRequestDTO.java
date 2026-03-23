package com.magiclibrary.dto.notification;

import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* =============================================================================
   DTO : NotificationRequestDTO
   ---------------------------------------------------------------------------
   Description :
       Représente les données reçues lors de la création ou mise à jour
       d’une notification. Utilisé par l’Administrateur pour créer une notification
       et par l’utilisateur pour marquer une notification comme lue.

   Objectifs CDA / Jury :
       - Champs strictement conformes au dictionnaire NOTIFICATION MVP
       - Validation Bean Validation pour la cohérence front/back
       - Documentation complète pour chaque champ
   =============================================================================
*/
public class NotificationRequestDTO {

    // -------------------------------------------------------------------------
    // IDENTIFIANT UTILISATEUR CIBLE (ADMIN)
    // -------------------------------------------------------------------------

    /** Identifiant de l’utilisateur destinataire de la notification. Obligatoire pour ADMIN. */
    @NotNull(message = "L'identifiant utilisateur est obligatoire.")
    private Integer idUser;

    // -------------------------------------------------------------------------
    // CONTENU
    // -------------------------------------------------------------------------

    /** Titre de la notification, obligatoire, longueur 2-150 */
    @NotBlank(message = "Le titre de la notification est obligatoire.")
    @Size(min = 2, max = 150, message = "Le titre doit contenir entre 2 et 150 caractères.")
    private String titleNotification;

    /** Contenu détaillé de la notification, obligatoire, longueur 2-10 000 */
    @NotBlank(message = "Le message de la notification est obligatoire.")
    @Size(min = 2, max = 10_000, message = "Le message doit contenir entre 2 et 10 000 caractères.")
    private String messageNotification;

    // -------------------------------------------------------------------------
    // LIEN OPTIONNEL
    // -------------------------------------------------------------------------

    /** Lien interne associé à la notification (optionnel), max 255 caractères */
    @Size(max = 255, message = "Le lien ne peut dépasser 255 caractères.")
    private String targetLinkNotification;

    // -------------------------------------------------------------------------
    // ENUMS
    // -------------------------------------------------------------------------

    /** Type interne de notification (OVERDUE, REMINDER, RETURN, SYSTEM, CONTACT). Obligatoire */
    @NotNull(message = "Le type de notification est obligatoire.")
    private NotificationType typeNotification;

    /** Catégorie métier de notification (INFO, ALERT, TASK, etc.). Obligatoire */
    @NotNull(message = "La catégorie de notification est obligatoire.")
    private NotificationCategory categoryNotification;

    // -------------------------------------------------------------------------
    // PRIORITÉ
    // -------------------------------------------------------------------------

    /** Priorité optionnelle : texte libre, longueur 2-20 (ex : basse/normale/haute) */
    @Size(min = 2, max = 20, message = "La priorité doit contenir entre 2 et 20 caractères.")
    private String priorityNotification;

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------

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