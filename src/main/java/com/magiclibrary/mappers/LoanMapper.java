package com.magiclibrary.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.LoanOrigin;

public final class LoanMapper {

    private LoanMapper() {
    }

    public static LoanResponseDTO toResponseDTO(Loan loan) {
        if (loan == null) {
            return null;
        }

        LoanResponseDTO dto = new LoanResponseDTO();

        dto.setIdLoan(loan.getIdLoan());

        User user = loan.getUser();
        dto.setIdUser(user != null ? user.getIdUser() : null);
        dto.setFirstNameUser(user != null ? formatFirstName(user.getFirstNameUser()) : null);
        dto.setLastNameUser(user != null ? formatLastName(user.getLastNameUser()) : null);

        dto.setStartDateLoan(loan.getStartDateLoan());
        dto.setDueDateLoan(loan.getDueDateLoan());
        dto.setReturnedLoan(loan.getReturnedLoan());
        dto.setReturnDateLoan(loan.getReturnDateLoan());

        dto.setOverdueLoan(loan.getOverdueLoan());
        dto.setExtendedLoan(loan.getExtendedLoan());
        dto.setExtensionCountLoan(loan.getExtensionCountLoan());

        dto.setStatusLoan(loan.getStatusLoan());
        dto.setStatusLoanLabel(
                loan.getStatusLoan() != null ? loan.getStatusLoan().getLabel() : null
        );

        String originCode = loan.getOriginLoan();
        dto.setOriginLoan(originCode);
        dto.setOriginLoanLabel(LoanOrigin.labelOf(originCode));

        dto.setDeletedDateLoan(loan.getDeletedDateLoan());

        return dto;
    }

    public static List<LoanResponseDTO> toResponseDTOList(List<Loan> loans) {
        List<LoanResponseDTO> list = new ArrayList<>();

        if (loans == null || loans.isEmpty()) {
            return list;
        }

        for (Loan loan : loans) {
            LoanResponseDTO dto = toResponseDTO(loan);
            if (dto != null) {
                list.add(dto);
            }
        }

        return list;
    }

    private static String formatFirstName(String firstName) {
        if (firstName == null) {
            return null;
        }

        String trimmed = firstName.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String formatLastName(String lastName) {
        if (lastName == null) {
            return null;
        }

        String trimmed = lastName.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        return trimmed.toUpperCase(Locale.ROOT);
    }
}