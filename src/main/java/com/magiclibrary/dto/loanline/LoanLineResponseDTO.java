package com.magiclibrary.dto.loanline;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.magiclibrary.enums.LoanLineStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de réponse représentant une ligne d'emprunt.
 *
 * Cette classe expose les informations d'un objet associé à un emprunt,
 * ainsi que les données utiles à son affichage dans les interfaces
 * d'administration et de consultation.
 */
@Schema(description = "Données renvoyées après l’ajout ou la consultation d’une ligne d’emprunt (US-05).")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanLineResponseDTO {

    @Schema(description = "Identifiant unique de la ligne d’emprunt.", example = "101")
    private Integer idLoanLine;

    @Schema(description = "Identifiant de l’emprunt parent.", example = "12")
    private Integer idLoan;

    @Schema(description = "Identifiant de l’objet emprunté.", example = "45")
    private Integer idItem;

    @Schema(description = "Titre de l’objet emprunté.", example = "Close-up professionnel : impact et timing")
    private String titleItem;

    @Schema(description = "Catégorie de l’objet emprunté.", example = "Livre")
    private String categoryItem;

    @Schema(description = "Indique si l’objet est actuellement disponible.", example = "true")
    private Boolean availableItem;

    @Schema(description = "URL de couverture de l’objet emprunté.", example = "https://example.com/covers/item-45.jpg")
    private String coverUrlItem;

    @Schema(description = "Quantité empruntée pour cet objet.", example = "1")
    private Integer quantityLoanLine;

    @Schema(description = "Statut métier de la ligne d’emprunt.", example = "ACTIVE")
    private LoanLineStatus statusLoanLine;

    @Schema(description = "Date et heure de création de la ligne.", example = "2025-01-12T14:22:31")
    private LocalDateTime createdAtLoanLine;

    @Schema(description = "Date et heure de mise à jour de la ligne.", example = "2025-01-12T16:05:47")
    private LocalDateTime updatedAtLoanLine;

    @Schema(description = "Notes internes optionnelles.", example = "Ajout exceptionnel validé par l’administrateur.")
    private String notesLoanLine;

    public LoanLineResponseDTO() {
    }

    public LoanLineResponseDTO(
            Integer idLoanLine,
            Integer idLoan,
            Integer idItem,
            String titleItem,
            String categoryItem,
            Boolean availableItem,
            String coverUrlItem,
            Integer quantityLoanLine,
            LoanLineStatus statusLoanLine,
            LocalDateTime createdAtLoanLine,
            LocalDateTime updatedAtLoanLine,
            String notesLoanLine
    ) {
        this.idLoanLine = idLoanLine;
        this.idLoan = idLoan;
        this.idItem = idItem;
        this.titleItem = titleItem;
        this.categoryItem = categoryItem;
        this.availableItem = availableItem;
        this.coverUrlItem = coverUrlItem;
        this.quantityLoanLine = quantityLoanLine;
        this.statusLoanLine = statusLoanLine;
        this.createdAtLoanLine = createdAtLoanLine;
        this.updatedAtLoanLine = updatedAtLoanLine;
        this.notesLoanLine = notesLoanLine;
    }

    public Integer getIdLoanLine() {
        return idLoanLine;
    }

    public void setIdLoanLine(Integer idLoanLine) {
        this.idLoanLine = idLoanLine;
    }

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

    public String getTitleItem() {
        return titleItem;
    }

    public void setTitleItem(String titleItem) {
        this.titleItem = titleItem;
    }

    public String getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(String categoryItem) {
        this.categoryItem = categoryItem;
    }

    public Boolean getAvailableItem() {
        return availableItem;
    }

    public void setAvailableItem(Boolean availableItem) {
        this.availableItem = availableItem;
    }

    public String getCoverUrlItem() {
        return coverUrlItem;
    }

    public void setCoverUrlItem(String coverUrlItem) {
        this.coverUrlItem = coverUrlItem;
    }

    public Integer getQuantityLoanLine() {
        return quantityLoanLine;
    }

    public void setQuantityLoanLine(Integer quantityLoanLine) {
        this.quantityLoanLine = quantityLoanLine;
    }

    public LoanLineStatus getStatusLoanLine() {
        return statusLoanLine;
    }

    public void setStatusLoanLine(LoanLineStatus statusLoanLine) {
        this.statusLoanLine = statusLoanLine;
    }

    public LocalDateTime getCreatedAtLoanLine() {
        return createdAtLoanLine;
    }

    public void setCreatedAtLoanLine(LocalDateTime createdAtLoanLine) {
        this.createdAtLoanLine = createdAtLoanLine;
    }

    public LocalDateTime getUpdatedAtLoanLine() {
        return updatedAtLoanLine;
    }

    public void setUpdatedAtLoanLine(LocalDateTime updatedAtLoanLine) {
        this.updatedAtLoanLine = updatedAtLoanLine;
    }

    public String getNotesLoanLine() {
        return notesLoanLine;
    }

    public void setNotesLoanLine(String notesLoanLine) {
        this.notesLoanLine = notesLoanLine;
    }
}