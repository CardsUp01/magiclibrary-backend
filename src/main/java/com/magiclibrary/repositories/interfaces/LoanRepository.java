package com.magiclibrary.repositories.interfaces;

// -----------------------------------------------------------------------------
// IMPORTS SPRING DATA
// -----------------------------------------------------------------------------
// JpaRepository pour opérations CRUD automatiques
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// Entité Loan
import com.magiclibrary.entities.Loan;
// Entité User (pour GET /loans/me)
import com.magiclibrary.entities.User;

import java.util.List;

/**
 * =============================================================================
 * REPOSITORY : LoanRepository
 * =============================================================================
 * Interface de persistance dédiée à l’entité LOAN.
 *
 * Caractéristiques :
 *      - étend JpaRepository<Loan, Integer> pour toutes les opérations CRUD
 *      - sauvegarde, consultation par identifiant, récupération liste
 *      - mise à jour via save()
 *      - suppression physique non implémentée (soft delete géré au service)
 *
 * Règles :
 *      - aucune logique métier ici
 *      - aucune requête personnalisée dans le MVP
 *      - cohérence avec dictionnaire LOAN et MCD/MLD
 *
 * Identifiant technique :
 *      - id_loan (Integer)
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Integer> {

    /**
     * Récupère la liste des emprunts associés à un utilisateur donné.
     * Utilisé par l’endpoint GET /loans/me.
     *
     * @param user utilisateur propriétaire des emprunts
     * @return liste des emprunts du membre
     */
    List<Loan> findByUser(User user);
}