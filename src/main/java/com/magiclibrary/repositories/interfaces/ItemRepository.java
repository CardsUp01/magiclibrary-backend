package com.magiclibrary.repositories.interfaces;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.magiclibrary.entities.Item;

/**
 * Repository JPA dédié à la gestion des objets du catalogue numérique.
 *
 * Cette interface centralise les opérations de recherche, de filtrage,
 * de pagination et de tri utilisées par les services de l'application.
 */
public interface ItemRepository extends JpaRepository<Item, Integer> {

    // -------------------------------------------------------------------------
    // SUGGESTIONS DE RECHERCHE
    // -------------------------------------------------------------------------

    /**
     * Recherche les premières suggestions actives correspondant à une partie de titre.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndTitleItemContainingIgnoreCaseOrderByTitleItemAsc(String titlePart);

    /**
     * Recherche les premières suggestions actives correspondant à une partie d'auteur.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndAuthorItemContainingIgnoreCaseOrderByAuthorItemAsc(String authorPart);

    /**
     * Recherche les premières suggestions actives correspondant à une partie d'éditeur.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndPublisherItemContainingIgnoreCaseOrderByPublisherItemAsc(String publisherPart);

    /**
     * Recherche les premières suggestions actives correspondant à une partie de catégorie.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndCategoryItemContainingIgnoreCaseOrderByCategoryItemAsc(String categoryPart);

    /**
     * Recherche les premières suggestions actives correspondant à une partie d'ISBN.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndIsbnItemContainingIgnoreCaseOrderByIsbnItemAsc(String isbnPart);

    // -------------------------------------------------------------------------
    // RECHERCHE TECHNIQUE PAR RÉFÉRENCE SOURCE
    // -------------------------------------------------------------------------

    /**
     * Recherche un objet actif à partir d'une référence source stockée dans tagsItem.
     *
     * Objectif :
     *      - permettre aux initialiseurs de démonstration de retrouver un objet
     *        sans dépendre d'un identifiant technique auto-incrémenté ;
     *      - utiliser une donnée métier stable issue de l'import catalogue :
     *        source_ref:L000xx ou source_ref:D000xx ;
     *      - conserver une logique compatible DEV, DEMO, Railway et future PROD.
     *
     * Exemple de valeur recherchée :
     *      source_ref:L00001
     *
     * La méthode retourne un Optional afin de forcer le code appelant à gérer
     * explicitement le cas où la référence attendue serait absente du catalogue.
     */
    Optional<Item> findFirstByDeletedDateItemIsNullAndTagsItemContaining(String sourceRef);

    // -------------------------------------------------------------------------
    // LECTURE DES OBJETS ACTIFS
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les objets actifs avec un tri fourni dynamiquement.
     */
    List<Item> findByDeletedDateItemIsNull(Sort sort);

    /**
     * Retourne les objets actifs sous forme paginée.
     */
    Page<Item> findByDeletedDateItemIsNull(Pageable pageable);

    // -------------------------------------------------------------------------
    // RECHERCHE MULTICRITÈRE
    // -------------------------------------------------------------------------

    /**
     * Recherche paginée dans les objets actifs du catalogue.
     *
     * La recherche couvre les principaux champs consultables côté interface :
     * titre, auteur, éditeur, catégorie, ISBN, tags, description et identifiant.
     *
     * Le countQuery est défini explicitement afin de sécuriser la pagination.
     */
    @Query(
            value = """
                    SELECT i
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                      AND (
                            LOWER(COALESCE(i.titleItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.authorItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.publisherItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.categoryItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.isbnItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.tagsItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.descriptionItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(CONCAT('', i.idItem)) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                      AND (
                            LOWER(COALESCE(i.titleItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.authorItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.publisherItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.categoryItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.isbnItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.tagsItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.descriptionItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(CONCAT('', i.idItem)) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    """
    )
    Page<Item> searchActiveItems(@Param("q") String q, Pageable pageable);

    // -------------------------------------------------------------------------
    // RECHERCHE MULTICRITÈRE AVEC TRI MÉTIER
    // -------------------------------------------------------------------------

    /**
     * Recherche paginée dans les objets actifs avec classement par disponibilité.
     *
     * Le CASE impose un ordre métier stable :
     * AVAILABLE, UNAVAILABLE, DAMAGED, LOST, puis les autres valeurs éventuelles.
     *
     * Le tri secondaire par titre puis identifiant garantit un affichage stable
     * lorsque plusieurs objets partagent le même statut.
     */
    @Query(
            value = """
                    SELECT i
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                      AND (
                            LOWER(COALESCE(i.titleItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.authorItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.publisherItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.categoryItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.isbnItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.tagsItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.descriptionItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(CONCAT('', i.idItem)) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    ORDER BY
                        CASE
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.AVAILABLE THEN 1
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.UNAVAILABLE THEN 2
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.DAMAGED THEN 3
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.LOST THEN 4
                            ELSE 99
                        END,
                        i.titleItem ASC,
                        i.idItem ASC
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                      AND (
                            LOWER(COALESCE(i.titleItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.authorItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.publisherItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.categoryItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.isbnItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.tagsItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.descriptionItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(CONCAT('', i.idItem)) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    """
    )
    Page<Item> searchActiveItemsOrderByStatusRankThenTitleThenId(@Param("q") String q, Pageable pageable);

    /**
     * Recherche paginée dans les objets actifs avec classement par état matériel.
     *
     * Le CASE impose un ordre métier stable :
     * NEW, GOOD, USED, DAMAGED, puis les autres valeurs éventuelles.
     *
     * Le tri secondaire par titre puis identifiant garantit un affichage stable
     * lorsque plusieurs objets partagent le même état.
     */
    @Query(
            value = """
                    SELECT i
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                      AND (
                            LOWER(COALESCE(i.titleItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.authorItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.publisherItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.categoryItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.isbnItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.tagsItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.descriptionItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(CONCAT('', i.idItem)) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    ORDER BY
                        CASE
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.NEW THEN 1
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.GOOD THEN 2
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.USED THEN 3
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.DAMAGED THEN 4
                            ELSE 99
                        END,
                        i.titleItem ASC,
                        i.idItem ASC
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                      AND (
                            LOWER(COALESCE(i.titleItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.authorItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.publisherItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.categoryItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.isbnItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.tagsItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(i.descriptionItem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(CONCAT('', i.idItem)) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                    """
    )
    Page<Item> searchActiveItemsOrderByConditionRankThenTitleThenId(@Param("q") String q, Pageable pageable);

    // -------------------------------------------------------------------------
    // TRI DES OBJETS ACTIFS PAR DISPONIBILITÉ
    // -------------------------------------------------------------------------

    /**
     * Retourne les objets actifs paginés selon le rang métier de disponibilité.
     *
     * Cette requête est utilisée lorsque l'utilisateur consulte le catalogue
     * sans recherche textuelle, avec un tri orienté disponibilité.
     */
    @Query(
            value = """
                    SELECT i
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                    ORDER BY
                        CASE
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.AVAILABLE THEN 1
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.UNAVAILABLE THEN 2
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.DAMAGED THEN 3
                            WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.LOST THEN 4
                            ELSE 99
                        END,
                        i.titleItem ASC,
                        i.idItem ASC
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                    """
    )
    Page<Item> findByDeletedDateItemIsNullOrderByStatusRankThenTitleThenId(Pageable pageable);

    /**
     * Retourne la liste complète des objets actifs selon le rang métier de disponibilité.
     *
     * Version non paginée utilisée lorsque le service a besoin d'une collection complète
     * tout en conservant le même ordre d'affichage que la version paginée.
     */
    @Query("""
            SELECT i
            FROM Item i
            WHERE i.deletedDateItem IS NULL
            ORDER BY
                CASE
                    WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.AVAILABLE THEN 1
                    WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.UNAVAILABLE THEN 2
                    WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.DAMAGED THEN 3
                    WHEN i.statusItem = com.magiclibrary.enums.ItemStatus.LOST THEN 4
                    ELSE 99
                END,
                i.titleItem ASC,
                i.idItem ASC
            """)
    List<Item> findByDeletedDateItemIsNullOrderByStatusRankThenTitleThenId();

    // -------------------------------------------------------------------------
    // TRI DES OBJETS ACTIFS PAR ÉTAT MATÉRIEL
    // -------------------------------------------------------------------------

    /**
     * Retourne les objets actifs paginés selon le rang métier de leur état matériel.
     *
     * Cette requête est utilisée lorsque l'utilisateur consulte le catalogue
     * sans recherche textuelle, avec un tri orienté condition physique.
     */
    @Query(
            value = """
                    SELECT i
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                    ORDER BY
                        CASE
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.NEW THEN 1
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.GOOD THEN 2
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.USED THEN 3
                            WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.DAMAGED THEN 4
                            ELSE 99
                        END,
                        i.titleItem ASC,
                        i.idItem ASC
                    """,
            countQuery = """
                    SELECT COUNT(i)
                    FROM Item i
                    WHERE i.deletedDateItem IS NULL
                    """
    )
    Page<Item> findByDeletedDateItemIsNullOrderByConditionRankThenTitleThenId(Pageable pageable);

    /**
     * Retourne la liste complète des objets actifs selon le rang métier de leur état matériel.
     *
     * Version non paginée utilisée lorsque le service a besoin d'une collection complète
     * tout en conservant le même ordre d'affichage que la version paginée.
     */
    @Query("""
            SELECT i
            FROM Item i
            WHERE i.deletedDateItem IS NULL
            ORDER BY
                CASE
                    WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.NEW THEN 1
                    WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.GOOD THEN 2
                    WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.USED THEN 3
                    WHEN i.conditionItem = com.magiclibrary.enums.ItemCondition.DAMAGED THEN 4
                    ELSE 99
                END,
                i.titleItem ASC,
                i.idItem ASC
            """)
    List<Item> findByDeletedDateItemIsNullOrderByConditionRankThenTitleThenId();
}