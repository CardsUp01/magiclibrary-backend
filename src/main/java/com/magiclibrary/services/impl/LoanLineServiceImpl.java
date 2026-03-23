package com.magiclibrary.services.impl;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
// Gestion des dates et heures pour les lignes d’emprunt
import java.time.LocalDateTime;

// Collections Java (retour de listes)
import java.util.List;
import java.util.stream.Collectors;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
// Déclaration du service Spring et gestion transactionnelle
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// DTO pour requêtes et réponses des lignes d’emprunt
import com.magiclibrary.dto.loanline.LoanLineRequestDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
// Entités métier
import com.magiclibrary.entities.Item;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;
// Enumérations métier
import com.magiclibrary.enums.LoanLineStatus;
// Exceptions personnalisées pour la logique métier
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.exceptions.custom.ItemUnavailableException;
import com.magiclibrary.exceptions.custom.LoanAlreadyReturnedException;
import com.magiclibrary.exceptions.custom.LoanLineValidationException;
import com.magiclibrary.exceptions.custom.LoanNotFoundException;
// Mapper pour conversion entité <-> DTO
import com.magiclibrary.mappers.LoanLineMapper;
// Repositories JPA
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.repositories.interfaces.LoanLineRepository;
import com.magiclibrary.repositories.interfaces.LoanRepository;
// Interface service
import com.magiclibrary.services.LoanLineService;

/**
 * =============================================================================
 * SERVICE IMPLEMENTATION : LOAN_LINE
 * =============================================================================
 * Implémentation concrète du service métier de gestion des lignes d’emprunt
 * dans l’application MagicLibrary.
 *
 * Rôle principal (US-05) :
 *      - ajouter un objet à un emprunt existant
 *      - garantir l’intégrité fonctionnelle
 *      - contrôler la cohérence LOAN / ITEM
 *      - appliquer toutes les règles métier du MVP
 *
 * Règles métier appliquées :
 *      - LOAN doit exister et ne pas être restitué
 *      - ITEM doit exister et être disponible
 *      - quantité >= 1
 *      - statut initial = ACTIVE
 *      - dates créées par le service
 *
 * Règle API :
 *      - camelCase strict côté JSON pour cohérence des DTO et erreurs
 */
@Service
@Transactional
public class LoanLineServiceImpl implements LoanLineService {

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    private final LoanRepository loanRepository;
    private final ItemRepository itemRepository;
    private final LoanLineRepository loanLineRepository;
    private final LoanLineMapper loanLineMapper;

    /**
     * Constructeur avec injection des dépendances nécessaires pour gérer les
     * lignes d’emprunt.
     *
     * @param loanRepository accès aux emprunts
     * @param itemRepository accès aux objets du catalogue numérique
     * @param loanLineRepository accès aux lignes d’emprunt
     * @param loanLineMapper conversion DTO <-> Entity
     */
    public LoanLineServiceImpl(
            LoanRepository loanRepository,
            ItemRepository itemRepository,
            LoanLineRepository loanLineRepository,
            LoanLineMapper loanLineMapper
    ) {
        this.loanRepository = loanRepository;
        this.itemRepository = itemRepository;
        this.loanLineRepository = loanLineRepository;
        this.loanLineMapper = loanLineMapper;
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRINCIPALE : création d’une ligne d’emprunt (US-05)
    // -------------------------------------------------------------------------

    /**
     * Crée une ligne d’emprunt associée à un emprunt et un item existants.
     *
     * Règles métier :
     *      - validation de la requête et des identifiants (loan, item)
     *      - vérification que l’emprunt n’est pas déjà restitué
     *      - vérification que l’item est disponible
     *      - vérification que la quantité est >= 1
     *      - conversion DTO → entity, initialisation du statut et des dates
     *      - sauvegarde en base et retour DTO
     *
     * @param request DTO de création contenant idLoan, idItem et quantité
     * @return LoanLineResponseDTO représentant la ligne d’emprunt créée
     * @throws LoanLineValidationException si la requête ou la quantité est invalide
     * @throws LoanNotFoundException si l’emprunt n’existe pas
     * @throws LoanAlreadyReturnedException si l’emprunt a déjà été restitué
     * @throws ItemNotFoundException si l’item n’existe pas
     * @throws ItemUnavailableException si l’item n’est pas disponible
     */
    @Override
    public LoanLineResponseDTO createLoanLine(LoanLineRequestDTO request) {

        // 0) Validation minimale de la requête
        if (request == null) {
            throw LoanLineValidationException.forField(
                    "body",
                    "Le corps de la requête est obligatoire."
            );
        }

        if (request.getIdLoan() == null) {
            throw LoanLineValidationException.forField(
                    "idLoan",
                    "L'identifiant de l'emprunt est obligatoire."
            );
        }

        if (request.getIdItem() == null) {
            throw LoanLineValidationException.forField(
                    "idItem",
                    "L'identifiant de l'objet est obligatoire."
            );
        }

        // 1) Vérification LOAN
        Loan loan = loanRepository.findById(request.getIdLoan())
                .orElseThrow(() -> LoanNotFoundException.forId(request.getIdLoan()));

        if (Boolean.TRUE.equals(loan.getReturnedLoan())) {
            throw new LoanAlreadyReturnedException(
                    "Impossible d’ajouter un objet : l’emprunt "
                            + loan.getIdLoan()
                            + " est déjà restitué."
            );
        }

        // 2) Vérification ITEM
        Item item = itemRepository.findById(request.getIdItem())
                .orElseThrow(() -> ItemNotFoundException.forId(request.getIdItem()));

        if (Boolean.FALSE.equals(item.getAvailableItem())) {
            throw ItemUnavailableException.forItem(item.getIdItem());
        }

        // 3) Vérification de la quantité
        if (request.getQuantityLoanLine() == null || request.getQuantityLoanLine() < 1) {
            throw LoanLineValidationException.forField(
                    "quantityLoanLine",
                    "La quantité doit être supérieure ou égale à 1."
            );
        }

        // 4) Conversion DTO → Entity (structure)
        LoanLine loanLine = loanLineMapper.toEntity(request, loan, item);

        // 5) Règles métier : statut initial
        loanLine.setStatusLoanLine(LoanLineStatus.ACTIVE);

        // 6) Dates système
        loanLine.setCreatedAtLoanLine(LocalDateTime.now());
        loanLine.setUpdatedAtLoanLine(null);

        // 7) Sauvegarde
        LoanLine saved = loanLineRepository.save(loanLine);

        // 8) Conversion Entity → ResponseDTO
        return loanLineMapper.toResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // NOUVEAU (SSR / DÉTAIL EMPRUNT) : lecture des lignes d’emprunt par LOAN
    // -------------------------------------------------------------------------

    /**
     * Récupère toutes les lignes associées à un emprunt.
     *
     * Objectif :
     *      - alimenter l’écran SSR loan-detail
     *      - afficher au minimum les informations disponibles dans LoanLineResponseDTO
     *
     * @param idLoan identifiant du LOAN
     * @return liste des lignes d’emprunt du LOAN
     */
    @Override
    public List<LoanLineResponseDTO> getLoanLinesByLoanId(Integer idLoan) {

        if (idLoan == null) {
            throw new IllegalArgumentException("L'identifiant de l'emprunt est obligatoire.");
        }

        // Sécurisation SSR :
        // - si le LOAN n’existe pas, on ne renvoie pas "vide" silencieusement
        // - on lève une exception explicite pour diagnostiquer un idLoan invalide
        loanRepository.findById(idLoan)
                .orElseThrow(() -> LoanNotFoundException.forId(idLoan));

        List<LoanLine> lines = loanLineRepository.findByLoan_IdLoan(idLoan);

        return lines.stream()
                .map(loanLineMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}