package com.magiclibrary.dto.item;

import com.magiclibrary.enums.ItemCondition;
import com.magiclibrary.enums.ItemStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * =============================================================================
 * DTO : ITEM REQUEST
 * =============================================================================
 * Données saisissables par un administrateur lors de la création ou de la
 * modification d'un objet du catalogue MagicLibrary.
 *
 * Ce DTO ne contient volontairement aucune donnée technique :
 * - identifiant ;
 * - date d'ajout ;
 * - date de mise à jour ;
 * - date d'archivage ;
 * - disponibilité booléenne.
 *
 * Ces informations sont déterminées exclusivement par l'application afin
 * d'éviter toute incohérence métier.
 *
 * La disponibilité est notamment déduite automatiquement du statut de l'objet :
 * - AVAILABLE   -> disponible ;
 * - UNAVAILABLE -> indisponible ;
 * - DAMAGED     -> indisponible ;
 * - LOST        -> indisponible.
 *
 * CLASSIFICATION
 * -----------------------------------------------------------------------------
 * categoryItem reste la valeur principale utilisée pour représenter la
 * catégorie sélectionnée dans le formulaire.
 *
 * newCategoryItem est un champ exclusivement destiné au formulaire
 * d'administration. Il permet de saisir explicitement une nouvelle catégorie
 * lorsque l'administrateur choisit l'option correspondante.
 *
 * Ce champ complémentaire :
 * - n'existe pas dans l'entité Item ;
 * - n'est pas persisté directement ;
 * - sera interprété par la couche métier avant le mapping vers l'entité.
 *
 * Cette séparation permet de conserver le modèle Item actuel tout en évitant
 * que la saisie libre soit le fonctionnement normal du champ Catégorie.
 * =============================================================================
 */
public class ItemRequestDTO {

    // -------------------------------------------------------------------------
    // IDENTITÉ ET CLASSIFICATION
    // -------------------------------------------------------------------------

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(min = 2, max = 150,
            message = "Le titre doit contenir entre 2 et 150 caractères.")
    private String titleItem;

    /**
     * Catégorie sélectionnée dans la liste proposée par l'application.
     *
     * La catégorie reste obligatoire.
     *
     * Lorsque l'administrateur choisira explicitement de créer une nouvelle
     * catégorie, la couche métier utilisera newCategoryItem pour déterminer
     * la valeur finale à enregistrer.
     */
    @NotBlank(message = "La catégorie est obligatoire.")
    @Size(min = 2, max = 50,
            message = "La catégorie doit contenir entre 2 et 50 caractères.")
    private String categoryItem;

    /**
     * Nouvelle catégorie saisie explicitement par l'administrateur.
     *
     * Ce champ est facultatif et n'est utilisé que lorsque le formulaire
     * indique qu'une nouvelle catégorie doit être créée.
     *
     * Il ne correspond à aucune colonne de l'entité Item.
     */
    @Size(max = 50,
            message = "La nouvelle catégorie ne doit pas dépasser 50 caractères.")
    private String newCategoryItem;

    @Size(max = 100,
            message = "Le nom de l’auteur ne doit pas dépasser 100 caractères.")
    private String authorItem;

    @Size(max = 100,
            message = "L’éditeur ne doit pas dépasser 100 caractères.")
    private String publisherItem;

    private Integer publishYearItem;

    @Size(max = 50,
            message = "L’édition ne doit pas dépasser 50 caractères.")
    private String editionItem;

    @Size(max = 20,
            message = "L’ISBN ne doit pas dépasser 20 caractères.")
    private String isbnItem;

    @Min(value = 1,
            message = "Le nombre de pages doit être supérieur ou égal à 1.")
    private Integer pageCountItem;

    // -------------------------------------------------------------------------
    // INFORMATIONS COMPLÉMENTAIRES
    // -------------------------------------------------------------------------

    @Size(max = 10_000,
            message = "La description ne doit pas dépasser 10 000 caractères.")
    private String descriptionItem;

    @Size(max = 10_000,
            message = "Les mots-clés ne doivent pas dépasser 10 000 caractères.")
    private String tagsItem;

    /**
     * Format fonctionnel de l'objet.
     *
     * Le type reste volontairement String dans le DTO afin de conserver
     * la compatibilité avec l'entité et le schéma SQL existants.
     *
     * Les valeurs réellement autorisées seront contrôlées par la couche
     * métier à partir du référentiel ItemFormat.
     */
    @Size(max = 100,
            message = "Le format ne doit pas dépasser 100 caractères.")
    private String formatItem;

    /**
     * Code langue éventuellement renseigné pour l'objet.
     *
     * Le champ reste facultatif et utilise une String pour rester compatible
     * avec le stockage actuel.
     *
     * Les valeurs autorisées seront contrôlées à partir du référentiel
     * LanguageCode existant.
     */
    @Size(max = 10,
            message = "La langue ne doit pas dépasser 10 caractères.")
    private String languageItem;

    // -------------------------------------------------------------------------
    // ÉTAT ET STATUT MÉTIER
    // -------------------------------------------------------------------------

    private ItemCondition conditionItem;

    /**
     * Le statut est obligatoire.
     *
     * availableItem n'est volontairement pas exposé dans ce DTO :
     * sa valeur sera calculée automatiquement par la couche métier.
     */
    @NotNull(message = "Le statut est obligatoire.")
    private ItemStatus statusItem;

    // -------------------------------------------------------------------------
    // MÉTADONNÉE VISUELLE
    // -------------------------------------------------------------------------

    @Size(max = 300,
            message = "L’URL de couverture ne doit pas dépasser 300 caractères.")
    private String coverUrlItem;

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR
    // -------------------------------------------------------------------------

    public ItemRequestDTO() {
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

    public String getTitleItem() {
        return titleItem;
    }

    public void setTitleItem(String titleItem) {
        this.titleItem = titleItem;
    }

    public String getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(String categoryItem) {
        this.categoryItem = categoryItem;
    }

    public String getNewCategoryItem() {
        return newCategoryItem;
    }

    public void setNewCategoryItem(String newCategoryItem) {
        this.newCategoryItem = newCategoryItem;
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

    public ItemCondition getConditionItem() {
        return conditionItem;
    }

    public void setConditionItem(ItemCondition conditionItem) {
        this.conditionItem = conditionItem;
    }

    public ItemStatus getStatusItem() {
        return statusItem;
    }

    public void setStatusItem(ItemStatus statusItem) {
        this.statusItem = statusItem;
    }

    public String getCoverUrlItem() {
        return coverUrlItem;
    }

    public void setCoverUrlItem(String coverUrlItem) {
        this.coverUrlItem = coverUrlItem;
    }
}