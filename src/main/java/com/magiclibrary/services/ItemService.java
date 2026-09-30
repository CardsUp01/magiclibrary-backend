package com.magiclibrary.services;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.data.domain.Page;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.item.ItemRequestDTO;
import com.magiclibrary.dto.item.ItemResponseDTO;

/**
 * =============================================================================
 * SERVICE : ITEM
 * =============================================================================
 * Contrat métier dédié à la gestion des objets du catalogue numérique
 * MagicLibrary.
 *
 * Ce service centralise :
 * - la consultation d'un objet actif ;
 * - la consultation complète du catalogue actif ;
 * - la recherche, le tri et la pagination des objets ;
 * - la création administrative d'un nouvel objet ;
 * - la modification administrative d'un objet existant ;
 * - l'archivage logique d'un objet ;
 * - la consultation administrative des objets archivés ;
 * - la restauration d'un objet archivé.
 *
 * IMPORTANT :
 * MagicLibrary ne supprime pas physiquement les objets du catalogue dans le
 * cadre de la gestion fonctionnelle courante.
 *
 * L'archivage repose sur le champ deletedDateItem de l'entité Item afin de :
 * - préserver l'historique des emprunts ;
 * - conserver les relations LoanLine -> Item ;
 * - retirer l'objet des recherches et consultations du catalogue actif ;
 * - garantir la traçabilité minimale du cycle de vie de l'objet ;
 * - permettre une restauration ultérieure sans recréer l'objet.
 *
 * La restauration consiste à rendre à nouveau actif un objet précédemment
 * archivé. Elle ne recrée pas l'objet et ne modifie pas ses informations
 * fonctionnelles : identifiant, statut, disponibilité, état matériel et
 * historique restent conservés.
 *
 * Les opérations de création, modification, archivage et restauration sont
 * destinées à l'administration du catalogue. Le contrôle d'accès ADMIN est
 * assuré au niveau de la couche Web / sécurité.
 * =============================================================================
 */
public interface ItemService {

    // -------------------------------------------------------------------------
    // CONSULTATION D'UN OBJET
    // -------------------------------------------------------------------------

    /**
     * Recherche un objet actif à partir de son identifiant technique.
     *
     * Un objet archivé n'est pas considéré comme un objet actif et ne doit donc
     * pas être retourné par cette méthode.
     *
     * @param id identifiant technique de l'objet
     * @return représentation complète de l'objet
     * @throws com.magiclibrary.exceptions.custom.ItemNotFoundException
     *         si l'objet n'existe pas ou s'il est archivé
     */
    ItemResponseDTO getItemById(Integer id);

    // -------------------------------------------------------------------------
    // CONSULTATION DU CATALOGUE COMPLET
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les objets actifs du catalogue selon le tri métier par
     * défaut.
     *
     * Le tri par défaut privilégie actuellement la disponibilité, puis le titre.
     *
     * @return liste des objets actifs du catalogue
     */
    List<ItemResponseDTO> getAllItems();

    /**
     * Retourne tous les objets actifs du catalogue selon le tri demandé.
     *
     * L'implémentation peut surcharger cette méthode afin de prendre en charge
     * plusieurs stratégies de tri.
     *
     * Par défaut, un service ne proposant pas de tri spécifique conserve le
     * comportement de getAllItems().
     *
     * @param sort clé fonctionnelle représentant le tri demandé
     * @return liste des objets actifs triés
     */
    default List<ItemResponseDTO> getAllItems(String sort) {
        return getAllItems();
    }

    // -------------------------------------------------------------------------
    // CONSULTATION DES OBJETS ARCHIVÉS
    // -------------------------------------------------------------------------

    /**
     * Retourne l'ensemble des objets archivés du catalogue.
     *
     * Un objet est considéré comme archivé lorsque son champ deletedDateItem
     * est renseigné.
     *
     * Ces objets sont exclus du catalogue actif mais restent physiquement
     * présents en base afin de préserver leur historique et leurs éventuelles
     * relations avec les emprunts passés.
     *
     * L'ordre d'affichage attendu est :
     * - date d'archivage la plus récente en premier ;
     * - titre ;
     * - identifiant technique.
     *
     * Cette méthode est destinée à l'espace d'administration et ne modifie
     * aucune donnée.
     *
     * @return liste des objets archivés
     */
    List<ItemResponseDTO> getArchivedItems();

    // -------------------------------------------------------------------------
    // RECHERCHE, TRI ET PAGINATION
    // -------------------------------------------------------------------------

    /**
     * Retourne une page d'objets actifs du catalogue.
     *
     * La méthode prend en charge :
     * - une recherche textuelle facultative ;
     * - un tri fonctionnel ;
     * - la pagination ;
     * - l'exclusion automatique des objets archivés.
     *
     * @param q terme de recherche, éventuellement vide ou null
     * @param sort clé du tri demandé
     * @param page numéro de page, indexé à partir de zéro
     * @param size nombre maximal d'objets par page
     * @return page de DTO représentant les objets correspondants
     */
    Page<ItemResponseDTO> getItemsPage(
            String q,
            String sort,
            int page,
            int size
    );

    // -------------------------------------------------------------------------
    // CRÉATION D'UN OBJET
    // -------------------------------------------------------------------------

    /**
     * Crée un nouvel objet dans le catalogue.
     *
     * Les données fonctionnelles proviennent du ItemRequestDTO.
     *
     * Les informations techniques sont déterminées par l'application :
     * - génération de l'identifiant par la base ;
     * - date d'ajout définie automatiquement ;
     * - date de mise à jour initialement absente ;
     * - date d'archivage initialement absente ;
     * - disponibilité calculée automatiquement à partir du statut.
     *
     * @param request données saisies pour le nouvel objet
     * @return objet nouvellement créé
     */
    ItemResponseDTO createItem(ItemRequestDTO request);

    // -------------------------------------------------------------------------
    // MODIFICATION D'UN OBJET
    // -------------------------------------------------------------------------

    /**
     * Modifie les informations fonctionnelles d'un objet actif existant.
     *
     * L'identifiant technique, la date d'ajout et la date d'archivage ne sont
     * jamais modifiés à partir du formulaire.
     *
     * La date de dernière modification est actualisée automatiquement et la
     * disponibilité est recalculée à partir du statut métier.
     *
     * @param id identifiant technique de l'objet à modifier
     * @param request nouvelles données fonctionnelles
     * @return objet après modification
     * @throws com.magiclibrary.exceptions.custom.ItemNotFoundException
     *         si l'objet n'existe pas ou s'il est déjà archivé
     */
    ItemResponseDTO updateItem(Integer id, ItemRequestDTO request);

    // -------------------------------------------------------------------------
    // ARCHIVAGE LOGIQUE
    // -------------------------------------------------------------------------

    /**
     * Archive un objet actif du catalogue.
     *
     * Cette opération ne réalise aucune suppression physique en base.
     * Elle renseigne la date d'archivage de l'objet afin que celui-ci soit
     * automatiquement exclu du catalogue actif tout en restant référencé par
     * les éventuelles lignes d'emprunt historiques.
     *
     * L'archivage doit être refusé lorsqu'une ligne d'emprunt ACTIVE référence
     * encore l'objet.
     *
     * @param id identifiant technique de l'objet à archiver
     * @throws com.magiclibrary.exceptions.custom.ItemNotFoundException
     *         si l'objet n'existe pas ou s'il est déjà archivé
     */
    void archiveItem(Integer id);

    // -------------------------------------------------------------------------
    // RESTAURATION D'UN OBJET ARCHIVÉ
    // -------------------------------------------------------------------------

    /**
     * Restaure un objet précédemment archivé.
     *
     * La restauration ne recrée pas une nouvelle ligne en base et ne modifie
     * pas l'identifiant de l'objet.
     *
     * Elle consiste à supprimer la date d'archivage afin que l'objet soit de
     * nouveau considéré comme actif par les requêtes du catalogue.
     *
     * Le statut métier, la disponibilité, l'état matériel et les autres
     * informations fonctionnelles de l'objet sont conservés tels qu'ils étaient
     * au moment de l'archivage.
     *
     * La date de dernière modification est actualisée afin de matérialiser
     * l'opération de restauration.
     *
     * @param id identifiant technique de l'objet archivé à restaurer
     * @throws com.magiclibrary.exceptions.custom.ItemNotFoundException
     *         si l'objet n'existe pas ou s'il n'est pas archivé
     */
    void restoreItem(Integer id);
}