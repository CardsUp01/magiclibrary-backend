package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * =============================================================================
 *  EXCEPTION MÉTIER : LOAN_LINE NOT FOUND
 * =============================================================================
 *  Exception levée lorsqu’une ligne d’emprunt est introuvable en base :
 *
 *      - ID inexistant
 *      - ligne supprimée ou non accessible selon règles futures
 *
 *  Objectifs :
 *      - clarifier l’erreur fonctionnelle ;
 *      - éviter d’exposer des détails techniques ;
 *      - permettre un traitement propre dans GlobalExceptionHandler ;
 *      - standardiser les messages d'erreur pour l’API REST.
 *
 *  Génère un code HTTP 404.
 * =============================================================================
 */
public class LoanLineNotFoundException extends RuntimeException {

    private final String message;
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message description claire de l’erreur
     */
    public LoanLineNotFoundException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Méthode utilitaire standardisée pour un ID de ligne d'emprunt.
     *
     * @param idLoanLine identifiant recherché
     * @return exception formatée
     */
    public static LoanLineNotFoundException forId(Integer idLoanLine) {
        return new LoanLineNotFoundException(
                "Aucune ligne d’emprunt trouvée avec l’identifiant " + idLoanLine + "."
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
