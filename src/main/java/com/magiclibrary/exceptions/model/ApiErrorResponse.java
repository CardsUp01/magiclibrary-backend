package com.magiclibrary.exceptions.model;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// (Aucun import interne nécessaire)

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI (AJOUTÉS)
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * =============================================================================
 *  MODEL : API ERROR RESPONSE
 * =============================================================================
 *  Représente le format standard de réponse d’erreur renvoyé par l’API
 *  REST de l’application *MagicLibrary*.
 *
 *  Objectifs :
 *      - fournir un format homogène pour toutes les erreurs ;
 *      - faciliter le débogage côté client (Postman, Front-End) ;
 *      - éviter l’exposition des stack traces et messages techniques internes.
 *
 *  Champs principaux :
 *      - timestamp : date/heure de l’erreur côté serveur ;
 *      - status    : code HTTP (ex : 400, 404, 500) ;
 *      - error     : libellé Court (ex : "Bad Request", "Not Found") ;
 *      - message   : description claire de l’erreur ;
 *      - path      : URI de la requête à l’origine de l’erreur.
 *
 *  Cette classe est utilisée par GlobalExceptionHandler.
 * =============================================================================
 */
@Schema(description = "Structure standard de réponse d’erreur renvoyée par l’API MagicLibrary.")
public class ApiErrorResponse {

    // -------------------------------------------------------------------------
    // CHAMPS
    // -------------------------------------------------------------------------

    /**
     * Date et heure de survenue de l’erreur côté serveur.
     */
    @Schema(description = "Horodatage exact de l’erreur côté serveur.",
            example = "2025-12-10T14:32:45")
    private LocalDateTime timestamp;

    /**
     * Code du statut HTTP renvoyé au client (ex : 400, 404, 500).
     */
    @Schema(description = "Code HTTP renvoyé (ex : 400, 404, 500).",
            example = "400")
    private int status;

    /**
     * Libellé du statut HTTP (ex : "Bad Request", "Not Found").
     */
    @Schema(description = "Libellé du statut HTTP.",
            example = "Bad Request")
    private String error;

    /**
     * Message d’erreur destiné au client. Ne doit jamais contenir
     * d’informations techniques sensibles.
     */
    @Schema(description = "Message d’erreur lisible, sans détails techniques.",
            example = "L’adresse email est déjà utilisée.")
    private String message;

    /**
     * URI de la requête HTTP qui a déclenché l’erreur.
     */
    @Schema(description = "Chemin de la requête ayant déclenché l’erreur.",
            example = "/users")
    private String path;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    /**
     * Constructeur sans argument requis pour la sérialisation JSON.
     */
    public ApiErrorResponse() {
    }

    /**
     * Constructeur principal permettant d’instancier proprement une réponse
     * d’erreur complète.
     *
     * @param timestamp date et heure de l’erreur
     * @param status    code HTTP
     * @param error     libellé du statut HTTP
     * @param message   message d’erreur détaillé
     * @param path      URI de la requête en erreur
     */
    public ApiErrorResponse(LocalDateTime timestamp,
                            int status,
                            String error,
                            String message,
                            String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
