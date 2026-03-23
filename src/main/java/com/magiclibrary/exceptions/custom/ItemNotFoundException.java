package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
/**
 * =============================================================================
 *  EXCEPTION : ItemNotFoundException
 * =============================================================================
 *  Exception métier levée lorsqu'un objet du catalogue numérique MagicLibrary
 *  est introuvable en base de données.
 *
 *  Cette exception est utilisée dans les cas suivants :
 *      - consultation d’un objet : GET /items/{id}
 *      - mise à jour d’un objet : PUT /items/{id}
 *      - suppression logique ou définitive : DELETE /items/{id}
 *
 *  Elle permet au service et au contrôleur d’exprimer clairement une situation
 *  d’erreur fonctionnelle (404 NOT FOUND), et s’intègre automatiquement dans le
 *  GlobalExceptionHandler afin d’offrir une réponse JSON cohérente et lisible
 *  pour le client.
 *
 *  Aucun détail technique interne n’est exposé : seul un message clair et
 *  exploitable est renvoyé à l'utilisateur.
 * =============================================================================
 */
public class ItemNotFoundException extends RuntimeException {

    /**
     * Constructeur principal permettant de préciser la nature de l’erreur.
     *
     * @param message message décrivant l'élément introuvable.
     */
    public ItemNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructeur pratique pour générer automatiquement
     * un message standard basé sur l'identifiant recherché.
     *
     * @param itemId identifiant de l'objet recherché.
     * @return nouvelle instance ItemNotFoundException
     */
    public static ItemNotFoundException forId(Integer itemId) {
        return new ItemNotFoundException(
                "L’objet avec l’identifiant " + itemId + " est introuvable dans le catalogue."
        );
    }
}
