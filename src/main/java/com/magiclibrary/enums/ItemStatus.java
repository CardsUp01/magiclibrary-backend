package com.magiclibrary.enums;

/**
 * Référentiel des statuts possibles d'un objet
 * du catalogue numérique.
 *
 * Ces valeurs décrivent la disponibilité métier
 * d'un objet au sein de la bibliothèque.
 */
public enum ItemStatus {

    AVAILABLE("Disponible"),
    UNAVAILABLE("Indisponible"),
    DAMAGED("Endommagé"),
    LOST("Perdu");

    private final String label;

    ItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}