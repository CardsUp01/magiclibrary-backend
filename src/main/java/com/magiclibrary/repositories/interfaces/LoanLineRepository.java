package com.magiclibrary.repositories.interfaces;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
// Collections Java (retour de listes)
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS SPRING DATA JPA
// -----------------------------------------------------------------------------
// JpaRepository pour opérations CRUD automatiques et gestion des entités
import org.springframework.data.jpa.repository.JpaRepository;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// Entité LoanLine
import com.magiclibrary.entities.LoanLine;

/**
 * =============================================================================
 * REPOSITORY : LOAN_LINE
 * =============================================================================
 * Interface d’accès aux données pour l’entité LoanLine.
 *
 * Caractéristiques :
 *      - hérite de JpaRepository<LoanLine, Integer>
 *      - fournit automatiquement les opérations CRUD standards
 *      - supporte la gestion des transactions et recherches par identifiant
 *      - possibilité de pagination si nécessaire
 *
 * Remarque :
 *      - aucune méthode personnalisée requise dans le MVP
 *      - aucune logique métier dans ce repository
 */
public interface LoanLineRepository extends JpaRepository<LoanLine, Integer> {

    // -------------------------------------------------------------------------
    // NOUVEAU (SSR / DÉTAIL EMPRUNT) : lecture des lignes par emprunt
    // -------------------------------------------------------------------------
    // Objectif :
    //      - permettre l’affichage SSR du détail d’un emprunt (loan-detail)
    //      - récupérer toutes les lignes associées à un idLoan
    //
    // Note :
    //      - dérivation Spring Data JPA basée sur l’association LoanLine -> Loan
    //      - ne contient aucune logique métier
    List<LoanLine> findByLoan_IdLoan(Integer idLoan);
}