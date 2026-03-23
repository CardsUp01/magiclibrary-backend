package com.magiclibrary.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.magiclibrary.dto.loan.LoanRequestDTO;
import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.exceptions.custom.LoanAlreadyReturnedException;
import com.magiclibrary.exceptions.custom.LoanNotFoundException;

public interface LoanService {

    LoanResponseDTO createLoan(LoanRequestDTO request);

    LoanResponseDTO returnLoan(Integer idLoan)
            throws LoanNotFoundException, LoanAlreadyReturnedException;

    LoanResponseDTO getLoanById(Integer idLoan) throws LoanNotFoundException;

    List<LoanResponseDTO> getAllLoans();

    List<LoanResponseDTO> getAllLoansSorted(String sort);

    Page<LoanResponseDTO> getAllLoansPagedAndSorted(String sort, int page, int size);

    Page<LoanResponseDTO> searchLoansPagedAndSorted(String query, String sort, int page, int size);

    List<LoanResponseDTO> getLoansForUser(String email);

    Page<LoanResponseDTO> getLoansForUserPagedAndSorted(String email, String sort, int page, int size);

    LoanResponseDTO getLoanByIdForUser(Integer idLoan, String email, boolean isAdmin);

    List<LoanResponseDTO> suggestLoans(String query);

    List<LoanResponseDTO> suggestLoansForUser(String email, String query);
}