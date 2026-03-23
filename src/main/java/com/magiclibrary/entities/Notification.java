package com.magiclibrary.entities;

import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/* ============================================================================
   ENTITY : Notification
   ---------------------------------------------------------------------------
   Entité représentant une notification envoyée à un utilisateur dans l’application
   MagicLibrary. Elle contient les informations métier (titre, message, type, catégorie)
   ainsi que les métadonnées système (date, lecture, priorité).
============================================================================ */
@Entity
@Table(name = "notification")
public class Notification {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE
    // -------------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification", nullable = false)
    private Integer idNotification; // Clé primaire idNotification, identifiant unique auto-incrémenté

    // -------------------------------------------------------------------------
    // UTILISATEUR DESTINATAIRE
    // -------------------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user; // FK, NOT NULL, destinataire de la notification, relation ManyToOne → User(idUser)

    // -------------------------------------------------------------------------
    // CONTENU MÉTIER
    // -------------------------------------------------------------------------
    @Column(name = "title_notification", nullable = false, length = 150)
    private String titleNotification; // Titre de la notification, obligatoire, max 150 caractères

    @Column(name = "message_notification", nullable = false, columnDefinition = "TEXT")
    private String messageNotification; // Message de la notification, obligatoire, type TEXT

    @Column(name = "target_link_notification", length = 255)
    private String targetLinkNotification; // Lien cible éventuel, facultatif, max 255 caractères

    @Column(name = "read_notification", nullable = false)
    private Boolean readNotification = false; // Flag indiquant si la notification a été lue, défaut false

    @Column(name = "date_notification", nullable = false)
    private LocalDateTime dateNotification; // Date de création, obligatoire, générée côté backend

    // -------------------------------------------------------------------------
    // ENUMS : TYPE & CATÉGORIE
    // -------------------------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false, length = 50)
    private NotificationType typeNotification; // Type de notification (OVERDUE, REMINDER, RETURN, SYSTEM, CONTACT)

    @Enumerated(EnumType.STRING)
    @Column(name = "category_notification", nullable = false, length = 50)
    private NotificationCategory categoryNotification; // Catégorie interne (ex: SYSTEM, USER_ACTION)

    // -------------------------------------------------------------------------
    // PRIORITÉ OPTIONNELLE
    // -------------------------------------------------------------------------
    @Column(name = "priority_notification", length = 20)
    private String priorityNotification; // Priorité (ex : LOW, MEDIUM, HIGH), facultatif, max 20 caractères

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR PAR DÉFAUT
    // -------------------------------------------------------------------------
    public Notification() {
        // Requis par JPA
    }

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR COMPLET
    // -------------------------------------------------------------------------
    public Notification(
            User user,
            String titleNotification,
            String messageNotification,
            String targetLinkNotification,
            NotificationType typeNotification,
            NotificationCategory categoryNotification,
            String priorityNotification
    ) {
        this.user = user;
        this.titleNotification = titleNotification;
        this.messageNotification = messageNotification;
        this.targetLinkNotification = targetLinkNotification;
        this.typeNotification = typeNotification;
        this.categoryNotification = categoryNotification;
        this.priorityNotification = priorityNotification;
        this.readNotification = false;
        this.dateNotification = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // HOOKS JPA : CHAMPS SYSTÈME
    // -------------------------------------------------------------------------
    @PrePersist
    private void onCreate() {
        if (this.dateNotification == null) {
            this.dateNotification = LocalDateTime.now();
        }
        if (this.readNotification == null) {
            this.readNotification = false;
        }
    }

    @PreUpdate
    private void onUpdate() {
        if (this.readNotification == null) {
            this.readNotification = false;
        }
    }

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------
    public Integer getIdNotification() { return idNotification; }
    public void setIdNotification(Integer idNotification) { this.idNotification = idNotification; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitleNotification() { return titleNotification; }
    public void setTitleNotification(String titleNotification) { this.titleNotification = titleNotification; }

    public String getMessageNotification() { return messageNotification; }
    public void setMessageNotification(String messageNotification) { this.messageNotification = messageNotification; }

    public String getTargetLinkNotification() { return targetLinkNotification; }
    public void setTargetLinkNotification(String targetLinkNotification) { this.targetLinkNotification = targetLinkNotification; }

    public Boolean getReadNotification() { return readNotification; }
    public void setReadNotification(Boolean readNotification) { this.readNotification = readNotification; }

    public LocalDateTime getDateNotification() { return dateNotification; }
    public void setDateNotification(LocalDateTime dateNotification) { this.dateNotification = dateNotification; }

    public NotificationType getTypeNotification() { return typeNotification; }
    public void setTypeNotification(NotificationType typeNotification) { this.typeNotification = typeNotification; }

    public NotificationCategory getCategoryNotification() { return categoryNotification; }
    public void setCategoryNotification(NotificationCategory categoryNotification) { this.categoryNotification = categoryNotification; }

    public String getPriorityNotification() { return priorityNotification; }
    public void setPriorityNotification(String priorityNotification) { this.priorityNotification = priorityNotification; }
}