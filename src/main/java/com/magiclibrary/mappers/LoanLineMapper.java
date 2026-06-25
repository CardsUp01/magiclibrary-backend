package com.magiclibrary.mappers;

import org.springframework.stereotype.Component;

import com.magiclibrary.dto.loanline.LoanLineRequestDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;

/**
 * Mapper chargé des conversions entre l'entité LoanLine
 * et les DTO utilisés par les API REST.
 *
 * Cette classe centralise la transformation des lignes
 * d'emprunt lors des opérations de création et de consultation.
 */
@Component
public class LoanLineMapper {

    /*
     * Construit une entité LoanLine à partir du DTO reçu
     * et des entités déjà résolues par la couche service.
     */
    public LoanLine toEntity(LoanLineRequestDTO dto, Loan loan, Item item) {
        if (dto == null) {
            return null;
        }

        LoanLine loanLine = new LoanLine();
        loanLine.setLoan(loan);
        loanLine.setItem(item);
        loanLine.setQuantityLoanLine(dto.getQuantityLoanLine());
        loanLine.setNotesLoanLine(dto.getNotesLoanLine());

        return loanLine;
    }

    /*
     * Transforme une entité LoanLine en DTO de réponse.
     * Les informations utiles de l'objet associé sont exposées
     * directement afin de simplifier l'affichage côté client.
     */
    public LoanLineResponseDTO toResponseDTO(LoanLine loanLine) {
        if (loanLine == null) {
            return null;
        }

        Integer idLoan = null;
        Integer idItem = null;
        String titleItem = null;
        String categoryItem = null;
        Boolean availableItem = null;
        String coverUrlItem = null;

        if (loanLine.getLoan() != null) {
            idLoan = loanLine.getLoan().getIdLoan();
        }

        if (loanLine.getItem() != null) {
            idItem = loanLine.getItem().getIdItem();
            titleItem = loanLine.getItem().getTitleItem();
            categoryItem = loanLine.getItem().getCategoryItem();
            availableItem = loanLine.getItem().getAvailableItem();
            coverUrlItem = loanLine.getItem().getCoverUrlItem();
        }

        return new LoanLineResponseDTO(
                loanLine.getIdLoanLine(),
                idLoan,
                idItem,
                titleItem,
                categoryItem,
                availableItem,
                coverUrlItem,
                loanLine.getQuantityLoanLine(),
                loanLine.getStatusLoanLine(),
                loanLine.getCreatedAtLoanLine(),
                loanLine.getUpdatedAtLoanLine(),
                loanLine.getNotesLoanLine()
        );
    }
}