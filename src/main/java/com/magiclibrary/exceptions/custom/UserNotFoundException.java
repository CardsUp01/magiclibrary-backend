package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : USER NOT FOUND
 * =============================================================================
 *  Cette exception est levée lorsqu'une recherche d’utilisateur échoue :
 *
 *      - ID inexistant
 *      - email introuvable
 *      - utilisateur supprimé/inactif (selon règles futures)
 *
 *  Rôle :
 *      - clarifier la logique métier ;
 *      - distinguer une erreur fonctionnelle d’une erreur technique ;
 *      - permettre une gestion fine dans GlobalExceptionHandler ;
 *      - garantir une réponse REST propre pour le client.
 *
 *  IMPORTANT :
 *      Cette exception ne doit jamais exposer de détails techniques internes.
 * =============================================================================
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Message explicite destiné au client / front-end.
     * Exemple : "Utilisateur introuvable avec l’ID 12"
     */
    private final String message;

    /**
     * Timestamp utile pour logs avancés (pas renvoyé au client).
     */
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal, recevant un message clair.
     *
     * @param message description fonctionnelle du problème
     */
    public UserNotFoundException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Retourne le message fonctionnel de l’exception.
     *
     * @return message métier
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Timestamp interne de l’exception — utile pour les logs serveur.
     *
     * @return date/heure de création de l’exception
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
