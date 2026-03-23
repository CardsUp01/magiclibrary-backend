package com.magiclibrary.enums;

public enum LoanOrigin {

    USER("USER", "Membre"),
    ADMIN("ADMIN", "Administrateur"),
    SYSTEM("SYSTEM", "Système");

    private final String code;
    private final String label;

    LoanOrigin(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String code) {
        if (code == null) {
            return null;
        }

        String normalized = code.trim().toUpperCase();

        for (LoanOrigin origin : values()) {
            if (origin.code.equals(normalized)) {
                return origin.label;
            }
        }

        return code;
    }
}