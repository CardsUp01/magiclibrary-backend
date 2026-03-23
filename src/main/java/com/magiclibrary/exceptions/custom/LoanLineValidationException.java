package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : LOAN_LINE VALIDATION
 * =============================================================================
 *  Levée lorsqu'une règle métier LOAN_LINE est violée :
 *
 *      - quantité invalide
 *      - incohérence entre données fournies
 *      - problème sur le format ou la logique métier
 *
 *  Objectifs :
 *      - fournir une erreur claire côté API ;
 *      - garantir l'intégrité des données ;
 *      - éviter les erreurs techniques non contrôlées.
 *
 *  Génère un code HTTP 400.
 * =============================================================================
 */
public class LoanLineValidationException extends RuntimeException {

    private final String message;
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message description fonctionnelle du problème
     */
    public LoanLineValidationException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Méthode utilitaire permettant de formater un message standardisé.
     *
     * @param field champ concerné
     * @param detail détail de l’erreur
     * @return exception formatée
     */
    public static LoanLineValidationException forField(String field, String detail) {
        return new LoanLineValidationException(
                "Erreur de validation sur le champ '" + field + "' : " + detail
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
