package com.magiclibrary.enums;

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