package com.magiclibrary.services;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
// Collections Java (retour de listes)
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.loanline.LoanLineRequestDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;

/**
 * =============================================================================
 *  SERVICE INTERFACE : LOAN_LINE
 * =============================================================================
 *  Déclare les opérations métier liées aux lignes d’emprunt dans
 *  l’application *MagicLibrary*.
 *
 *  Conformité :
 *      - respecte le périmètre du MVP (US-05 uniquement) ;
 *      - aucune suppression ou édition de LOAN_LINE ;
 *      - création strictement contrôlée selon les règles LOAN/ITEM.
 *
 *  Règles métier intégrées :
 *      - un emprunt restitué ne peut pas recevoir de LOAN_LINE ;
 *      - un ITEM indisponible ne peut pas être emprunté ;
 *      - LOAN et ITEM doivent exister ;
 *      - quantité ≥ 1.
 *
 *  Cette interface est implémentée dans LoanLineServiceImpl.
 * =============================================================================
 */
public interface LoanLineService {

    /**
     * Crée une nouvelle ligne d’emprunt pour un emprunt existant (US-05).
     *
     * Étapes réalisées dans l’implémentation :
     *      - vérification de l’existence de l’emprunt ;
     *      - vérification que l’emprunt n’est pas restitué ;
     *      - vérification de l’existence de l’objet ITEM ;
     *      - validation de sa disponibilité ;
     *      - validation de la quantité ;
     *      - création et sauvegarde de la ligne d’emprunt ;
     *      - retour d’un DTO structuré.
     *
     * @param request données de création provenant du client
     * @return DTO complet représentant la ligne créée
     */
    LoanLineResponseDTO createLoanLine(LoanLineRequestDTO request);

    /**
     * Récupère toutes les lignes d’emprunt associées à un emprunt.
     *
     * Objectif :
     *      - alimenter le rendu SSR "loan-detail" (détail emprunt)
     *      - afficher au minimum : idItem + quantité + statut ligne
     *
     * Note :
     *      - aucune logique métier additionnelle ici
     *      - la sécurité d’accès au LOAN (propriétaire / admin) est gérée ailleurs
     *
     * @param idLoan identifiant de l’emprunt parent
     * @return liste des lignes d’emprunt associées à l’emprunt
     */
    List<LoanLineResponseDTO> getLoanLinesByLoanId(Integer idLoan);
}