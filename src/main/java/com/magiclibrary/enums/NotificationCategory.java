package com.magiclibrary.enums;

/* ============================================================================
   ENUM : NotificationCategory
   ---------------------------------------------------------------------------
   Définition :
   Catégorisation fonctionnelle des notifications permettant le regroupement,
   le filtrage métier et la classification dans l’interface utilisateur.
   Conformité dictionnaire :
   Exemple : RETOUR, REMPLACEMENT, RAPPEL, CONTACT, SYSTEM.
   ============================================================================ */
public enum NotificationCategory {

    /* ------------------------------------------------------------------------
       Catégorie liée au retour d’un objet.
       ------------------------------------------------------------------------ */
    RETOUR("Retour"),

    /* ------------------------------------------------------------------------
       Catégorie couvrant les notifications de remplacement ou d’échange.
       ------------------------------------------------------------------------ */
    REMPLACEMENT("Remplacement"),

    /* ------------------------------------------------------------------------
       Catégorie correspondant aux rappels temporisés.
       ------------------------------------------------------------------------ */
    RAPPEL("Rappel"),

    /* ------------------------------------------------------------------------
       Catégorie associée au module CONTACT.
       ------------------------------------------------------------------------ */
    CONTACT("Contact"),

    /* ------------------------------------------------------------------------
       Catégorie interne générée automatiquement par le système.
       ------------------------------------------------------------------------ */
    SYSTEM("Système");

    private final String label;

    NotificationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}