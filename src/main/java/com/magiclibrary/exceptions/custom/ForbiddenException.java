package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : FORBIDDEN
 * =============================================================================
 *  Exception levée lorsqu’un utilisateur est authentifié mais ne possède pas
 *  les autorisations nécessaires pour accéder à une ressource ou exécuter
 *  une action.
 *
 *  Contexte typique :
 *      - un MEMBRE tente d’appeler un endpoint réservé à ADMIN ;
 *      - un utilisateur authentifié tente d’accéder aux données d’un autre ;
 *      - une action est refusée car les droits métier ne le permettent pas.
 *
 *  Cette exception correspond strictement au statut HTTP :
 *      403 Forbidden
 *
 *  Avantages :
 *      - distinction explicite avec UnauthorizedException (401) ;
 *      - améliore la lisibilité métier ;
 *      - facilite la gestion via GlobalExceptionHandler.
 * =============================================================================
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Message métier destiné au client ou aux logs.
     */
    private final String message;

    /**
     * Timestamp interne (utile en analyse technique).
     */
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message description claire de la raison du refus
     */
    public ForbiddenException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Retourne le message métier associé.
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Retourne la date/heure d’émission de cette exception.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
