package com.magiclibrary.exceptions.custom;


/**
 * =============================================================================
 *  EXCEPTION METIER : InvalidJwtException
 * =============================================================================
 *  Exception spécifique à la sécurité JWT de l’application MagicLibrary.
 *
 *  RÔLE :
 *      - Signaler qu’un token JWT est invalide ou ne peut pas être utilisé
 *        pour authentifier l’utilisateur.
 *
 *  CAS D’UTILISATION TYPIQUES :
 *      - token signé avec une clé incorrecte,
 *      - token expiré,
 *      - token mal formé (structure invalide),
 *      - token dont les claims obligatoires sont absents ou incohérents.
 *
 *  Cette exception est destinée à être :
 *      - levée par la couche de sécurité (filtre JWT, utilitaire JwtUtil),
 *      - interceptée par un gestionnaire global d’exceptions
 *        (GlobalExceptionHandler),
 *      - traduite en réponse HTTP standardisée (JSON) pour le client frontal.
 *
 *  IMPORTANT :
 *      - Il s’agit d’une RuntimeException afin de simplifier la propagation
 *        dans la chaîne de filtres Spring Security.
 *      - Elle ne dépend d’aucune entité métier (USER, ROLE, etc.).
 * =============================================================================
 */
public class InvalidJwtException extends RuntimeException {

    // -------------------------------------------------------------------------
    // CONSTANTES
    // -------------------------------------------------------------------------

    /**
     * Identifiant de version pour la sérialisation.
     * Permet de garantir la compatibilité lors d’un éventuel transport
     * de l’exception (logs, mécanismes distribués).
     */
    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    /**
     * Constructeur par défaut avec message générique.
     * Utile lorsque le détail de l’erreur n’a pas besoin d’être exposé.
     */
    public InvalidJwtException() {
        super("Token JWT invalide ou non utilisable.");
    }

    /**
     * Constructeur avec message personnalisé.
     *
     * @param message message décrivant la cause fonctionnelle ou technique
     */
    public InvalidJwtException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause technique.
     * Permet de chaîner une exception d’origine (ex. JwtException de la
     * bibliothèque io.jsonwebtoken) tout en exposant une exception métier
     * propre à MagicLibrary.
     *
     * @param message message décrivant le contexte de l’erreur
     * @param cause exception d’origine (technique)
     */
    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}