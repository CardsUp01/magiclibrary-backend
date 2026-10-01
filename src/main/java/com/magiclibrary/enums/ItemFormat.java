package com.magiclibrary.enums;

/**
 * =============================================================================
 * ENUM : ITEM FORMAT
 * =============================================================================
 * Référentiel des formats actuellement supportés par le catalogue MagicLibrary.
 *
 * Un format décrit la nature documentaire / matérielle de l'objet et ne doit
 * pas être confondu avec sa catégorie thématique.
 *
 * Exemples :
 * - catégorie : cartes
 * - format    : LIVRE
 *
 * Les valeurs actuellement présentes dans le catalogue réel sont :
 * - LIVRE ;
 * - DVD.
 *
 * Ce référentiel permet :
 * - d'éviter les variantes libres telles que "Livre", "livre" ou " LIVRE " ;
 * - d'afficher un libellé lisible dans les formulaires administratifs ;
 * - de conserver une valeur technique canonique en base ;
 * - de centraliser la validation des formats supportés.
 *
 * La colonne SQL format_item reste volontairement une chaîne de caractères.
 * L'introduction de cet enum ne nécessite donc aucune migration de schéma.
 *
 * Le format reste facultatif à ce stade :
 * une valeur null ou vide peut continuer à représenter un format non renseigné.
 * =============================================================================
 */
public enum ItemFormat {

    LIVRE("LIVRE", "Livre"),
    DVD("DVD", "DVD");

    private final String code;
    private final String label;

    ItemFormat(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * Retourne la valeur technique canonique destinée au stockage.
     *
     * @return code technique du format
     */
    public String getCode() {
        return code;
    }

    /**
     * Retourne le libellé destiné à l'interface utilisateur.
     *
     * @return libellé lisible du format
     */
    public String getLabel() {
        return label;
    }

    /**
     * Recherche un format à partir d'une valeur reçue.
     *
     * La comparaison :
     * - ignore les espaces situés avant et après la valeur ;
     * - ignore la casse.
     *
     * Exemples reconnus comme LIVRE :
     * - "LIVRE"
     * - "Livre"
     * - "livre"
     * - " LIVRE "
     *
     * Une valeur null, vide ou uniquement composée d'espaces retourne null.
     *
     * Une valeur non supportée retourne également null afin de permettre
     * à la couche métier de produire son propre message d'erreur fonctionnel.
     *
     * @param value valeur à analyser
     * @return format correspondant, ou null si absent / non reconnu
     */
    public static ItemFormat fromValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        for (ItemFormat format : values()) {

            if (format.code.equalsIgnoreCase(normalized)) {
                return format;
            }
        }

        return null;
    }

    /**
     * Retourne le code canonique correspondant à une valeur reconnue.
     *
     * Cette méthode permet notamment de transformer :
     *
     *     "Livre" -> "LIVRE"
     *
     * Une valeur absente ou non reconnue retourne null.
     *
     * @param value valeur à normaliser
     * @return code canonique ou null
     */
    public static String canonicalCodeOf(String value) {

        ItemFormat format = fromValue(value);

        return format != null
                ? format.getCode()
                : null;
    }

    /**
     * Indique si une valeur non vide correspond à un format supporté.
     *
     * Les valeurs nulles ou vides sont considérées comme valides ici puisque
     * le format reste facultatif. La présence obligatoire éventuelle du format
     * relève d'une autre règle métier.
     *
     * @param value valeur à contrôler
     * @return true si la valeur est absente ou correspond à un format supporté
     */
    public static boolean isSupported(String value) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return fromValue(value) != null;
    }
}