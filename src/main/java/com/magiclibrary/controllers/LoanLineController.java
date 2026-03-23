package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;

// -----------------------------------------------------------------------------
// IMPORTS SPRING WEB
// -----------------------------------------------------------------------------
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.loanline.LoanLineRequestDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
import com.magiclibrary.services.LoanLineService;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/* =============================================================================
   CONTROLLER : LoanLineController
   -----------------------------------------------------------------------------
   US-05 — Gestion des lignes d’emprunt (LOAN_LINE)
       - Endpoint : POST /loan-lines
       - Rôle requis : ADMIN
       - Toute la logique métier (existence LOAN/ITEM, disponibilité, returned, etc.)
         est déléguée au service LoanLineServiceImpl
   =============================================================================
*/
@RestController
@RequestMapping("/loan-lines")
@Tag(
        name = "Lignes d’emprunt (LOAN_LINE)",
        description = "US-05 : Ajout d’un objet à un emprunt existant (ADMIN uniquement)."
)
@SecurityRequirement(name = "bearerAuth")
public class LoanLineController {

    // -------------------------------------------------------------------------
    // DÉPENDANCE SERVICE
    // -------------------------------------------------------------------------
    private final LoanLineService loanLineService;

    /**
     * Injection du service via le constructeur
     *
     * @param loanLineService service métier pour la gestion des LOAN_LINE
     */
    public LoanLineController(LoanLineService loanLineService) {
        this.loanLineService = loanLineService;
    }

    // =========================================================================
    // POST /loan-lines — Création d’une ligne d’emprunt (US-05 — ADMIN)
    // =========================================================================
    @Operation(
            summary = "Ajouter une ligne d’emprunt (US-05 — ADMIN)",
            description = "Ajoute un item (id_item) à un emprunt existant (id_loan). Endpoint sécurisé : rôle Administrateur requis."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Ligne d’emprunt créée avec succès.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoanLineResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : données manquantes ou non conformes.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle Administrateur requis.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ressource introuvable : emprunt ou item inexistant.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflit : emprunt déjà restitué ou item indisponible.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class)
                    )
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanLineResponseDTO> createLoanLine(
            @Valid @RequestBody LoanLineRequestDTO request
    ) {
        // Délégation au service pour création et gestion de la logique métier
        LoanLineResponseDTO created = loanLineService.createLoanLine(request);

        // Retourne 201 CREATED avec la ligne d’emprunt créée
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}