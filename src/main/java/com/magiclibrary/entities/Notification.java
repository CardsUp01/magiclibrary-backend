package com.magiclibrary.entities;

import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/* ============================================================================
   ENTITY : Notification
   ---------------------------------------------------------------------------
   Entité représentant une notification envoyée à un utilisateur dans
   l’application MagicLibrary.

   Elle contient :
   - le destinataire de la notification ;
   - l'expéditeur humain éventuel ;
   - les informations métier : titre, message, type, catégorie ;
   - les métadonnées système : date, lecture, priorité ;
   - l'état d'archivage personnel du destinataire.

   PRINCIPES DE TRAÇABILITÉ
   ---------------------------------------------------------------------------
   Une notification possède toujours un destinataire.

   L'expéditeur humain reste facultatif :
   - notification manuelle envoyée par un ADMIN :
       sentByUser = administrateur expéditeur ;
   - notification système automatique :
       sentByUser = null ;
   - anciennes notifications créées avant l'ajout de la traçabilité :
       sentByUser = null.

   PRINCIPES D'ARCHIVAGE
   ---------------------------------------------------------------------------
   L'archivage est strictement logique.

   Une notification archivée :
   - reste présente en base ;
   - reste disponible dans l'historique des notifications envoyées ;
   - disparaît uniquement de la boîte "Reçues" du destinataire ;
   - peut être restaurée ultérieurement.

   Aucun DELETE physique n'est nécessaire pour ce fonctionnement.
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
    private Integer idNotification;
    // Clé primaire auto-incrémentée.

    // -------------------------------------------------------------------------
    // UTILISATEUR DESTINATAIRE
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;
    /*
     * Destinataire obligatoire de la notification.
     *
     * Cette relation détermine notamment :
     * - dans quelle boîte "Reçues" la notification apparaît ;
     * - quel utilisateur peut la marquer comme lue ;
     * - quel utilisateur peut l'archiver ou la restaurer.
     */

    // -------------------------------------------------------------------------
    // UTILISATEUR EXPÉDITEUR ÉVENTUEL
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_user_id")
    private User sentByUser;
    /*
     * Expéditeur humain éventuel de la notification.
     *
     * Ce champ est volontairement nullable :
     *
     * - notification créée manuellement par un ADMIN :
     *       sentByUser = administrateur ayant effectué l'envoi ;
     *
     * - notification automatique générée par MagicLibrary :
     *       sentByUser = null ;
     *
     * - notification historique créée avant l'introduction
     *   de cette traçabilité :
     *       sentByUser = null.
     *
     * Cette relation permettra de construire l'historique partagé
     * "Notifications envoyées" de l'équipe administrative.
     */

    // -------------------------------------------------------------------------
    // CONTENU MÉTIER
    // -------------------------------------------------------------------------

    @Column(name = "title_notification", nullable = false, length = 150)
    private String titleNotification;
    // Titre de la notification, obligatoire, max 150 caractères.

    @Column(name = "message_notification", nullable = false, columnDefinition = "TEXT")
    private String messageNotification;
    // Message de la notification, obligatoire, type TEXT.

    @Column(name = "target_link_notification", length = 255)
    private String targetLinkNotification;
    // Lien cible éventuel, facultatif, max 255 caractères.

    // -------------------------------------------------------------------------
    // ÉTAT DE LECTURE
    // -------------------------------------------------------------------------

    @Column(name = "read_notification", nullable = false)
    private Boolean readNotification = false;
    /*
     * Indique si la notification a été lue par son destinataire.
     *
     * Seul le destinataire doit pouvoir modifier cet état.
     * L'expéditeur pourra uniquement consulter cette information
     * depuis l'historique des notifications envoyées.
     */

    // -------------------------------------------------------------------------
    // DATE DE CRÉATION
    // -------------------------------------------------------------------------

    @Column(name = "date_notification", nullable = false)
    private LocalDateTime dateNotification;
    // Date de création, obligatoire, générée côté backend.

    // -------------------------------------------------------------------------
    // ARCHIVAGE LOGIQUE CÔTÉ DESTINATAIRE
    // -------------------------------------------------------------------------

    @Column(name = "archived_by_recipient", nullable = false)
    private Boolean archivedByRecipient = false;
    /*
     * Indique si le destinataire a archivé cette notification.
     *
     * false :
     *     notification visible dans "Reçues".
     *
     * true :
     *     notification visible dans "Archivées".
     *
     * IMPORTANT :
     * cet état ne doit jamais supprimer la notification de l'historique
     * administratif "Envoyées".
     */

    @Column(name = "archived_at_by_recipient")
    private LocalDateTime archivedAtByRecipient;
    /*
     * Date et heure du dernier archivage par le destinataire.
     *
     * Valeur null lorsque la notification n'est pas archivée.
     *
     * Lors d'une restauration, cette valeur devra être remise à null
     * par le service métier.
     */

    // -------------------------------------------------------------------------
    // ENUMS : TYPE & CATÉGORIE
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false, length = 50)
    private NotificationType typeNotification;
    // Type de notification : OVERDUE, REMINDER, RETURN, SYSTEM, CONTACT...

    @Enumerated(EnumType.STRING)
    @Column(name = "category_notification", nullable = false, length = 50)
    private NotificationCategory categoryNotification;
    // Catégorie interne : SYSTEM, USER_ACTION...

    // -------------------------------------------------------------------------
    // PRIORITÉ OPTIONNELLE
    // -------------------------------------------------------------------------

    @Column(name = "priority_notification", length = 20)
    private String priorityNotification;
    // Priorité éventuelle : LOW, MEDIUM, HIGH...

    // -------------------------------------------------------------------------
    // MARQUEUR TECHNIQUE DE DÉMONSTRATION
    // -------------------------------------------------------------------------

    /**
     * Code de scénario de démonstration associé à la notification.
     *
     * Ce champ permet d'identifier les notifications recréables de démonstration
     * sans dépendre d'un titre, d'un message, d'un utilisateur, d'un statut métier
     * ou d'un identifiant technique.
     *
     * Les notifications réelles conservent une valeur null.
     */
    @Column(name = "demo_scenario_code", length = 150)
    private String demoScenarioCode;

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR PAR DÉFAUT
    // -------------------------------------------------------------------------

    public Notification() {
        // Requis par JPA.
    }

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR COMPLET EXISTANT
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

        /*
         * Les champs système sont initialisés ici pour conserver
         * le comportement historique du constructeur.
         *
         * sentByUser reste volontairement null :
         * l'identité de l'expéditeur doit être déterminée exclusivement
         * par le backend sécurisé, jamais par le constructeur métier générique.
         */
        this.readNotification = false;
        this.archivedByRecipient = false;
        this.archivedAtByRecipient = null;
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

        /*
         * Toute nouvelle notification est active dans la boîte du destinataire
         * tant qu'une opération métier explicite ne l'archive pas.
         */
        if (this.archivedByRecipient == null) {
            this.archivedByRecipient = false;
        }

        /*
         * Une notification nouvellement créée et non archivée
         * ne possède logiquement aucune date d'archivage.
         */
        if (!Boolean.TRUE.equals(this.archivedByRecipient)) {
            this.archivedAtByRecipient = null;
        }
    }

    @PreUpdate
    private void onUpdate() {

        if (this.readNotification == null) {
            this.readNotification = false;
        }

        if (this.archivedByRecipient == null) {
            this.archivedByRecipient = false;
        }

        /*
         * Protection de cohérence minimale :
         * une notification non archivée ne doit pas conserver
         * une ancienne date d'archivage.
         *
         * La date d'archivage elle-même sera définie explicitement
         * par le service métier lors de l'action "Archiver".
         */
        if (!Boolean.TRUE.equals(this.archivedByRecipient)) {
            this.archivedAtByRecipient = null;
        }
    }

    // -------------------------------------------------------------------------
    // GETTERS / SETTERS
    // -------------------------------------------------------------------------

    public Integer getIdNotification() {
        return idNotification;
    }

    public void setIdNotification(Integer idNotification) {
        this.idNotification = idNotification;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getSentByUser() {
        return sentByUser;
    }

    public void setSentByUser(User sentByUser) {
        this.sentByUser = sentByUser;
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

    public Boolean getArchivedByRecipient() {
        return archivedByRecipient;
    }

    public void setArchivedByRecipient(Boolean archivedByRecipient) {
        this.archivedByRecipient = archivedByRecipient;
    }

    public LocalDateTime getArchivedAtByRecipient() {
        return archivedAtByRecipient;
    }

    public void setArchivedAtByRecipient(LocalDateTime archivedAtByRecipient) {
        this.archivedAtByRecipient = archivedAtByRecipient;
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

    public String getDemoScenarioCode() {
        return demoScenarioCode;
    }

    public void setDemoScenarioCode(String demoScenarioCode) {
        this.demoScenarioCode = demoScenarioCode;
    }
}