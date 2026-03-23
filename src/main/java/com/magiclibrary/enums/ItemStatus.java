package com.magiclibrary.enums;

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