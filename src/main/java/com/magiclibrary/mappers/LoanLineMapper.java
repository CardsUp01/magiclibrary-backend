package com.magiclibrary.mappers;

import org.springframework.stereotype.Component;

import com.magiclibrary.dto.loanline.LoanLineRequestDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;

@Component
public class LoanLineMapper {

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