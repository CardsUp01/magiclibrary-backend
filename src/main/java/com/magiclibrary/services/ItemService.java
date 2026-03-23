package com.magiclibrary.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.magiclibrary.dto.item.ItemResponseDTO;

public interface ItemService {

    ItemResponseDTO getItemById(Integer id);

    List<ItemResponseDTO> getAllItems();

    default List<ItemResponseDTO> getAllItems(String sort) {
        return getAllItems();
    }

    Page<ItemResponseDTO> getItemsPage(String q, String sort, int page, int size);
}