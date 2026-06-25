package com.magiclibrary.repositories.interfaces;

import java.util.List;

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

    /*
     * Suggestions de recherche par titre.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndTitleItemContainingIgnoreCaseOrderByTitleItemAsc(String titlePart);

    /*
     * Suggestions de recherche par auteur.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndAuthorItemContainingIgnoreCaseOrderByAuthorItemAsc(String authorPart);

    /*
     * Suggestions de recherche par éditeur.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndPublisherItemContainingIgnoreCaseOrderByPublisherItemAsc(String publisherPart);

    /*
     * Suggestions de recherche par catégorie.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndCategoryItemContainingIgnoreCaseOrderByCategoryItemAsc(String categoryPart);

    /*
     * Suggestions de recherche par ISBN.
     */
    List<Item> findTop8ByDeletedDateItemIsNullAndIsbnItemContainingIgnoreCaseOrderByIsbnItemAsc(String isbnPart);

    /*
     * Retourne tous les objets actifs avec un tri fourni dynamiquement.
     */
    List<Item> findByDeletedDateItemIsNull(Sort sort);

    /*
     * Retourne les objets actifs sous forme paginée.
     */
    Page<Item> findByDeletedDateItemIsNull(Pageable pageable);

    /*
     * Recherche multicritère dans les objets actifs du catalogue.
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

    /*
     * Recherche multicritère avec classement par disponibilité,
     * puis par titre et identifiant.
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

    /*
     * Recherche multicritère avec classement par état,
     * puis par titre et identifiant.
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

    /*
     * Retourne les objets actifs triés selon le rang de disponibilité.
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

    /*
     * Retourne la liste complète des objets actifs triés selon le rang de disponibilité.
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

    /*
     * Retourne les objets actifs triés selon le rang d'état.
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

    /*
     * Retourne la liste complète des objets actifs triés selon le rang d'état.
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