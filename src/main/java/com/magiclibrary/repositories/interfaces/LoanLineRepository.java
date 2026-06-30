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
 *      - aucune logique métier dans ce repository ;
 *      - LoanLine ne possède volontairement aucun champ demoScenarioCode ;
 *      - les opérations de reconstruction des données de démonstration
 *        s'appuient exclusivement sur le marqueur porté par l'entité Loan.
 */
public interface LoanLineRepository extends JpaRepository<LoanLine, Integer> {

    // -------------------------------------------------------------------------
    // LECTURE DES LIGNES D'UN EMPRUNT
    // -------------------------------------------------------------------------
    /**
     * Retourne toutes les lignes appartenant à un emprunt.
     *
     * Utilisé notamment pour l'affichage SSR du détail d'un emprunt.
     *
     * @param idLoan identifiant technique de l'emprunt
     * @return liste des lignes associées
     */
    List<LoanLine> findByLoan_IdLoan(Integer idLoan);

    // -------------------------------------------------------------------------
    // SCÉNARIOS DE DÉMONSTRATION
    // -------------------------------------------------------------------------
    /**
     * Retourne toutes les lignes d'emprunt rattachées à un emprunt appartenant
     * à un scénario de démonstration.
     *
     * Important :
     *      LoanLine ne possède pas son propre champ demoScenarioCode.
     *      La dérivation Spring Data traverse simplement la relation :
     *
     *          LoanLine -> Loan -> demoScenarioCode
     *
     * @param demoScenarioCode code fonctionnel du scénario de démonstration
     * @return liste des lignes d'emprunt concernées
     */
    List<LoanLine> findByLoan_DemoScenarioCode(String demoScenarioCode);

    /**
     * Supprime les lignes d'emprunt rattachées à un emprunt appartenant à un
     * scénario de démonstration.
     *
     * Cette méthode est utilisée uniquement lors de la reconstruction contrôlée
     * des données de démonstration.
     *
     * @param demoScenarioCode code fonctionnel du scénario de démonstration
     */
    void deleteByLoan_DemoScenarioCode(String demoScenarioCode);
}