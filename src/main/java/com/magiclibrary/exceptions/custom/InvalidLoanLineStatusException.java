package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : INVALID LOAN_LINE STATUS
 * =============================================================================
 *  Exception levée lorsqu'un statut de ligne d’emprunt est invalide ou non
 *  autorisé selon les règles métier MagicLibrary :
 *
 *      - statut inconnu
 *      - tentative de modification non autorisée
 *
 *  Objectifs :
 *      - garantir une gestion propre des statuts ;
 *      - éviter d'accepter des valeurs dangereuses ;
 *      - renvoyer un message clair au client REST.
 *
 *  Génère un code HTTP 400.
 * =============================================================================
 */
public class InvalidLoanLineStatusException extends RuntimeException {

    private final String message;
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message description de l’erreur métier
     */
    public InvalidLoanLineStatusException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Méthode utilitaire standardisée pour un statut invalide.
     *
     * @param status statut reçu
     * @return exception formatée
     */
    public static InvalidLoanLineStatusException forStatus(String status) {
        return new InvalidLoanLineStatusException(
                "Le statut '" + status + "' est invalide pour une ligne d’emprunt."
        );
    }

    @Override
    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
