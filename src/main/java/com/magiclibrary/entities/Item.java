package com.magiclibrary.entities;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS JPA (explicites, jamais de wildcard)
// -----------------------------------------------------------------------------
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION (Bean Validation Jakarta)
// -----------------------------------------------------------------------------
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.ItemCondition;

/**
 * =============================================================================
 *  ENTITY : ITEM
 * =============================================================================
 *  Représente un objet du catalogue numérique MagicLibrary : livre, DVD,
 *  magazine, accessoire, etc.
 *
 *  Cette entité est strictement conforme :
 *      - au dictionnaire de données validé ;
 *      - au MCD / MLD / MPD ;
 *      - aux règles métiers (disponibilité, statut, conditions physiques) ;
 *      - aux conventions d’écriture MagicLibrary (booléens, dates, formats).
 *
 *  Les champs status_item et condition_item utilisent des ENUMS métier :
 *      - ItemStatus : AVAILABLE, UNAVAILABLE, DAMAGED, LOST
 *      - ItemCondition : NEW, GOOD, USED, DAMAGED
 *
 *  Le stockage SQL utilise EnumType.STRING pour garantir lisibilité,
 *  maintenabilité et éviter les effets indésirables en cas d’ajout ou de
 *  réorganisation des valeurs d’énumération.
 *
 *  Aucun attribut n'a été inventé. Aucune logique métier n’est présente ici.
 * =============================================================================
 */
@Entity
@Table(name = "item")
public class Item {

    // -------------------------------------------------------------------------
    // IDENTIFIANT TECHNIQUE (PRIMARY KEY)
    // -------------------------------------------------------------------------

    /**
     * Identifiant unique de l’objet.
     * Clé primaire : id_item (INT AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item", nullable = false)
    private Integer idItem;

    // -------------------------------------------------------------------------
    // INFORMATIONS D’IDENTITÉ & CLASSIFICATION
    // -------------------------------------------------------------------------

    @Column(name = "title_item", length = 150, nullable = false)
    @NotBlank(message = "Le titre est obligatoire.")
    @Size(min = 2, max = 150,
            message = "Le titre doit contenir entre 2 et 150 caractères.")
    private String titleItem;

    @Column(name = "category_item", length = 50, nullable = false)
    @NotBlank(message = "La catégorie est obligatoire.")
    @Size(min = 2, max = 50,
            message = "La catégorie doit contenir entre 2 et 50 caractères.")
    private String categoryItem;

    @Column(name = "author_item", length = 100)
    @Size(max = 100, message = "Le nom de l’auteur ne doit pas dépasser 100 caractères.")
    private String authorItem;

    @Column(name = "publisher_item", length = 100)
    @Size(max = 100, message = "L’éditeur ne doit pas dépasser 100 caractères.")
    private String publisherItem;

    @Column(name = "publish_year_item")
    private Integer publishYearItem;

    @Column(name = "edition_item", length = 50)
    @Size(max = 50, message = "L’édition ne doit pas dépasser 50 caractères.")
    private String editionItem;

    @Column(name = "isbn_item", length = 20)
    @Size(max = 20, message = "L’ISBN ne doit pas dépasser 20 caractères.")
    private String isbnItem;

    @Column(name = "page_count_item")
    private Integer pageCountItem;

    // -------------------------------------------------------------------------
    // CHAMPS MÉTIER DÉTAILLÉS
    // -------------------------------------------------------------------------

    @Column(name = "description_item", columnDefinition = "TEXT")
    @Size(min = 2, max = 10_000,
            message = "La description doit contenir entre 2 et 10 000 caractères.")
    private String descriptionItem;

    @Column(name = "tags_item", columnDefinition = "TEXT")
    @Size(min = 2, max = 10_000,
            message = "Les mots-clés doivent contenir entre 2 et 10 000 caractères.")
    private String tagsItem;

    @Column(name = "format_item", length = 100)
    @Size(min = 3, max = 100,
            message = "Le format doit contenir entre 3 et 100 caractères.")
    private String formatItem;

    @Column(name = "language_item", length = 10)
    @Size(min = 2, max = 10,
            message = "La langue doit contenir entre 2 et 10 caractères.")
    private String languageItem;

    // -------------------------------------------------------------------------
    // ENUMS : ÉTAT PHYSIQUE & STATUT FONCTIONNEL
    // -------------------------------------------------------------------------

    /**
     * État physique de l’objet (liste fermée).
     * Valeurs possibles : NEW, GOOD, USED, DAMAGED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_item", length = 50)
    private ItemCondition conditionItem;

    /**
     * Statut fonctionnel de l’objet (liste fermée).
     * Valeurs possibles : AVAILABLE, UNAVAILABLE, DAMAGED, LOST.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_item", length = 50, nullable = false)
    @NotNull(message = "Le statut est obligatoire.")
    private ItemStatus statusItem;

    // -------------------------------------------------------------------------
    // DISPONIBILITÉ
    // -------------------------------------------------------------------------

    @Column(name = "available_item", nullable = false)
    @NotNull(message = "La disponibilité est obligatoire.")
    private Boolean availableItem;

    // -------------------------------------------------------------------------
    // MÉTADONNÉES VISUELLES
    // -------------------------------------------------------------------------

    @Column(name = "cover_url_item", length = 300)
    @Size(max = 300, message = "L’URL de couverture ne doit pas dépasser 300 caractères.")
    private String coverUrlItem;

    // -------------------------------------------------------------------------
    // DATES TECHNIQUES
    // -------------------------------------------------------------------------

    @Column(name = "added_date_item", nullable = false)
    @NotNull(message = "La date d’ajout est obligatoire.")
    @PastOrPresent(message = "La date d’ajout ne peut pas être future.")
    private LocalDateTime addedDateItem;

    @Column(name = "updated_at_item")
    private LocalDateTime updatedAtItem;

    @Column(name = "deleted_date_item")
    private LocalDate deletedDateItem;

    // -------------------------------------------------------------------------
    // CONSTRUCTEURS
    // -------------------------------------------------------------------------

    public Item() {
    }

    // -------------------------------------------------------------------------
    // GETTERS & SETTERS
    // -------------------------------------------------------------------------

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

    public Boolean getAvailableItem() {
        return availableItem;
    }

    public void setAvailableItem(Boolean availableItem) {
        this.availableItem = availableItem;
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

    // -------------------------------------------------------------------------
    // MÉTHODES UTILITAIRES
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return Objects.equals(idItem, item.idItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idItem);
    }

    @Override
    public String toString() {
        return "Item{" +
                "idItem=" + idItem +
                ", titleItem='" + titleItem + '\'' +
                '}';
    }
}
