package com.magiclibrary.mappers;

import org.springframework.stereotype.Component;

import com.magiclibrary.dto.item.ItemRequestDTO;
import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.enums.LanguageCode;

/**
 * =============================================================================
 * MAPPER : ITEM
 * =============================================================================
 * Assure les conversions entre l'entité Item et les DTO utilisés par
 * MagicLibrary.
 *
 * Responsabilités :
 * - convertir une entité Item en ItemResponseDTO pour l'affichage ;
 * - créer une entité Item à partir d'un ItemRequestDTO ;
 * - appliquer les données modifiables d'un ItemRequestDTO sur une entité
 *   existante.
 *
 * Les données purement techniques ou dérivées ne sont volontairement pas
 * gérées ici :
 * - identifiant technique ;
 * - date d'ajout ;
 * - date de dernière modification ;
 * - date d'archivage ;
 * - disponibilité booléenne.
 *
 * Ces données relèvent de la couche service afin de garantir la cohérence
 * métier du catalogue.
 * =============================================================================
 */
@Component
public class ItemMapper {

    // -------------------------------------------------------------------------
    // ENTITY -> RESPONSE DTO
    // -------------------------------------------------------------------------

    /**
     * Transforme une entité Item en ItemResponseDTO.
     *
     * Le DTO de réponse contient les données brutes ainsi que les libellés
     * destinés à l'affichage dans l'interface utilisateur.
     *
     * @param entity entité Item à convertir
     * @return DTO de réponse correspondant, ou null si l'entité est nulle
     */
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

    // -------------------------------------------------------------------------
    // REQUEST DTO -> NEW ENTITY
    // -------------------------------------------------------------------------

    /**
     * Crée une nouvelle entité Item à partir des données saisies par
     * l'administrateur.
     *
     * Cette méthode ne renseigne volontairement pas :
     * - idItem ;
     * - availableItem ;
     * - addedDateItem ;
     * - updatedAtItem ;
     * - deletedDateItem.
     *
     * Ces valeurs seront déterminées par ItemService selon les règles métier.
     *
     * @param dto données administrateur
     * @return nouvelle entité Item non encore persistée
     */
    public Item toEntity(ItemRequestDTO dto) {

        if (dto == null) {
            return null;
        }

        Item entity = new Item();

        applyEditableFields(dto, entity);

        return entity;
    }

    // -------------------------------------------------------------------------
    // REQUEST DTO -> EXISTING ENTITY
    // -------------------------------------------------------------------------

    /**
     * Met à jour les champs administrativement modifiables d'un objet existant.
     *
     * L'identifiant, les dates techniques, la disponibilité calculée et la date
     * d'archivage ne sont jamais écrasés par les données du formulaire.
     *
     * @param dto données saisies par l'administrateur
     * @param entity entité existante à mettre à jour
     */
    public void updateEntity(ItemRequestDTO dto, Item entity) {

        if (dto == null || entity == null) {
            return;
        }

        applyEditableFields(dto, entity);
    }

    // -------------------------------------------------------------------------
    // MAPPING COMMUN DES CHAMPS ÉDITABLES
    // -------------------------------------------------------------------------

    /**
     * Applique les champs fonctionnels pouvant être renseignés ou modifiés par
     * un administrateur.
     *
     * Les chaînes optionnelles vides sont converties en null afin d'éviter de
     * stocker des valeurs vides et de rester cohérent avec les contraintes de
     * validation de l'entité Item.
     */
    private void applyEditableFields(ItemRequestDTO dto, Item entity) {

        entity.setTitleItem(normalizeRequiredText(dto.getTitleItem()));
        entity.setCategoryItem(normalizeRequiredText(dto.getCategoryItem()));

        entity.setAuthorItem(normalizeOptionalText(dto.getAuthorItem()));
        entity.setPublisherItem(normalizeOptionalText(dto.getPublisherItem()));
        entity.setPublishYearItem(dto.getPublishYearItem());
        entity.setEditionItem(normalizeOptionalText(dto.getEditionItem()));
        entity.setIsbnItem(normalizeOptionalText(dto.getIsbnItem()));
        entity.setPageCountItem(dto.getPageCountItem());

        entity.setDescriptionItem(normalizeOptionalText(dto.getDescriptionItem()));
        entity.setTagsItem(normalizeOptionalText(dto.getTagsItem()));
        entity.setFormatItem(normalizeOptionalText(dto.getFormatItem()));
        entity.setLanguageItem(normalizeOptionalText(dto.getLanguageItem()));

        entity.setConditionItem(dto.getConditionItem());
        entity.setStatusItem(dto.getStatusItem());

        entity.setCoverUrlItem(normalizeOptionalText(dto.getCoverUrlItem()));
    }

    // -------------------------------------------------------------------------
    // NORMALISATION DES DONNÉES TEXTUELLES
    // -------------------------------------------------------------------------

    /**
     * Supprime les espaces inutiles autour d'une donnée obligatoire.
     *
     * La validation Bean Validation reste responsable du contrôle des valeurs
     * nulles ou vides.
     */
    private String normalizeRequiredText(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    /**
     * Normalise une donnée textuelle optionnelle.
     *
     * Une chaîne vide ou uniquement composée d'espaces est convertie en null.
     * Cela évite notamment qu'un champ facultatif soumis vide entre en conflit
     * avec certaines contraintes @Size(min = ...) présentes sur l'entité.
     */
    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}