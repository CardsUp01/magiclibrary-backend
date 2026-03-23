package com.magiclibrary.exceptions.custom;

/**
 * =============================================================================
 *  EXCEPTION : LoanAlreadyReturnedException
 * =============================================================================
 *  Exception métier déclenchée lorsqu’une opération tente de modifier ou
 *  restituer un emprunt qui est déjà marqué comme restitué :
 *
 *      - returned_loan = 1
 *
 *  Cette erreur correspond au statut HTTP 409 (Conflict) dans l’endpoint :
 *
 *      - PUT /loans/{id}/return
 *
 *  Utilisée exclusivement dans la couche Service, elle permet d’appliquer
 *  les règles métiers du MVP MagicLibrary :
 *      - un emprunt restitué ne peut plus être modifié ;
 *      - aucune LOAN_LINE ne peut y être ajoutée ;
 *      - aucune nouvelle restitution n’est possible.
 *
 *  Cette classe respecte le style des exceptions déjà validées dans :
 *  com.magiclibrary.exceptions.custom
 * =============================================================================
 */
public class LoanAlreadyReturnedException extends RuntimeException {

    /**
     * Constructeur principal.
     *
     * @param message message explicatif retourné jusqu’au client REST.
     */
    public LoanAlreadyReturnedException(String message) {
        super(message);
    }
}
