package com.magiclibrary.dto.loanline;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS JACKSON
// -----------------------------------------------------------------------------
import com.fasterxml.jackson.annotation.JsonInclude;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * =============================================================================
 *  DTO : LOAN_LINE REQUEST
 * =============================================================================
 *  US-07 — Ajout d’une ligne d’emprunt
 *
 *  RÈGLE API :
 *      - camelCase strict côté JSON
 *      - aucun snake_case exposé
 *      - aucun @JsonProperty
 * =============================================================================
 */
@Schema(description = "Payload utilisé pour ajouter une ligne d’emprunt (US-07).")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanLineRequestDTO {

    @Schema(description = "Identifiant de l’emprunt parent.", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "L'identifiant de l'emprunt est obligatoire.")
    private Integer idLoan;

    @Schema(description = "Identifiant de l’objet emprunté.", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "L'identifiant de l'objet est obligatoire.")
    private Integer idItem;

    @Schema(description = "Quantité empruntée (minimum : 1).", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La quantité est obligatoire.")
    @Min(value = 1, message = "La quantité doit être supérieure ou égale à 1.")
    private Integer quantityLoanLine;

    @Schema(
            description = "Notes internes optionnelles concernant la ligne d’emprunt.",
            example = "Prêt exceptionnel autorisé par l’administrateur.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(min = 2, max = 10_000, message = "Les notes doivent contenir entre 2 et 10 000 caractères.")
    private String notesLoanLine;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    public LoanLineRequestDTO() {}

    public LoanLineRequestDTO(
            Integer idLoan,
            Integer idItem,
            Integer quantityLoanLine,
            String notesLoanLine
    ) {
        this.idLoan = idLoan;
        this.idItem = idItem;
        this.quantityLoanLine = quantityLoanLine;
        this.notesLoanLine = notesLoanLine;
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    public Integer getIdLoan() {
        return idLoan;
    }

    public void setIdLoan(Integer idLoan) {
        this.idLoan = idLoan;
    }

    public Integer getIdItem() {
        return idItem;
    }

    public void setIdItem(Integer idItem) {
        this.idItem = idItem;
    }

    public Integer getQuantityLoanLine() {
        return quantityLoanLine;
    }

    public void setQuantityLoanLine(Integer quantityLoanLine) {
        this.quantityLoanLine = quantityLoanLine;
    }

    public String getNotesLoanLine() {
        return notesLoanLine;
    }

    public void setNotesLoanLine(String notesLoanLine) {
        this.notesLoanLine = notesLoanLine;
    }
}