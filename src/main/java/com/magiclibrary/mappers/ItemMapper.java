package com.magiclibrary.mappers;

import org.springframework.stereotype.Component;

import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.enums.LanguageCode;

@Component
public class ItemMapper {

    public ItemResponseDTO toResponse(Item entity) {

        if (entity == null) {
            return null;
        }

        ItemResponseDTO dto = new ItemResponseDTO();

        dto.setIdItem(entity.getIdItem());
        dto.setTitleItem(entity.getTitleItem());
        dto.setCategoryItem(entity.getCategoryItem());
        dto.setAuthorItem(entity.getAuthorItem());
        dto.setPublisherItem(entity.getPublisherItem());
        dto.setPublishYearItem(entity.getPublishYearItem());
        dto.setEditionItem(entity.getEditionItem());
        dto.setIsbnItem(entity.getIsbnItem());
        dto.setPageCountItem(entity.getPageCountItem());
        dto.setDescriptionItem(entity.getDescriptionItem());
        dto.setTagsItem(entity.getTagsItem());
        dto.setFormatItem(entity.getFormatItem());

        String languageCode = entity.getLanguageItem();
        dto.setLanguageItem(languageCode);
        dto.setLanguageLabel(LanguageCode.labelOf(languageCode));

        if (entity.getConditionItem() != null) {
            dto.setConditionItem(entity.getConditionItem().name());
            dto.setConditionLabel(entity.getConditionItem().getLabel());
        } else {
            dto.setConditionItem(null);
            dto.setConditionLabel(null);
        }

        if (entity.getStatusItem() != null) {
            dto.setStatusItem(entity.getStatusItem().name());
            dto.setStatusLabel(entity.getStatusItem().getLabel());
        } else {
            dto.setStatusItem(null);
            dto.setStatusLabel(null);
        }

        dto.setAvailableItem(entity.getAvailableItem());
        dto.setCoverUrlItem(entity.getCoverUrlItem());
        dto.setAddedDateItem(entity.getAddedDateItem());
        dto.setUpdatedAtItem(entity.getUpdatedAtItem());
        dto.setDeletedDateItem(entity.getDeletedDateItem());

        return dto;
    }
}