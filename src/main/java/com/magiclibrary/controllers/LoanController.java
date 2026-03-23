package com.magiclibrary.controllers;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.magiclibrary.dto.loan.LoanRequestDTO;
import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.services.LoanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/loans")
@Validated
@Tag(
        name = "Emprunts (LOAN)",
        description = "Endpoints permettant de créer, restituer et consulter les emprunts."
)
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(
            summary = "Consulter mes emprunts",
            description = "Retourne la liste des emprunts de l’utilisateur actuellement authentifié."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des emprunts retournée avec succès.",
                    content = @Content(schema = @Schema(implementation = LoanResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content(schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<?> getMyLoans(Authentication authentication) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                loanService.getLoansForUser(email)
        );
    }

    @Operation(
            summary = "Suggérer des emprunts pour l’administration",
            description = "Retourne une liste légère d’emprunts pour l’autocomplétion de la page d’administration des emprunts. Accès réservé à l’administrateur."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Suggestions retournées avec succès.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content(schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : rôle administrateur requis.",
                    content = @Content(schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class))
            )
    })
    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanSuggestResponse>> suggestLoans(
            @RequestParam(name = "q", required = false) String q
    ) {
        List<LoanSuggestResponse> suggestions = loanService.suggestLoans(q).stream()
                .map(this::toSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    @Operation(
            summary = "Consulter le détail d’un emprunt",
            description = "Retourne le détail d’un emprunt. ADMIN : accès libre. MEMBRE : accès uniquement si l’emprunt lui appartient."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Emprunt retourné avec succès.",
                    content = @Content(schema = @Schema(implementation = LoanResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié : JWT absent ou invalide.",
                    content = @Content(schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit : l’emprunt ne correspond pas au membre connecté.",
                    content = @Content(schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Emprunt introuvable.",
                    content = @Content(schema = @Schema(implementation = com.magiclibrary.exceptions.model.ApiErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> getLoanById(
            @Parameter(description = "Identifiant de l’emprunt", required = true)
            @PathVariable("id") Integer idLoan,
            Authentication authentication
    ) {
        String email = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(Objects::nonNull)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));

        LoanResponseDTO response = loanService.getLoanByIdForUser(idLoan, email, isAdmin);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<LoanResponseDTO> createLoan(@Valid @RequestBody LoanRequestDTO request) {
        LoanResponseDTO response = loanService.createLoan(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<LoanResponseDTO> returnLoan(
            @PathVariable("id") Integer idLoan
    ) {
        LoanResponseDTO response = loanService.returnLoan(idLoan);

        return ResponseEntity.ok(response);
    }

    private LoanSuggestResponse toSuggestResponse(LoanResponseDTO loan) {
        return new LoanSuggestResponse(
                loan.getIdLoan(),
                loan.getIdUser(),
                loan.getFirstNameUser(),
                loan.getLastNameUser(),
                loan.getStatusLoanLabel(),
                loan.getOriginLoanLabel()
        );
    }

    public static final class LoanSuggestResponse {

        private Integer idLoan;
        private Integer idUser;
        private String firstName;
        private String lastName;
        private String status;
        private String origin;

        public LoanSuggestResponse() {
        }

        public LoanSuggestResponse(
                Integer idLoan,
                Integer idUser,
                String firstName,
                String lastName,
                String status,
                String origin
        ) {
            this.idLoan = idLoan;
            this.idUser = idUser;
            this.firstName = firstName;
            this.lastName = lastName;
            this.status = status;
            this.origin = origin;
        }

        public Integer getIdLoan() {
            return idLoan;
        }

        public void setIdLoan(Integer idLoan) {
            this.idLoan = idLoan;
        }

        public Integer getIdUser() {
            return idUser;
        }

        public void setIdUser(Integer idUser) {
            this.idUser = idUser;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }
    }
}