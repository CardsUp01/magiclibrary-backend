package com.magiclibrary.enums;

public enum LanguageCode {

    FR("FR", "Français"),
    EN("EN", "Anglais"),
    ES("ES", "Espagnol");

    private final String code;
    private final String label;

    LanguageCode(String code, String label) {
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

        for (LanguageCode lc : values()) {
            if (lc.code.equals(normalized)) {
                return lc.label;
            }
        }

        return code;
    }
}