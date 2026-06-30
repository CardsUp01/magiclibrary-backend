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
 *      - les méthodes de démonstration servent uniquement à reconstruire les
 *        scénarios recruteurs ;
 *      - aucune logique métier ne doit être implémentée dans le repository ;
 *      - cohérence avec le dictionnaire LOAN et le MCD/MLD.
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

    // -------------------------------------------------------------------------
    // SCÉNARIOS DE DÉMONSTRATION
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les emprunts appartenant à un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des emprunts correspondants
     */
    List<Loan> findByDemoScenarioCode(String demoScenarioCode);

    /**
     * Vérifie si un scénario d'emprunt de démonstration existe.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return true si au moins un emprunt existe
     */
    boolean existsByDemoScenarioCode(String demoScenarioCode);

    /**
     * Compte le nombre d'emprunts associés à un scénario de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     * @return nombre d'emprunts
     */
    long countByDemoScenarioCode(String demoScenarioCode);

    /**
     * Retourne les emprunts d'un utilisateur appartenant à un scénario de
     * démonstration donné.
     *
     * @param user utilisateur concerné
     * @param demoScenarioCode code fonctionnel de scénario
     * @return liste des emprunts correspondants
     */
    List<Loan> findByUserAndDemoScenarioCode(User user, String demoScenarioCode);

    /**
     * Supprime les emprunts appartenant à un scénario de démonstration.
     *
     * Cette méthode est destinée exclusivement au mécanisme de reconstruction
     * automatique des données de démonstration.
     *
     * @param demoScenarioCode code fonctionnel de scénario
     */
    void deleteByDemoScenarioCode(String demoScenarioCode);
}