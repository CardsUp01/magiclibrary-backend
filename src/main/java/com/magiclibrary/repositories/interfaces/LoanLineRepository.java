package com.magiclibrary.repositories.interfaces;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS SPRING DATA JPA
// -----------------------------------------------------------------------------
import org.springframework.data.jpa.repository.JpaRepository;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.entities.LoanLine;
import com.magiclibrary.enums.LoanLineStatus;

/**
 * =============================================================================
 * REPOSITORY : LOAN_LINE
 * =============================================================================
 * Interface d'accès aux données dédiée aux lignes d'emprunt MagicLibrary.
 *
 * Une LoanLine représente le lien entre :
 *
 *     Loan -> LoanLine -> Item
 *
 * Ce repository centralise les opérations nécessaires pour :
 * - consulter les lignes appartenant à un emprunt ;
 * - vérifier l'existence d'une ligne selon un objet et un statut ;
 * - gérer les lignes appartenant aux scénarios de démonstration.
 *
 * Spring Data JPA fournit automatiquement les opérations CRUD standards grâce
 * à l'héritage de JpaRepository<LoanLine, Integer>.
 *
 * IMPORTANT :
 * aucune logique métier n'est implémentée dans ce repository.
 *
 * Les décisions fonctionnelles, telles que l'autorisation ou le refus
 * d'archiver un objet encore emprunté, restent exclusivement gérées par la
 * couche service.
 *
 * LoanLine ne possède volontairement aucun champ demoScenarioCode.
 * Les opérations propres à la DEMO traversent la relation :
 *
 *     LoanLine -> Loan -> demoScenarioCode
 *
 * afin de conserver le marqueur de scénario uniquement sur l'entité Loan.
 * =============================================================================
 */
public interface LoanLineRepository extends JpaRepository<LoanLine, Integer> {

    // -------------------------------------------------------------------------
    // LECTURE DES LIGNES D'UN EMPRUNT
    // -------------------------------------------------------------------------

    /**
     * Retourne toutes les lignes rattachées à un emprunt.
     *
     * Cette méthode est notamment utilisée pour l'affichage SSR du détail d'un
     * emprunt et permet de récupérer les différents objets associés à celui-ci.
     *
     * La dérivation Spring Data traverse la relation :
     *
     *     LoanLine -> Loan -> idLoan
     *
     * @param idLoan identifiant technique de l'emprunt
     * @return liste des lignes associées à l'emprunt
     */
    List<LoanLine> findByLoan_IdLoan(Integer idLoan);

    // -------------------------------------------------------------------------
    // CONTRÔLE D'UTILISATION D'UN OBJET
    // -------------------------------------------------------------------------

    /**
     * Vérifie si au moins une ligne d'emprunt référence un objet donné avec un
     * statut précis.
     *
     * Cette méthode est notamment utilisée avant l'archivage d'un Item.
     *
     * Exemple métier :
     *
     *     existsByItem_IdItemAndStatusLoanLine(idItem, ACTIVE)
     *
     * permet de savoir si l'objet est actuellement associé à une ligne
     * d'emprunt active.
     *
     * L'utilisation d'une requête d'existence évite de charger inutilement
     * toutes les LoanLine correspondantes en mémoire.
     *
     * La dérivation Spring Data traverse :
     *
     *     LoanLine -> Item -> idItem
     *
     * puis applique le filtre sur :
     *
     *     statusLoanLine
     *
     * @param idItem identifiant technique de l'objet
     * @param statusLoanLine statut de ligne recherché
     * @return true si au moins une ligne correspondante existe, sinon false
     */
    boolean existsByItem_IdItemAndStatusLoanLine(
            Integer idItem,
            LoanLineStatus statusLoanLine
    );

    // -------------------------------------------------------------------------
    // SCÉNARIOS DE DÉMONSTRATION
    // -------------------------------------------------------------------------

    /**
     * Retourne toutes les lignes d'emprunt rattachées à un emprunt appartenant
     * à un scénario de démonstration.
     *
     * LoanLine ne possède pas son propre champ demoScenarioCode.
     *
     * Spring Data traverse automatiquement la relation :
     *
     *     LoanLine -> Loan -> demoScenarioCode
     *
     * Cette méthode permet ainsi de retrouver les lignes appartenant à un
     * scénario sans dupliquer le marqueur fonctionnel dans plusieurs entités.
     *
     * @param demoScenarioCode code fonctionnel du scénario de démonstration
     * @return liste des lignes d'emprunt concernées
     */
    List<LoanLine> findByLoan_DemoScenarioCode(String demoScenarioCode);

    /**
     * Supprime les lignes d'emprunt rattachées à un emprunt appartenant à un
     * scénario de démonstration.
     *
     * Cette opération est exclusivement destinée à la reconstruction contrôlée
     * des données de la DEMO.
     *
     * Elle ne doit pas être utilisée dans le fonctionnement métier normal de
     * MagicLibrary ni dans l'environnement CLIENT.
     *
     * La suppression repose sur la relation :
     *
     *     LoanLine -> Loan -> demoScenarioCode
     *
     * @param demoScenarioCode code fonctionnel du scénario de démonstration
     */
    void deleteByLoan_DemoScenarioCode(String demoScenarioCode);
}