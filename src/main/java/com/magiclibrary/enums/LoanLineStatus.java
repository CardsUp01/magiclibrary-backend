package com.magiclibrary.enums;

/**
 * Référentiel des statuts possibles d'une ligne d'emprunt.
 *
 * Ces valeurs permettent de suivre l'état individuel
 * d'un objet au sein d'un emprunt.
 */
public enum LoanLineStatus {

    ACTIVE("En cours"),
    RETURNED("Restitué"),
    LOST("Perdu");

    private final String label;

    LoanLineStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}