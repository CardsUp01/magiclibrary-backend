package com.magiclibrary.dto.item;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représente un objet complet du catalogue MagicLibrary.")
public class ItemResponseDTO {

    @Schema(description = "Identifiant unique de l’objet.", example = "12")
    private Integer idItem;

    @Schema(description = "Titre de l’objet.", example = "Grimoire des Ombres")
    private String titleItem;

    @Schema(description = "Catégorie de l’objet.", example = "Livre")
    private String categoryItem;

    @Schema(description = "Auteur de l’objet.", example = "Merlin l’Enchanteur")
    private String authorItem;

    @Schema(description = "Éditeur de l’objet.", example = "Arcana Editions")
    private String publisherItem;

    @Schema(description = "Année de publication.", example = "1998")
    private Integer publishYearItem;

    @Schema(description = "Édition de l’objet.", example = "Edition Collector")
    private String editionItem;

    @Schema(description = "Code ISBN si applicable.", example = "978-2-266-18237-1")
    private String isbnItem;

    @Schema(description = "Nombre de pages.", example = "320")
    private Integer pageCountItem;

    @Schema(description = "Description détaillée.", example = "Un grimoire ancien contenant des rituels oubliés.")
    private String descriptionItem;

    @Schema(description = "Liste de tags séparés par virgules.", example = "magie,rituels,ancien")
    private String tagsItem;

    @Schema(description = "Format (Livre, DVD, Accessoire…).", example = "Livre")
    private String formatItem;

    @Schema(description = "Langue (code).", example = "FR")
    private String languageItem;

    @Schema(description = "État (code).", example = "GOOD")
    private String conditionItem;

    @Schema(description = "Disponibilité actuelle.", example = "true")
    private Boolean availableItem;

    @Schema(description = "Statut (code).", example = "AVAILABLE")
    private String statusItem;

    @Schema(description = "Label utilisateur pour le statut.", example = "Disponible")
    private String statusLabel;

    @Schema(description = "Label utilisateur pour l’état.", example = "Bon état")
    private String conditionLabel;

    @Schema(description = "Label utilisateur pour la langue.", example = "Français")
    private String languageLabel;

    @Schema(description = "URL de la couverture ou de l’image associée.", example = "https://cdn.magiclibrary/items/12.jpg")
    private String coverUrlItem;

    @Schema(description = "Date d’ajout de l’objet dans le catalogue.", example = "2025-01-12T14:22:30")
    private LocalDateTime addedDateItem;

    @Schema(description = "Date de dernière mise à jour.", example = "2025-02-10T09:15:00")
    private LocalDateTime updatedAtItem;

    @Schema(description = "Date de suppression logique si l’objet est archivé.", example = "2025-02-20")
    private LocalDate deletedDateItem;

    public Integer getIdItem() {
        return idItem;
    }

    public void setIdItem(Integer idItem) {
        this.idItem = idItem;
    }

    public String getTitleItem() {
        return titleItem;
    }

    public void setTitleItem(String titleItem) {
        this.titleItem = titleItem;
    }

    public String getTitle() {
        return titleItem;
    }

    public void setTitle(String title) {
        this.titleItem = title;
    }

    public String getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(String categoryItem) {
        this.categoryItem = categoryItem;
    }

    public String getAuthorItem() {
        return authorItem;
    }

    public void setAuthorItem(String authorItem) {
        this.authorItem = authorItem;
    }

    public String getPublisherItem() {
        return publisherItem;
    }

    public void setPublisherItem(String publisherItem) {
        this.publisherItem = publisherItem;
    }

    public Integer getPublishYearItem() {
        return publishYearItem;
    }

    public void setPublishYearItem(Integer publishYearItem) {
        this.publishYearItem = publishYearItem;
    }

    public String getEditionItem() {
        return editionItem;
    }

    public void setEditionItem(String editionItem) {
        this.editionItem = editionItem;
    }

    public String getIsbnItem() {
        return isbnItem;
    }

    public void setIsbnItem(String isbnItem) {
        this.isbnItem = isbnItem;
    }

    public Integer getPageCountItem() {
        return pageCountItem;
    }

    public void setPageCountItem(Integer pageCountItem) {
        this.pageCountItem = pageCountItem;
    }

    public String getDescriptionItem() {
        return descriptionItem;
    }

    public void setDescriptionItem(String descriptionItem) {
        this.descriptionItem = descriptionItem;
    }

    public String getTagsItem() {
        return tagsItem;
    }

    public void setTagsItem(String tagsItem) {
        this.tagsItem = tagsItem;
    }

    public String getFormatItem() {
        return formatItem;
    }

    public void setFormatItem(String formatItem) {
        this.formatItem = formatItem;
    }

    public String getLanguageItem() {
        return languageItem;
    }

    public void setLanguageItem(String languageItem) {
        this.languageItem = languageItem;
    }

    public String getConditionItem() {
        return conditionItem;
    }

    public void setConditionItem(String conditionItem) {
        this.conditionItem = conditionItem;
    }

    public Boolean getAvailableItem() {
        return availableItem;
    }

    public void setAvailableItem(Boolean availableItem) {
        this.availableItem = availableItem;
    }

    public String getStatusItem() {
        return statusItem;
    }

    public void setStatusItem(String statusItem) {
        this.statusItem = statusItem;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public void setConditionLabel(String conditionLabel) {
        this.conditionLabel = conditionLabel;
    }

    public String getLanguageLabel() {
        return languageLabel;
    }

    public void setLanguageLabel(String languageLabel) {
        this.languageLabel = languageLabel;
    }

    public String getCoverUrlItem() {
        return coverUrlItem;
    }

    public void setCoverUrlItem(String coverUrlItem) {
        this.coverUrlItem = coverUrlItem;
    }

    public LocalDateTime getAddedDateItem() {
        return addedDateItem;
    }

    public void setAddedDateItem(LocalDateTime addedDateItem) {
        this.addedDateItem = addedDateItem;
    }

    public LocalDateTime getUpdatedAtItem() {
        return updatedAtItem;
    }

    public void setUpdatedAtItem(LocalDateTime updatedAtItem) {
        this.updatedAtItem = updatedAtItem;
    }

    public LocalDate getDeletedDateItem() {
        return deletedDateItem;
    }

    public void setDeletedDateItem(LocalDate deletedDateItem) {
        this.deletedDateItem = deletedDateItem;
    }
}