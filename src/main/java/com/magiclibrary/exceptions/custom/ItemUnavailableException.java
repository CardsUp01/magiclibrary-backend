package com.magiclibrary.exceptions.custom;

/**
 * ============================================================================
 *  EXCEPTION MÉTIER : ItemUnavailableException
 * ============================================================================
 *  Exception levée lorsqu’un emprunt est demandé sur un objet du catalogue
 *  numérique qui n’est pas disponible (available_item = false).
 *
 *  Cette exception est utilisée exclusivement dans la logique LOAN / LOAN_LINE
 *  afin de garantir les règles métier du MVP MagicLibrary :
 *      - Impossible d’emprunter un objet indisponible ;
 *      - Message explicite envoyé au contrôleur, puis au client REST.
 * ============================================================================
 */
public class ItemUnavailableException extends RuntimeException {

    /**
     * Constructeur générique avec message personnalisé.
     *
     * @param message message explicatif de l’erreur.
     */
    public ItemUnavailableException(String message) {
        super(message);
    }

    /**
     * Méthode utilitaire générant un message standardisé.
     *
     * @param itemId identifiant de l’objet indisponible.
     * @return instance de l’exception avec message formaté.
     */
    public static ItemUnavailableException forItem(Integer itemId) {
        return new ItemUnavailableException(
                "L’objet " + itemId + " n’est pas disponible pour l’emprunt."
        );
    }
}
