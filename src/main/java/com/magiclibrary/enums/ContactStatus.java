package com.magiclibrary.enums;

/**
 * =============================================================================
 * ENUM : ContactStatus
 * =============================================================================
 * Représente les statuts métier d’un message de contact dans MagicLibrary.
 *
 * Rôle :
 *      - centraliser les valeurs techniques de statut utilisées côté backend
 *      - fournir un libellé français prêt à afficher côté SSR ou API
 *      - éviter les chaînes métier en dur dans les services et templates
 *
 * Remarques :
 *      - la valeur technique reste stable : NEW, ANSWERED
 *      - le libellé est destiné à l’affichage utilisateur : Nouveau, Répondu
 *      - une méthode utilitaire permet de convertir une valeur String stockée
 *        en base MongoDB vers l’enum correspondant
 */
public enum ContactStatus {

    NEW("Nouveau"),
    ANSWERED("Répondu");

    private final String label;

    ContactStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Convertit une valeur technique String en enum ContactStatus.
     *
     * @param value valeur technique attendue (ex : NEW, ANSWERED)
     * @return enum correspondant
     * @throws IllegalArgumentException si la valeur est nulle, vide ou inconnue
     */
    public static ContactStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le statut de contact est nul ou vide.");
        }

        for (ContactStatus status : ContactStatus.values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Statut de contact inconnu : " + value);
    }
}