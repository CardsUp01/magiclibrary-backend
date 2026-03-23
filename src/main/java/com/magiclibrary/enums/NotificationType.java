package com.magiclibrary.enums;

/* ============================================================================
   ENUM : NotificationType
   ---------------------------------------------------------------------------
   Définition :
   Liste des types internes de notification utilisés par l’application pour
   déclencher ou identifier un traitement automatique. Les valeurs sont
   normalisées afin d’éviter toute ambiguïté ou variation d’écriture.
   Conformité dictionnaire :
   Exemple : OVERDUE, REMINDER, RETURN, SYSTEM, CONTACT.
   ============================================================================ */
public enum NotificationType {

    /* ------------------------------------------------------------------------
       Notification indiquant un retard (ex : emprunt non rendu).
       ------------------------------------------------------------------------ */
    OVERDUE("Retard"),

    /* ------------------------------------------------------------------------
       Notification destinée à rappeler un événement important.
       ------------------------------------------------------------------------ */
    REMINDER("Rappel"),

    /* ------------------------------------------------------------------------
       Notification liée à un retour d’objet.
       ------------------------------------------------------------------------ */
    RETURN("Retour"),

    /* ------------------------------------------------------------------------
       Notification générée automatiquement par le système.
       ------------------------------------------------------------------------ */
    SYSTEM("Système"),

    /* ------------------------------------------------------------------------
       Notification résultant d’une action liée au module CONTACT.
       ------------------------------------------------------------------------ */
    CONTACT("Contact");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}