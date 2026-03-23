package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : ROLE NOT FOUND
 * =============================================================================
 *  Cette exception est levée lorsqu'une opération nécessite l’existence
 *  d’un rôle précis, mais que celui-ci n’a pas été trouvé en base.
 *
 *  Cas typiques :
 *      - rôle MEMBRE introuvable lors de la création d’un utilisateur (US-02)
 *      - rôle ADMIN manquant dans CustomUserDetailsService
 *      - incohérence ou base partiellement initialisée
 *
 *  Importance :
 *      - permet de distinguer clairement l’erreur "rôle absent"
 *        des erreurs plus générales comme IllegalStateException ;
 *      - améliore la qualité du diagnostic serveur ;
 *      - garantit des réponses REST propres via GlobalExceptionHandler.
 *
 *  Ce fichier ne doit jamais révéler de détails internes ou techniques.
 * =============================================================================
 */
public class RoleNotFoundException extends RuntimeException {

    /**
     * Message métier destiné au client et aux logs.
     */
    private final String message;

    /**
     * Date et heure de la création de l’exception (utile pour logs avancés).
     */
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message description fonctionnelle de l’erreur
     */
    public RoleNotFoundException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * @return message fonctionnel destiné au client
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @return timestamp interne utilisé pour le suivi de l’erreur
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
