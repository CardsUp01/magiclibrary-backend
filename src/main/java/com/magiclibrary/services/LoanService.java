package com.magiclibrary.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.magiclibrary.dto.loan.AdminLoanRequestDTO;
import com.magiclibrary.dto.loan.LoanRequestDTO;
import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.exceptions.custom.LoanAlreadyReturnedException;
import com.magiclibrary.exceptions.custom.LoanNotFoundException;

/**
 * =============================================================================
 * SERVICE : LOAN
 * =============================================================================
 *
 * Contrat métier principal dédié à la gestion des emprunts MagicLibrary.
 *
 * Ce service couvre notamment :
 * - la création historique d'un emprunt vide issue du MVP ;
 * - la création administrative complète d'un emprunt avec ses objets ;
 * - la restitution d'un emprunt ;
 * - la consultation des emprunts ;
 * - la recherche, le tri et la pagination ;
 * - les suggestions utilisées par les interfaces SSR.
 *
 * IMPORTANT :
 *
 * La méthode historique createLoan(LoanRequestDTO) est conservée afin de ne pas
 * casser le contrat REST existant du MVP.
 *
 * La méthode createAdminLoan(AdminLoanRequestDTO) correspond au nouveau
 * workflow métier utilisé par l'administration SSR. Elle devra créer, dans une
 * même transaction :
 *
 * 1. l'emprunt ;
 * 2. les lignes d'emprunt associées ;
 * 3. la mise à jour de disponibilité des objets concernés.
 *
 * =============================================================================
 */
public interface LoanService {

    // -------------------------------------------------------------------------
    // CRÉATION HISTORIQUE MVP
    // -------------------------------------------------------------------------

    /**
     * Crée un emprunt vide selon le fonctionnement historique du MVP.
     *
     * Cette méthode est notamment utilisée par l'API REST existante.
     *
     * @param request données minimales de création
     * @return emprunt créé
     */
    LoanResponseDTO createLoan(LoanRequestDTO request);

    // -------------------------------------------------------------------------
    // CRÉATION ADMINISTRATIVE COMPLÈTE
    // -------------------------------------------------------------------------

    /**
     * Crée un emprunt complet depuis l'espace d'administration.
     *
     * Cette opération doit être atomique : aucune donnée ne doit être conservée
     * si la validation du membre, d'un objet ou d'une règle métier échoue.
     *
     * La couche Service est responsable notamment :
     * - de la validation du membre ;
     * - de la validation de tous les objets sélectionnés ;
     * - de la détection des doublons d'objets ;
     * - de la cohérence des dates ;
     * - du calcul du statut ONGOING / LATE ;
     * - de la création du Loan ;
     * - de la création des LoanLine ACTIVE ;
     * - du passage des objets à UNAVAILABLE.
     *
     * @param request données fonctionnelles saisies par l'administrateur
     * @return emprunt complet nouvellement créé
     */
    LoanResponseDTO createAdminLoan(AdminLoanRequestDTO request);

    // -------------------------------------------------------------------------
    // RESTITUTION
    // -------------------------------------------------------------------------

    /**
     * Restitue un emprunt existant.
     *
     * @param idLoan identifiant de l'emprunt
     * @return emprunt après restitution
     * @throws LoanNotFoundException si l'emprunt n'existe pas
     * @throws LoanAlreadyReturnedException si l'emprunt est déjà restitué
     */
    LoanResponseDTO returnLoan(Integer idLoan)
            throws LoanNotFoundException, LoanAlreadyReturnedException;

    // -------------------------------------------------------------------------
    // CONSULTATION
    // -------------------------------------------------------------------------

    /**
     * Retourne un emprunt à partir de son identifiant.
     *
     * @param idLoan identifiant de l'emprunt
     * @return emprunt correspondant
     * @throws LoanNotFoundException si l'emprunt n'existe pas
     */
    LoanResponseDTO getLoanById(Integer idLoan) throws LoanNotFoundException;

    /**
     * Retourne tous les emprunts actifs.
     *
     * @return liste des emprunts
     */
    List<LoanResponseDTO> getAllLoans();

    /**
     * Retourne tous les emprunts actifs selon le tri demandé.
     *
     * @param sort clé de tri
     * @return emprunts triés
     */
    List<LoanResponseDTO> getAllLoansSorted(String sort);

    /**
     * Retourne une page d'emprunts actifs selon le tri demandé.
     *
     * @param sort clé de tri
     * @param page numéro de page
     * @param size taille de page
     * @return page d'emprunts
     */
    Page<LoanResponseDTO> getAllLoansPagedAndSorted(
            String sort,
            int page,
            int size
    );

    /**
     * Recherche les emprunts avec pagination et tri.
     *
     * @param query recherche textuelle
     * @param sort clé de tri
     * @param page numéro de page
     * @param size taille de page
     * @return page d'emprunts correspondants
     */
    Page<LoanResponseDTO> searchLoansPagedAndSorted(
            String query,
            String sort,
            int page,
            int size
    );

    // -------------------------------------------------------------------------
    // CONSULTATION PAR UTILISATEUR
    // -------------------------------------------------------------------------

    /**
     * Retourne les emprunts appartenant à un utilisateur.
     *
     * @param email email de l'utilisateur
     * @return emprunts de l'utilisateur
     */
    List<LoanResponseDTO> getLoansForUser(String email);

    /**
     * Retourne une page d'emprunts appartenant à un utilisateur.
     *
     * @param email email de l'utilisateur
     * @param sort clé de tri
     * @param page numéro de page
     * @param size taille de page
     * @return page d'emprunts de l'utilisateur
     */
    Page<LoanResponseDTO> getLoansForUserPagedAndSorted(
            String email,
            String sort,
            int page,
            int size
    );

    /**
     * Retourne le détail d'un emprunt en appliquant les droits de consultation
     * correspondant à l'utilisateur courant.
     *
     * @param idLoan identifiant de l'emprunt
     * @param email email de l'utilisateur authentifié
     * @param isAdmin indique si l'utilisateur possède le rôle ADMIN
     * @return emprunt autorisé
     */
    LoanResponseDTO getLoanByIdForUser(
            Integer idLoan,
            String email,
            boolean isAdmin
    );

    // -------------------------------------------------------------------------
    // SUGGESTIONS
    // -------------------------------------------------------------------------

    /**
     * Retourne les suggestions d'emprunts utilisées par l'administration.
     *
     * @param query recherche saisie
     * @return suggestions correspondantes
     */
    List<LoanResponseDTO> suggestLoans(String query);

    /**
     * Retourne les suggestions d'emprunts limitées à un utilisateur donné.
     *
     * @param email email de l'utilisateur
     * @param query recherche saisie
     * @return suggestions correspondantes
     */
    List<LoanResponseDTO> suggestLoansForUser(String email, String query);
}