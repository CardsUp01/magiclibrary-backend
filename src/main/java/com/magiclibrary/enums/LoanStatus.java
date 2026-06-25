package com.magiclibrary.enums;

/**
 * Référentiel des statuts métier possibles d'un emprunt.
 *
 * Ces valeurs permettent de suivre le cycle de vie
 * d'un emprunt au sein de l'application.
 */
public enum LoanStatus {

    ONGOING("En cours"),
    RETURNED("Restitué"),
    LATE("En retard");

    private final String label;

    LoanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}