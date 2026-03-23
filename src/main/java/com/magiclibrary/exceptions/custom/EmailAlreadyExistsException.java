package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : EMAIL ALREADY EXISTS
 * =============================================================================
 *  Exception levée lorsqu'une tentative de création ou mise à jour d’un
 *  utilisateur échoue parce que l’adresse email fournie existe déjà
 *  dans la base de données.
 *
 *  Cas typiques :
 *      - création de compte membre (US-02)
 *      - mise à jour du profil (US-12) si tu ajoutes cette règle plus tard
 *
 *  Intérêt :
 *      - éviter l’utilisation d’IllegalStateException ;
 *      - fournir un message clair au client ;
 *      - permettre un traitement propre dans GlobalExceptionHandler.
 *
 *  Aucune information sensible n’est exposée dans cette exception.
 * =============================================================================
 */
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Message fonctionnel renvoyé côté API et enregistré dans les logs.
     */
    private final String message;

    /**
     * Date et heure de création de l’exception (utile pour audit interne).
     */
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message message métier clair décrivant le problème.
     */
    public EmailAlreadyExistsException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Retourne le message fonctionnel de l’exception.
     *
     * @return description de l’erreur métier
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Retourne la date de création de l’exception.
     *
     * @return timestamp interne
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
