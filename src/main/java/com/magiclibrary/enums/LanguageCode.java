package com.magiclibrary.enums;

/**
 * Référentiel des langues supportées par le catalogue numérique.
 *
 * Chaque valeur associe un code normalisé utilisé dans les données
 * à un libellé destiné à l'affichage dans l'interface utilisateur.
 */
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

    /*
     * Retourne le libellé correspondant à un code langue.
     * Si le code n'est pas reconnu, la valeur reçue est renvoyée telle quelle.
     */
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