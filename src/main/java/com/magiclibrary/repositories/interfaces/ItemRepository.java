package com.magiclibrary.repositories.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.magiclibrary.entities.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findTop8ByDeletedDateItemIsNullAndTitleItemContainingIgnoreCaseOrderByTitleItemAsc(String titlePart);

    List<Item> findTop8ByDeletedDateItemIsNullAndAuthorItemContainingIgnoreCaseOrderByAuthorItemAsc(String authorPart);

    List<Item> findTop8ByDeletedDateItemIsNullAndPublisherItemContainingIgnoreCaseOrderByPublisherItemAsc(String publisherPart);

    List<Item> findTop8ByDeletedDateItemIsNullAndCategoryItemContainingIgnoreCaseOrderByCategoryItemAsc(String categoryPart);

    List<Item> findTop8ByDeletedDateItemIsNullAndIsbnItemContainingIgnoreCaseOrderByIsbnItemAsc(String isbnPart);

    List<Item> findByDeletedDateItemIsNull(Sort sort);

    Page<Item> findByDeletedDateItemIsNull(Pageable pageable);

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