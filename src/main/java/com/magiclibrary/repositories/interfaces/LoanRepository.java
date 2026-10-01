package com.magiclibrary.repositories.interfaces;

// -----------------------------------------------------------------------------
// IMPORTS SPRING DATA
// -----------------------------------------------------------------------------

// JpaRepository pour opérations CRUD automatiques
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
 *
 * Interface de persistance dédiée à l’entité LOAN.
 *
 * Caractéristiques :
 *      - étend JpaRepository<Loan, Integer> pour toutes les opérations CRUD ;
 *      - sauvegarde, consultation par identifiant, récupération liste ;
 *      - mise à jour via save() ;
 *      - suppression physique non implémentée pour le fonctionnement métier
 *        courant (soft delete géré au service).
 *
 * Règles :
 *      - aucune logique métier ici ;
 *      - les méthodes de démonstration servent uniquement à reconstruire les
 *        scénarios recruteurs ;
 *      - les requêtes d'agrégation exposent uniquement les données nécessaires
 *        aux services, qui restent responsables de leur interprétation métier ;
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
    // CONTRÔLE DES EMPRUNTS ACTIFS
    // -------------------------------------------------------------------------

    /**
     * Vérifie si un utilisateur possède encore au moins un emprunt actif.
     *
     * Un emprunt est considéré comme actif pour ce contrôle lorsque :
     *      - il n'a pas encore été restitué ;
     *      - il n'a pas été supprimé logiquement.
     *
     * Cette méthode est notamment utilisée comme garde-fou avant l'archivage
     * logique d'un utilisateur. Un membre possédant encore un emprunt en cours
     * ou en retard ne doit pas pouvoir être archivé tant que cet emprunt n'a
     * pas été restitué.
     *
     * Le contrôle repose volontairement sur returnedLoan plutôt que seulement
     * sur statusLoan afin de s'appuyer sur l'état réel de restitution.
     *
     * @param user utilisateur dont les emprunts doivent être contrôlés
     * @return true si au moins un emprunt non restitué et non supprimé existe
     */
    boolean existsByUserAndReturnedLoanFalseAndDeletedDateLoanIsNull(User user);

    // -------------------------------------------------------------------------
    // COMPTAGE AGRÉGÉ DES EMPRUNTS OUVERTS PAR UTILISATEUR
    // -------------------------------------------------------------------------

    /**
     * Compte en une seule requête les emprunts actuellement ouverts pour les
     * utilisateurs dont les identifiants sont fournis.
     *
     * Un emprunt est comptabilisé lorsque :
     *      - returnedLoan = false ;
     *      - deletedDateLoan IS NULL.
     *
     * Cette définition est volontairement identique à celle utilisée par le
     * garde-fou d'archivage.
     *
     * Elle couvre donc notamment :
     *      - les emprunts EN COURS ;
     *      - les emprunts EN RETARD.
     *
     * Les emprunts restitués et les emprunts supprimés logiquement sont exclus.
     *
     * Cette requête agrégée permet d'éviter un problème N+1 lors de l'affichage
     * de la page d'administration des utilisateurs : tous les compteurs des
     * utilisateurs affichés sont récupérés en une seule requête SQL.
     *
     * Chaque ligne retournée contient :
     *      index 0 -> Integer : identifiant utilisateur ;
     *      index 1 -> Long    : nombre d'emprunts ouverts.
     *
     * Un utilisateur n'ayant aucun emprunt ouvert n'apparaît pas dans le
     * résultat. Le service lui attribuera alors explicitement la valeur 0.
     *
     * @param userIds identifiants des utilisateurs à analyser
     * @return couples [id utilisateur, nombre d'emprunts ouverts]
     */
    @Query("""
            SELECT l.user.idUser, COUNT(l)
            FROM Loan l
            WHERE l.user.idUser IN :userIds
              AND l.returnedLoan = false
              AND l.deletedDateLoan IS NULL
            GROUP BY l.user.idUser
            """)
    List<Object[]> countOpenLoansByUserIds(
            @Param("userIds") List<Integer> userIds
    );

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
    List<Loan> findByUserAndDemoScenarioCode(
            User user,
            String demoScenarioCode
    );

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