package com.magiclibrary.exceptions.custom;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : UnauthorizedException
 * =============================================================================
 *  Exception levée lorsqu’un utilisateur tente d'accéder à une ressource
 *  nécessitant une authentification, mais :
 *
 *      - aucun JWT n’a été fourni ;
 *      - le JWT est invalide (signature incorrecte) ;
 *      - le JWT est expiré ;
 *      - le format du token est incorrect ;
 *      - l'utilisateur n'a pas pu être authentifié.
 *
 *  Cette exception correspond au statut HTTP 401 Unauthorized
 *  et ne doit JAMAIS être utilisée pour représenter une action interdite
 *  (dans ce cas, utiliser ForbiddenException).
 *
 *  Avantages :
 *      - distinction claire entre authentification et autorisation ;
 *      - améliore la lisibilité métier et technique du code ;
 *      - facilite le traitement des erreurs dans GlobalExceptionHandler.
 * =============================================================================
 */
public class UnauthorizedException extends RuntimeException {

    // -------------------------------------------------------------------------
    // CONSTANTES
    // -------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    /**
     * Constructeur avec message personnalisé.
     *
     * @param message message expliquant la raison du refus d'authentification
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause technique.
     *
     * @param message message décrivant le contexte de l’erreur
     * @param cause   exception d’origine (technique)
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}