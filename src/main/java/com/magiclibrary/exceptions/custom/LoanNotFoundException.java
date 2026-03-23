package com.magiclibrary.exceptions.custom;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

/**
 * ============================================================================
 *  EXCEPTION MÉTIER : LoanNotFoundException
 * ============================================================================
 *  Exception levée lorsqu'une recherche d’emprunt (LOAN) échoue :
 *
 *      - ID inexistant ;
 *      - emprunt supprimé / non accessible (selon règles futures) ;
 *
 *  Rôle :
 *      - distinguer une erreur fonctionnelle d’une erreur technique ;
 *      - garantir une réponse REST propre et cohérente ;
 *      - s’intégrer parfaitement au GlobalExceptionHandler.
 * ============================================================================
 */
public class LoanNotFoundException extends RuntimeException {

    /**
     * Message métier destiné au client.
     */
    private final String message;

    /**
     * Timestamp interne pour logs avancés (non exposé au client).
     */
    private final LocalDateTime timestamp;

    /**
     * Constructeur principal.
     *
     * @param message description fonctionnelle explicite.
     */
    public LoanNotFoundException(String message) {
        super(message);
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Message fonctionnel propre.
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Timestamp interne utile pour les logs serveur.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Méthode utilitaire standardisée.
     *
     * @param loanId identifiant d’emprunt inexistant.
     * @return instance préparée de l’exception.
     */
    public static LoanNotFoundException forId(Integer loanId) {
        return new LoanNotFoundException(
                "Aucun emprunt trouvé avec l’identifiant " + loanId + "."
        );
    }
}
