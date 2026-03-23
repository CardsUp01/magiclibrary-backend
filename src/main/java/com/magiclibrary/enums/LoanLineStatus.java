package com.magiclibrary.enums;

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