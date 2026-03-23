package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : INVALID CREDENTIALS
 * =============================================================================
 *  Cette exception est levée lorsqu'une tentative de connexion échoue en raison
 *  de données d’authentification incorrectes :
 *
 *      - email inconnu,
 *      - mot de passe incorrect,
 *      - combinaison email/mot de passe non valide.
 *
 *  Elle est utilisée spécifiquement dans :
 *      - AuthServiceImpl.login()
 *      - JwtAuthenticationFilter (éventuellement)
 *
 *  Avantages :
 *      - distingue les erreurs métier des erreurs techniques ;
 *      - permet une gestion centralisée dans GlobalExceptionHandler ;
 *      - renvoie au front une réponse claire, sans fuite de logique interne.
 *
 *  IMPORTANT :
 *      Le message de cette exception NE DOIT JAMAIS révéler
 *      si l’email ou le mot de passe est incorrect → règle de sécurité OWASP.
 * =============================================================================
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Message renvoyé au client et utilisé dans les logs.
     * Toujours générique pour éviter toute indication sur la validité de l’email.
     */
    private final String message;

    /**
     * Timestamp interne permettant l’audit et le suivi des incidents.
     */
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message message fonctionnel générique
     */
    public InvalidCredentialsException(String message) {
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
     * @return date et heure de création de l’exception
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
