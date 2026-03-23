package com.magiclibrary.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.mappers.ItemMapper;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.services.ItemService;

@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final String SORT_AVAILABILITY_THEN_TITLE = "availabilityThenTitle";
    private static final String SORT_TITLE_ASC = "titleAsc";
    private static final String SORT_TITLE_DESC = "titleDesc";
    private static final String SORT_AUTHOR_THEN_TITLE = "authorThenTitle";
    private static final String SORT_PUBLISHER_THEN_TITLE = "publisherThenTitle";
    private static final String SORT_CATEGORY_THEN_TITLE = "categoryThenTitle";
    private static final String SORT_STATUS_THEN_TITLE = "statusThenTitle";
    private static final String SORT_CONDITION_THEN_TITLE = "conditionThenTitle";
    private static final String SORT_NEWEST = "newest";

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemServiceImpl(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Override
    public ItemResponseDTO getItemById(Integer id) {
        Item item = itemRepository.findById(id)
                .filter(i -> i.getDeletedDateItem() == null)
                .orElseThrow(() -> ItemNotFoundException.forId(id));

        return itemMapper.toResponse(item);
    }

    @Override
    public List<ItemResponseDTO> getAllItems() {
        return getAllItems(SORT_AVAILABILITY_THEN_TITLE);
    }

    @Override
    public List<ItemResponseDTO> getAllItems(String sort) {

        String key = normalizeSort(sort);

        List<Item> items;

        if (SORT_STATUS_THEN_TITLE.equals(key)) {
            items = itemRepository.findByDeletedDateItemIsNullOrderByStatusRankThenTitleThenId();
        } else if (SORT_CONDITION_THEN_TITLE.equals(key)) {
            items = itemRepository.findByDeletedDateItemIsNullOrderByConditionRankThenTitleThenId();
        } else {
            Sort resolvedSort = resolveSort(key);
            items = itemRepository.findByDeletedDateItemIsNull(resolvedSort);
        }

        return items.stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ItemResponseDTO> getItemsPage(String q, String sort, int page, int size) {

        String normalizedSort = normalizeSort(sort);
        String normalizedQuery = normalizeQuery(q);
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        Page<Item> itemsPage;

        if (normalizedQuery.isEmpty()) {
            itemsPage = getItemsPageWithoutSearch(normalizedSort, safePage, safeSize);
        } else {
            itemsPage = getItemsPageWithSearch(normalizedQuery, normalizedSort, safePage, safeSize);
        }

        List<ItemResponseDTO> content = itemsPage.getContent().stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, itemsPage.getPageable(), itemsPage.getTotalElements());
    }

    private Page<Item> getItemsPageWithoutSearch(String sort, int page, int size) {

        if (SORT_STATUS_THEN_TITLE.equals(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return itemRepository.findByDeletedDateItemIsNullOrderByStatusRankThenTitleThenId(pageable);
        }

        if (SORT_CONDITION_THEN_TITLE.equals(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return itemRepository.findByDeletedDateItemIsNullOrderByConditionRankThenTitleThenId(pageable);
        }

        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return itemRepository.findByDeletedDateItemIsNull(pageable);
    }

    private Page<Item> getItemsPageWithSearch(String q, String sort, int page, int size) {

        if (SORT_STATUS_THEN_TITLE.equals(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return itemRepository.searchActiveItemsOrderByStatusRankThenTitleThenId(q, pageable);
        }

        if (SORT_CONDITION_THEN_TITLE.equals(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return itemRepository.searchActiveItemsOrderByConditionRankThenTitleThenId(q, pageable);
        }

        Pageable pageable = PageRequest.of(page, size, resolveSearchSort(sort));
        return itemRepository.searchActiveItems(q, pageable);
    }

    private static String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SORT_AVAILABILITY_THEN_TITLE;
        }
        return sort.trim();
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return "";
        }
        return q.trim();
    }

    private static Sort resolveSearchSort(String key) {

        return switch (key) {

            case SORT_TITLE_ASC -> Sort.by(
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_TITLE_DESC -> Sort.by(
                    Sort.Order.desc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_AUTHOR_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("authorItem").ignoreCase().nullsLast(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_PUBLISHER_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("publisherItem").ignoreCase().nullsLast(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_CATEGORY_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("categoryItem").ignoreCase(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_STATUS_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("statusItem").ignoreCase().nullsLast(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_CONDITION_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("conditionItem").ignoreCase().nullsLast(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_NEWEST -> Sort.by(
                    Sort.Order.desc("addedDateItem"),
                    Sort.Order.desc("idItem")
            );

            case SORT_AVAILABILITY_THEN_TITLE, "" -> Sort.by(
                    Sort.Order.desc("availableItem"),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            default -> Sort.by(
                    Sort.Order.desc("availableItem"),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );
        };
    }

    private static Sort resolveSort(String key) {

        return switch (key) {

            case SORT_TITLE_ASC -> Sort.by(
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_TITLE_DESC -> Sort.by(
                    Sort.Order.desc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_AUTHOR_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("authorItem").ignoreCase().nullsLast(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_PUBLISHER_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("publisherItem").ignoreCase().nullsLast(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_CATEGORY_THEN_TITLE -> Sort.by(
                    Sort.Order.asc("categoryItem").ignoreCase(),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            case SORT_NEWEST -> Sort.by(
                    Sort.Order.desc("addedDateItem"),
                    Sort.Order.desc("idItem")
            );

            case SORT_AVAILABILITY_THEN_TITLE, "" -> Sort.by(
                    Sort.Order.desc("availableItem"),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );

            default -> Sort.by(
                    Sort.Order.desc("availableItem"),
                    Sort.Order.asc("titleItem").ignoreCase(),
                    Sort.Order.asc("idItem")
            );
        };
    }
}