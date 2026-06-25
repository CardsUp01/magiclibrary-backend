package com.magiclibrary.enums;

/**
 * Référentiel des origines possibles d'un emprunt.
 *
 * Chaque valeur associe un code technique stocké dans les données
 * à un libellé destiné à l'affichage dans l'interface utilisateur.
 */
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

    /*
     * Retourne le libellé correspondant à une origine d'emprunt.
     * Si le code n'est pas reconnu, la valeur reçue est renvoyée telle quelle.
     */
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