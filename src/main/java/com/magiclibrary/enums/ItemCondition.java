package com.magiclibrary.enums;

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