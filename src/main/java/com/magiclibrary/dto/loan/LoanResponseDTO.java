package com.magiclibrary.dto.loan;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.magiclibrary.enums.LoanStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation complète d’un emprunt renvoyé par l’API.")
public class LoanResponseDTO {

    @Schema(description = "Identifiant unique de l’emprunt.", example = "42")
    private Integer idLoan;

    @Schema(description = "Identifiant du membre ayant réalisé l’emprunt.", example = "7")
    private Integer idUser;

    @Schema(description = "Prénom du membre lié à l’emprunt.", example = "Hugo")
    private String firstNameUser;

    @Schema(description = "Nom du membre lié à l’emprunt.", example = "Dupont")
    private String lastNameUser;

    @Schema(description = "Date et heure de début de l’emprunt.", example = "2025-01-18T10:32:00")
    private LocalDateTime startDateLoan;

    @Schema(description = "Date limite à laquelle l’objet doit être restitué.", example = "2025-02-18")
    private LocalDate dueDateLoan;

    @Schema(description = "Indique si l’emprunt a été restitué.", example = "false")
    private Boolean returnedLoan;

    @Schema(description = "Date effective de restitution (si applicable).", example = "2025-01-28T14:20:00")
    private LocalDateTime returnDateLoan;

    @Schema(description = "Indique si l’emprunt est en retard.", example = "true")
    private Boolean overdueLoan;

    @Schema(description = "Indique si l’emprunt a été prolongé.", example = "false")
    private Boolean extendedLoan;

    @Schema(description = "Nombre total de prolongations appliquées.", example = "0")
    private Integer extensionCountLoan;

    @Schema(description = "Statut métier de l’emprunt.", example = "ONGOING")
    private LoanStatus statusLoan;

    @Schema(description = "Label utilisateur pour le statut.", example = "En cours")
    private String statusLoanLabel;

    @Schema(description = "Origine de l’emprunt (admin / système / futur frontend).", example = "ADMIN")
    private String originLoan;

    @Schema(description = "Label utilisateur pour l’origine.", example = "Administrateur")
    private String originLoanLabel;

    @Schema(description = "Date de suppression logique de l’emprunt, si applicable.", example = "2025-03-01")
    private LocalDate deletedDateLoan;

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

    public String getFirstNameUser() {
        return firstNameUser;
    }

    public void setFirstNameUser(String firstNameUser) {
        this.firstNameUser = firstNameUser;
    }

    public String getLastNameUser() {
        return lastNameUser;
    }

    public void setLastNameUser(String lastNameUser) {
        this.lastNameUser = lastNameUser;
    }

    public LocalDateTime getStartDateLoan() {
        return startDateLoan;
    }

    public void setStartDateLoan(LocalDateTime startDateLoan) {
        this.startDateLoan = startDateLoan;
    }

    public LocalDate getDueDateLoan() {
        return dueDateLoan;
    }

    public void setDueDateLoan(LocalDate dueDateLoan) {
        this.dueDateLoan = dueDateLoan;
    }

    public Boolean getReturnedLoan() {
        return returnedLoan;
    }

    public void setReturnedLoan(Boolean returnedLoan) {
        this.returnedLoan = returnedLoan;
    }

    public LocalDateTime getReturnDateLoan() {
        return returnDateLoan;
    }

    public void setReturnDateLoan(LocalDateTime returnDateLoan) {
        this.returnDateLoan = returnDateLoan;
    }

    public Boolean getOverdueLoan() {
        return overdueLoan;
    }

    public void setOverdueLoan(Boolean overdueLoan) {
        this.overdueLoan = overdueLoan;
    }

    public Boolean getExtendedLoan() {
        return extendedLoan;
    }

    public void setExtendedLoan(Boolean extendedLoan) {
        this.extendedLoan = extendedLoan;
    }

    public Integer getExtensionCountLoan() {
        return extensionCountLoan;
    }

    public void setExtensionCountLoan(Integer extensionCountLoan) {
        this.extensionCountLoan = extensionCountLoan;
    }

    public LoanStatus getStatusLoan() {
        return statusLoan;
    }

    public void setStatusLoan(LoanStatus statusLoan) {
        this.statusLoan = statusLoan;
    }

    public String getStatusLoanLabel() {
        return statusLoanLabel;
    }

    public void setStatusLoanLabel(String statusLoanLabel) {
        this.statusLoanLabel = statusLoanLabel;
    }

    public String getOriginLoan() {
        return originLoan;
    }

    public void setOriginLoan(String originLoan) {
        this.originLoan = originLoan;
    }

    public String getOriginLoanLabel() {
        return originLoanLabel;
    }

    public void setOriginLoanLabel(String originLoanLabel) {
        this.originLoanLabel = originLoanLabel;
    }

    public LocalDate getDeletedDateLoan() {
        return deletedDateLoan;
    }

    public void setDeletedDateLoan(LocalDate deletedDateLoan) {
        this.deletedDateLoan = deletedDateLoan;
    }
}