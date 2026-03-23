package com.magiclibrary.dto.loan;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.NotNull;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

/* =============================================================================
   DTO : LoanRequestDTO
   -----------------------------------------------------------------------------
   Description :
       Représente les données reçues lors de la création d’un nouvel emprunt
       via l’endpoint POST /loans. Aligné strictement avec le MVP MagicLibrary.

   Règles métier MVP :
       - Création d’un emprunt vide uniquement (pas de LOAN_LINE)
       - Association obligatoire à un utilisateur (id_user)
       - Dates, flags, statut et origine générés automatiquement par la couche Service
       - Conformité complète au dictionnaire LOAN et US-04

   Champs exposés :
       - idUser : identifiant du membre pour lequel l’emprunt est créé
   =============================================================================
*/
@Schema(description = "Représente les données nécessaires à la création d’un emprunt (MVP).")
public class LoanRequestDTO {

    // -------------------------------------------------------------------------
    // IDENTIFIANT UTILISATEUR
    // -------------------------------------------------------------------------

    /**
     * Identifiant du membre auquel l’emprunt est associé.
     * Contrainte : obligatoire (NOT NULL)
     * Relation : FK → USER(id_user)
     * Usage : résolu et injecté par le service pour créer l’emprunt.
     */
    @Schema(
            description = "Identifiant du membre pour lequel l’emprunt doit être créé.",
            example = "7",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "L'identifiant utilisateur est obligatoire.")
    private Integer idUser;

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    /**
     * Retourne l’identifiant du membre associé à l’emprunt.
     *
     * @return identifiant utilisateur
     */
    public Integer getIdUser() {
        return idUser;
    }

    /**
     * Définit l’identifiant du membre associé à l’emprunt.
     *
     * @param idUser identifiant utilisateur
     */
    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }
}