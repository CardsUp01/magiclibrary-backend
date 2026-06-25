package com.magiclibrary.enums;

/**
 * Référentiel des états physiques possibles d'un objet
 * du catalogue numérique.
 *
 * Chaque valeur est associée à un libellé destiné à l'affichage
 * dans l'interface utilisateur.
 */
public enum ItemCondition {

    NEW("Neuf"),
    GOOD("Bon état"),
    USED("Usé"),
    DAMAGED("Endommagé");

    private final String label;

    ItemCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}