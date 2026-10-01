package com.magiclibrary.services.impl;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.item.ItemRequestDTO;
import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.enums.ItemFormat;
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.LanguageCode;
import com.magiclibrary.enums.LoanLineStatus;
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.mappers.ItemMapper;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.repositories.interfaces.LoanLineRepository;
import com.magiclibrary.services.ItemService;

/**
 * =============================================================================
 * SERVICE IMPLEMENTATION : ITEM
 * =============================================================================
 * Implémentation de la couche métier dédiée au catalogue numérique
 * MagicLibrary.
 *
 * Cette classe centralise :
 * - la consultation d'un objet actif ;
 * - la consultation complète du catalogue ;
 * - la recherche multicritère ;
 * - les différents tris métier ;
 * - la pagination ;
 * - la préparation des catégories proposées dans les formulaires ;
 * - la validation et la normalisation des données de classification ;
 * - la création administrative d'un objet ;
 * - la modification administrative d'un objet ;
 * - l'archivage logique d'un objet ;
 * - la consultation administrative des objets archivés ;
 * - la restauration d'un objet archivé.
 *
 * GESTION DES OBJETS ARCHIVÉS
 * -----------------------------------------------------------------------------
 * Un objet est considéré comme actif lorsque deletedDateItem est null.
 *
 * L'archivage est volontairement logique et non physique :
 *
 *     deletedDateItem != null
 *
 * Cela permet :
 * - de retirer l'objet du catalogue courant ;
 * - de préserver son historique ;
 * - de conserver les relations LoanLine -> Item ;
 * - d'éviter toute rupture de cohérence avec les anciens emprunts ;
 * - de permettre une restauration ultérieure de l'objet.
 *
 * La restauration consiste uniquement à remettre deletedDateItem à null.
 *
 * Elle ne recrée pas l'objet et ne modifie pas :
 * - son identifiant ;
 * - son statut métier ;
 * - sa disponibilité ;
 * - son état matériel ;
 * - ses données fonctionnelles ;
 * - ses relations historiques.
 *
 * CLASSIFICATION
 * -----------------------------------------------------------------------------
 * La catégorie reste une donnée thématique stockée sous forme de texte.
 *
 * Le formulaire doit privilégier une catégorie déjà connue du catalogue.
 * Lorsqu'une nouvelle catégorie doit réellement être créée, le formulaire
 * utilise la valeur technique __NEW__ et transmet la nouvelle catégorie dans
 * newCategoryItem.
 *
 * Les valeurs manifestement impropres à une catégorie thématique, notamment
 * "???" ou une valeur correspondant à un format documentaire tel que LIVRE
 * ou DVD, ne sont jamais proposées comme catégories disponibles.
 *
 * FORMAT
 * -----------------------------------------------------------------------------
 * Le format reste stocké sous forme de String dans Item, mais sa valeur est
 * désormais contrôlée à partir du référentiel ItemFormat.
 *
 * Une variante reconnue est toujours convertie vers son code canonique :
 *
 *     Livre / livre / LIVRE -> LIVRE
 *
 * Une valeur inconnue est refusée.
 *
 * Le format reste facultatif.
 *
 * LANGUE
 * -----------------------------------------------------------------------------
 * La langue reste stockée sous forme de String.
 *
 * Lorsqu'elle est renseignée, elle doit correspondre à l'un des codes définis
 * dans LanguageCode. La valeur est enregistrée en majuscules.
 *
 * La langue reste facultative.
 *
 * DISPONIBILITÉ
 * -----------------------------------------------------------------------------
 * Le booléen availableItem n'est jamais saisi directement par
 * l'administrateur.
 *
 * Il est déterminé automatiquement à partir du statut fonctionnel :
 *
 *     AVAILABLE   -> true
 *     UNAVAILABLE -> false
 *     DAMAGED     -> false
 *     LOST        -> false
 *
 * Cette règle évite toute combinaison incohérente entre statusItem et
 * availableItem.
 *
 * TRANSACTIONS
 * -----------------------------------------------------------------------------
 * La classe est en lecture seule par défaut.
 *
 * Les opérations d'écriture utilisent explicitement @Transactional afin
 * d'autoriser les créations, modifications, archivages et restaurations.
 * =============================================================================
 */
@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    // -------------------------------------------------------------------------
    // CLÉS DE TRI SUPPORTÉES
    // -------------------------------------------------------------------------

    private static final String SORT_AVAILABILITY_THEN_TITLE = "availabilityThenTitle";
    private static final String SORT_TITLE_ASC = "titleAsc";
    private static final String SORT_TITLE_DESC = "titleDesc";
    private static final String SORT_AUTHOR_THEN_TITLE = "authorThenTitle";
    private static final String SORT_PUBLISHER_THEN_TITLE = "publisherThenTitle";
    private static final String SORT_CATEGORY_THEN_TITLE = "categoryThenTitle";
    private static final String SORT_STATUS_THEN_TITLE = "statusThenTitle";
    private static final String SORT_CONDITION_THEN_TITLE = "conditionThenTitle";
    private static final String SORT_NEWEST = "newest";

    // -------------------------------------------------------------------------
    // VALEURS TECHNIQUES DU FORMULAIRE DE CLASSIFICATION
    // -------------------------------------------------------------------------

    /**
     * Valeur technique utilisée par le futur select de catégorie lorsque
     * l'administrateur choisit explicitement de créer une nouvelle catégorie.
     *
     * Cette valeur ne doit jamais être persistée dans Item.categoryItem.
     */
    private static final String NEW_CATEGORY_SENTINEL = "__NEW__";

    /**
     * Valeur historique identifiée comme anomalie dans le catalogue importé.
     *
     * Elle ne doit pas être proposée comme une catégorie fonctionnelle.
     */
    private static final String UNKNOWN_CATEGORY_VALUE = "???";

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    private final ItemRepository itemRepository;
    private final LoanLineRepository loanLineRepository;
    private final ItemMapper itemMapper;

    /**
     * Injection explicite des dépendances nécessaires à la gestion du
     * catalogue.
     *
     * @param itemRepository repository des objets
     * @param loanLineRepository repository permettant notamment de contrôler
     *                           les emprunts actifs avant archivage
     * @param itemMapper mapper Item / DTO
     */
    public ItemServiceImpl(
            ItemRepository itemRepository,
            LoanLineRepository loanLineRepository,
            ItemMapper itemMapper
    ) {
        this.itemRepository = itemRepository;
        this.loanLineRepository = loanLineRepository;
        this.itemMapper = itemMapper;
    }

    // -------------------------------------------------------------------------
    // CONSULTATION D'UN OBJET ACTIF
    // -------------------------------------------------------------------------

    /**
     * Retourne un objet actif à partir de son identifiant.
     *
     * Un objet archivé est volontairement assimilé à un objet non disponible
     * dans le catalogue courant.
     */
    @Override
    public ItemResponseDTO getItemById(Integer id) {

        Item item = getActiveItemEntity(id);

        return itemMapper.toResponse(item);
    }

    // -------------------------------------------------------------------------
    // CONSULTATION COMPLÈTE DU CATALOGUE
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les objets actifs avec le tri métier par défaut.
     */
    @Override
    public List<ItemResponseDTO> getAllItems() {
        return getAllItems(SORT_AVAILABILITY_THEN_TITLE);
    }

    /**
     * Retourne tous les objets actifs selon la stratégie de tri demandée.
     *
     * Les tris sur le statut et l'état utilisent des requêtes métier dédiées
     * afin de garantir un ordre fonctionnel stable.
     */
    @Override
    public List<ItemResponseDTO> getAllItems(String sort) {

        String key = normalizeSort(sort);

        List<Item> items;

        if (SORT_STATUS_THEN_TITLE.equals(key)) {

            items = itemRepository
                    .findByDeletedDateItemIsNullOrderByStatusRankThenTitleThenId();

        } else if (SORT_CONDITION_THEN_TITLE.equals(key)) {

            items = itemRepository
                    .findByDeletedDateItemIsNullOrderByConditionRankThenTitleThenId();

        } else {

            Sort resolvedSort = resolveSort(key);

            items = itemRepository.findByDeletedDateItemIsNull(resolvedSort);
        }

        return items.stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // CONSULTATION DES OBJETS ARCHIVÉS
    // -------------------------------------------------------------------------

    /**
     * Retourne tous les objets actuellement archivés.
     *
     * Les objets sont récupérés selon l'ordre défini dans le repository :
     * - date d'archivage décroissante ;
     * - titre croissant ;
     * - identifiant croissant.
     *
     * Cette méthode est strictement en lecture seule.
     */
    @Override
    public List<ItemResponseDTO> getArchivedItems() {

        return itemRepository
                .findByDeletedDateItemIsNotNullOrderByDeletedDateItemDescTitleItemAscIdItemAsc()
                .stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // CATÉGORIES DISPONIBLES POUR L'ADMINISTRATION
    // -------------------------------------------------------------------------

    /**
     * Retourne les catégories pouvant être proposées dans les formulaires
     * administratifs.
     *
     * La liste brute provenant du repository est nettoyée avant exposition :
     * - suppression des valeurs nulles ou vides ;
     * - suppression de la valeur historique "???" ;
     * - suppression des valeurs correspondant à un format connu ;
     * - suppression des doublons sans tenir compte de la casse ;
     * - tri alphabétique sans tenir compte de la casse.
     *
     * Exemple :
     *
     *     cartes
     *     close-up
     *     histoire
     *     magie générale
     *
     * Une valeur telle que "Livre" ne peut donc plus devenir automatiquement
     * une catégorie proposée par l'interface.
     */
    @Override
    public List<String> getAvailableCategories() {

        TreeSet<String> categories =
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        itemRepository.findDistinctCategoryItems()
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .filter(value -> !isInvalidCategoryValue(value))
                .forEach(categories::add);

        return List.copyOf(categories);
    }

    // -------------------------------------------------------------------------
    // RECHERCHE, TRI ET PAGINATION
    // -------------------------------------------------------------------------

    /**
     * Retourne une page d'objets actifs.
     *
     * La méthode :
     * - sécurise les valeurs de pagination ;
     * - normalise la recherche ;
     * - normalise le tri ;
     * - sélectionne la stratégie de requête appropriée ;
     * - convertit les entités en DTO de réponse.
     */
    @Override
    public Page<ItemResponseDTO> getItemsPage(
            String q,
            String sort,
            int page,
            int size
    ) {

        String normalizedSort = normalizeSort(sort);
        String normalizedQuery = normalizeQuery(q);

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        Page<Item> itemsPage;

        if (normalizedQuery.isEmpty()) {

            itemsPage = getItemsPageWithoutSearch(
                    normalizedSort,
                    safePage,
                    safeSize
            );

        } else {

            itemsPage = getItemsPageWithSearch(
                    normalizedQuery,
                    normalizedSort,
                    safePage,
                    safeSize
            );
        }

        List<ItemResponseDTO> content = itemsPage.getContent()
                .stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(
                content,
                itemsPage.getPageable(),
                itemsPage.getTotalElements()
        );
    }

    // -------------------------------------------------------------------------
    // CRÉATION D'UN OBJET
    // -------------------------------------------------------------------------

    /**
     * Crée un nouvel objet actif dans le catalogue.
     *
     * Les informations techniques sont déterminées ici et ne proviennent jamais
     * du formulaire administrateur.
     *
     * Avant le mapping :
     * - la catégorie est résolue et contrôlée ;
     * - le format est validé puis converti vers sa valeur canonique ;
     * - la langue est validée puis normalisée.
     *
     * Lors de la création :
     * - l'identifiant sera généré automatiquement par la base ;
     * - addedDateItem prend la date et l'heure courantes ;
     * - updatedAtItem reste null ;
     * - deletedDateItem reste null ;
     * - availableItem est calculé à partir de statusItem.
     */
    @Override
    @Transactional
    public ItemResponseDTO createItem(ItemRequestDTO request) {

        prepareRequestForPersistence(request);

        Item item = itemMapper.toEntity(request);

        item.setAddedDateItem(LocalDateTime.now());
        item.setUpdatedAtItem(null);
        item.setDeletedDateItem(null);

        synchronizeAvailabilityWithStatus(item);

        Item savedItem = itemRepository.save(item);

        return itemMapper.toResponse(savedItem);
    }

    // -------------------------------------------------------------------------
    // MODIFICATION D'UN OBJET
    // -------------------------------------------------------------------------

    /**
     * Modifie les données fonctionnelles d'un objet actif existant.
     *
     * Les données techniques historiques sont préservées :
     * - l'identifiant ne change pas ;
     * - la date d'ajout ne change pas ;
     * - la date d'archivage ne peut pas être manipulée par ce traitement.
     *
     * Avant le mapping :
     * - la catégorie est résolue et contrôlée ;
     * - le format est validé puis converti vers sa valeur canonique ;
     * - la langue est validée puis normalisée.
     *
     * La date de dernière modification est actualisée automatiquement.
     */
    @Override
    @Transactional
    public ItemResponseDTO updateItem(
            Integer id,
            ItemRequestDTO request
    ) {

        Item item = getActiveItemEntity(id);

        prepareRequestForPersistence(request);

        itemMapper.updateEntity(request, item);

        item.setUpdatedAtItem(LocalDateTime.now());

        synchronizeAvailabilityWithStatus(item);

        Item savedItem = itemRepository.save(item);

        return itemMapper.toResponse(savedItem);
    }

    // -------------------------------------------------------------------------
    // ARCHIVAGE LOGIQUE
    // -------------------------------------------------------------------------

    /**
     * Archive un objet du catalogue sans suppression physique.
     *
     * Règle métier :
     * un objet encore présent dans une ligne d'emprunt ACTIVE ne peut pas être
     * archivé.
     *
     * Les emprunts RETURNED ou LOST restent conservés dans l'historique et
     * n'empêchent pas à eux seuls l'archivage.
     *
     * Une fois deletedDateItem renseignée, toutes les requêtes normales du
     * catalogue excluent automatiquement l'objet.
     */
    @Override
    @Transactional
    public void archiveItem(Integer id) {

        Item item = getActiveItemEntity(id);

        boolean activeLoanExists =
                loanLineRepository.existsByItem_IdItemAndStatusLoanLine(
                        id,
                        LoanLineStatus.ACTIVE
                );

        if (activeLoanExists) {
            throw new IllegalStateException(
                    "Impossible d'archiver cet objet : "
                            + "un emprunt actif lui est encore associé."
            );
        }

        item.setDeletedDateItem(LocalDate.now());
        item.setUpdatedAtItem(LocalDateTime.now());

        itemRepository.save(item);
    }

    // -------------------------------------------------------------------------
    // RESTAURATION D'UN OBJET ARCHIVÉ
    // -------------------------------------------------------------------------

    /**
     * Restaure un objet précédemment archivé.
     *
     * Cette opération ne crée aucune nouvelle entité et ne modifie aucune
     * donnée fonctionnelle de l'objet.
     *
     * Seules les informations techniques liées au cycle de vie sont modifiées :
     * - deletedDateItem redevient null afin que l'objet soit de nouveau actif ;
     * - updatedAtItem est actualisé afin de matérialiser la restauration.
     *
     * Le statut et availableItem sont volontairement conservés tels quels.
     *
     * Exemple :
     * un objet archivé avec le statut UNAVAILABLE sera restauré en restant
     * UNAVAILABLE.
     */
    @Override
    @Transactional
    public void restoreItem(Integer id) {

        Item item = getArchivedItemEntity(id);

        item.setDeletedDateItem(null);
        item.setUpdatedAtItem(LocalDateTime.now());

        itemRepository.save(item);
    }

    // -------------------------------------------------------------------------
    // PRÉPARATION DES DONNÉES SAISIES AVANT PERSISTANCE
    // -------------------------------------------------------------------------

    /**
     * Prépare les données administratives avant leur transmission au mapper.
     *
     * Cette méthode constitue la frontière métier empêchant un POST manuel ou
     * altéré de contourner les listes proposées par l'interface.
     *
     * Les trois données contrôlées ici sont :
     * - catégorie ;
     * - format ;
     * - langue.
     *
     * Les valeurs ne sont injectées dans le DTO qu'après validation complète
     * afin d'éviter de modifier partiellement la requête lorsqu'une erreur est
     * détectée.
     *
     * @param request données reçues depuis le formulaire
     */
    private void prepareRequestForPersistence(ItemRequestDTO request) {

        if (request == null) {
            throw new IllegalStateException(
                    "Les données de l'objet sont absentes."
            );
        }

        String resolvedCategory = resolveCategory(request);

        String normalizedFormat =
                normalizeAndValidateFormat(request.getFormatItem());

        String normalizedLanguage =
                normalizeAndValidateLanguage(request.getLanguageItem());

        request.setCategoryItem(resolvedCategory);
        request.setFormatItem(normalizedFormat);
        request.setLanguageItem(normalizedLanguage);
    }

    // -------------------------------------------------------------------------
    // RÉSOLUTION DE LA CATÉGORIE
    // -------------------------------------------------------------------------

    /**
     * Détermine la catégorie finale à enregistrer.
     *
     * Deux parcours sont supportés.
     *
     * 1. Catégorie existante :
     *    categoryItem contient directement une catégorie proposée par
     *    l'application.
     *
     * 2. Nouvelle catégorie :
     *    categoryItem vaut "__NEW__" et la véritable valeur est reçue dans
     *    newCategoryItem.
     *
     * Une catégorie existante est réutilisée avec son écriture canonique déjà
     * présente en base.
     *
     * Une nouvelle catégorie portant le même nom qu'une catégorie existante,
     * mais avec une casse différente, est également ramenée à la valeur déjà
     * existante afin d'éviter :
     *
     *     cartes
     *     Cartes
     *     CARTES
     *
     * @param request données du formulaire
     * @return catégorie finale à stocker
     */
    private String resolveCategory(ItemRequestDTO request) {

        String selectedCategory = request.getCategoryItem();

        if (selectedCategory == null || selectedCategory.isBlank()) {
            throw new IllegalStateException(
                    "La catégorie est obligatoire."
            );
        }

        selectedCategory = selectedCategory.trim();

        List<String> availableCategories = getAvailableCategories();

        if (NEW_CATEGORY_SENTINEL.equals(selectedCategory)) {

            String newCategory = request.getNewCategoryItem();

            if (newCategory == null || newCategory.isBlank()) {
                throw new IllegalStateException(
                        "Veuillez renseigner la nouvelle catégorie."
                );
            }

            newCategory = newCategory.trim();

            validateCategoryValue(newCategory);

            return findExistingCategoryIgnoreCase(
                    availableCategories,
                    newCategory
            ).orElse(newCategory);
        }

        if (isInvalidCategoryValue(selectedCategory)) {
            throw new IllegalStateException(
                    "La catégorie sélectionnée n'est pas valide."
            );
        }

        return findExistingCategoryIgnoreCase(
                availableCategories,
                selectedCategory
        ).orElseThrow(() -> new IllegalStateException(
                "La catégorie sélectionnée n'est pas reconnue. "
                        + "Utilisez une catégorie existante ou choisissez "
                        + "explicitement « Nouvelle catégorie »."
        ));
    }

    // -------------------------------------------------------------------------
    // VALIDATION D'UNE NOUVELLE CATÉGORIE
    // -------------------------------------------------------------------------

    /**
     * Contrôle une nouvelle catégorie avant son utilisation.
     *
     * La longueur est également contrôlée dans le DTO, mais le contrôle métier
     * est volontairement répété ici afin de protéger le service lorsqu'il est
     * appelé autrement que depuis un formulaire validé par Spring MVC.
     *
     * @param category catégorie à contrôler
     */
    private static void validateCategoryValue(String category) {

        if (category.length() < 2 || category.length() > 50) {
            throw new IllegalStateException(
                    "La catégorie doit contenir entre 2 et 50 caractères."
            );
        }

        if (isInvalidCategoryValue(category)) {
            throw new IllegalStateException(
                    "Cette valeur ne peut pas être utilisée comme catégorie."
            );
        }
    }

    // -------------------------------------------------------------------------
    // DÉTECTION D'UNE VALEUR DE CATÉGORIE INVALIDE
    // -------------------------------------------------------------------------

    /**
     * Identifie les valeurs qui ne doivent jamais être utilisées comme
     * catégories thématiques.
     *
     * Sont actuellement refusées :
     * - la valeur historique "???" ;
     * - la sentinelle technique "__NEW__" ;
     * - toute valeur reconnue comme un format ItemFormat.
     *
     * Cela empêche notamment de recréer l'anomalie :
     *
     *     categoryItem = "Livre"
     *
     * @param value valeur à contrôler
     * @return true lorsque la valeur ne doit pas être utilisée comme catégorie
     */
    private static boolean isInvalidCategoryValue(String value) {

        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.trim();

        if (UNKNOWN_CATEGORY_VALUE.equalsIgnoreCase(normalized)) {
            return true;
        }

        if (NEW_CATEGORY_SENTINEL.equalsIgnoreCase(normalized)) {
            return true;
        }

        return ItemFormat.fromValue(normalized) != null;
    }

    // -------------------------------------------------------------------------
    // RECHERCHE D'UNE CATÉGORIE EXISTANTE SANS TENIR COMPTE DE LA CASSE
    // -------------------------------------------------------------------------

    /**
     * Recherche une catégorie existante en ignorant la casse.
     *
     * La valeur originale déjà présente dans le catalogue est retournée afin
     * de conserver une écriture cohérente.
     *
     * @param categories catégories disponibles
     * @param expected valeur recherchée
     * @return catégorie correspondante éventuelle
     */
    private static java.util.Optional<String> findExistingCategoryIgnoreCase(
            List<String> categories,
            String expected
    ) {

        return categories.stream()
                .filter(category -> category.equalsIgnoreCase(expected))
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // NORMALISATION ET VALIDATION DU FORMAT
    // -------------------------------------------------------------------------

    /**
     * Contrôle et normalise le format.
     *
     * Le format reste facultatif.
     *
     * Une valeur reconnue est convertie vers son code canonique :
     *
     *     Livre -> LIVRE
     *     livre -> LIVRE
     *     dvd   -> DVD
     *
     * Une valeur non supportée est refusée, y compris lorsqu'elle provient
     * d'un POST modifié manuellement.
     *
     * @param format valeur reçue
     * @return code canonique ou null lorsque le format n'est pas renseigné
     */
    private static String normalizeAndValidateFormat(String format) {

        if (format == null || format.isBlank()) {
            return null;
        }

        if (!ItemFormat.isSupported(format)) {
            throw new IllegalStateException(
                    "Le format sélectionné n'est pas reconnu."
            );
        }

        return ItemFormat.canonicalCodeOf(format);
    }

    // -------------------------------------------------------------------------
    // NORMALISATION ET VALIDATION DE LA LANGUE
    // -------------------------------------------------------------------------

    /**
     * Contrôle et normalise le code langue.
     *
     * La langue reste facultative.
     *
     * Lorsqu'une valeur est renseignée :
     * - les espaces périphériques sont supprimés ;
     * - la valeur est convertie en majuscules ;
     * - elle doit correspondre à une constante de LanguageCode.
     *
     * Exemples :
     *
     *     fr -> FR
     *     En -> EN
     *
     * @param language valeur reçue
     * @return code normalisé ou null lorsque la langue n'est pas renseignée
     */
    private static String normalizeAndValidateLanguage(String language) {

        if (language == null || language.isBlank()) {
            return null;
        }

        String normalized =
                language.trim().toUpperCase(Locale.ROOT);

        for (LanguageCode languageCode : LanguageCode.values()) {

            if (languageCode.name().equals(normalized)) {
                return normalized;
            }
        }

        throw new IllegalStateException(
                "La langue sélectionnée n'est pas reconnue."
        );
    }

    // -------------------------------------------------------------------------
    // LECTURE INTERNE D'UN OBJET ACTIF
    // -------------------------------------------------------------------------

    /**
     * Recherche l'entité Item correspondant à un objet actif.
     *
     * Cette méthode centralise la règle :
     *
     *     deletedDateItem == null
     *
     * Elle évite de dupliquer le même contrôle dans getItemById(),
     * updateItem() et archiveItem().
     *
     * @param id identifiant technique recherché
     * @return entité active correspondante
     * @throws ItemNotFoundException si l'objet est absent ou déjà archivé
     */
    private Item getActiveItemEntity(Integer id) {

        return itemRepository.findById(id)
                .filter(item -> item.getDeletedDateItem() == null)
                .orElseThrow(() -> ItemNotFoundException.forId(id));
    }

    // -------------------------------------------------------------------------
    // LECTURE INTERNE D'UN OBJET ARCHIVÉ
    // -------------------------------------------------------------------------

    /**
     * Recherche l'entité Item correspondant à un objet actuellement archivé.
     *
     * Cette méthode applique la règle inverse de getActiveItemEntity() :
     *
     *     deletedDateItem != null
     *
     * Elle garantit qu'une opération de restauration ne puisse être appliquée
     * qu'à un objet réellement archivé.
     *
     * Un identifiant inexistant ou correspondant à un objet déjà actif est
     * volontairement traité comme une ressource non restaurable.
     *
     * @param id identifiant technique recherché
     * @return entité archivée correspondante
     * @throws ItemNotFoundException si l'objet est absent ou déjà actif
     */
    private Item getArchivedItemEntity(Integer id) {

        return itemRepository.findById(id)
                .filter(item -> item.getDeletedDateItem() != null)
                .orElseThrow(() -> ItemNotFoundException.forId(id));
    }

    // -------------------------------------------------------------------------
    // SYNCHRONISATION STATUT / DISPONIBILITÉ
    // -------------------------------------------------------------------------

    /**
     * Synchronise availableItem avec statusItem.
     *
     * La disponibilité booléenne est dérivée du statut métier :
     *
     *     AVAILABLE -> true
     *     tout autre statut -> false
     *
     * Cette centralisation garantit qu'aucune opération de création ou
     * modification ne puisse enregistrer un couple incohérent.
     */
    private static void synchronizeAvailabilityWithStatus(Item item) {

        ItemStatus status = item.getStatusItem();

        item.setAvailableItem(status == ItemStatus.AVAILABLE);
    }

    // -------------------------------------------------------------------------
    // PAGINATION SANS RECHERCHE TEXTUELLE
    // -------------------------------------------------------------------------

    /**
     * Retourne une page d'objets actifs sans filtre textuel.
     *
     * Les tris métier complexes disposent de requêtes spécifiques.
     * Les autres tris utilisent le mécanisme Sort de Spring Data.
     */
    private Page<Item> getItemsPageWithoutSearch(
            String sort,
            int page,
            int size
    ) {

        if (SORT_STATUS_THEN_TITLE.equals(sort)) {

            Pageable pageable = PageRequest.of(page, size);

            return itemRepository
                    .findByDeletedDateItemIsNullOrderByStatusRankThenTitleThenId(
                            pageable
                    );
        }

        if (SORT_CONDITION_THEN_TITLE.equals(sort)) {

            Pageable pageable = PageRequest.of(page, size);

            return itemRepository
                    .findByDeletedDateItemIsNullOrderByConditionRankThenTitleThenId(
                            pageable
                    );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                resolveSort(sort)
        );

        return itemRepository.findByDeletedDateItemIsNull(pageable);
    }

    // -------------------------------------------------------------------------
    // PAGINATION AVEC RECHERCHE TEXTUELLE
    // -------------------------------------------------------------------------

    /**
     * Retourne une page d'objets actifs correspondant à une recherche.
     *
     * Les tris statut / état utilisent les requêtes JPQL dédiées du repository.
     */
    private Page<Item> getItemsPageWithSearch(
            String q,
            String sort,
            int page,
            int size
    ) {

        if (SORT_STATUS_THEN_TITLE.equals(sort)) {

            Pageable pageable = PageRequest.of(page, size);

            return itemRepository
                    .searchActiveItemsOrderByStatusRankThenTitleThenId(
                            q,
                            pageable
                    );
        }

        if (SORT_CONDITION_THEN_TITLE.equals(sort)) {

            Pageable pageable = PageRequest.of(page, size);

            return itemRepository
                    .searchActiveItemsOrderByConditionRankThenTitleThenId(
                            q,
                            pageable
                    );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                resolveSearchSort(sort)
        );

        return itemRepository.searchActiveItems(q, pageable);
    }

    // -------------------------------------------------------------------------
    // NORMALISATION DU TRI
    // -------------------------------------------------------------------------

    /**
     * Normalise la clé de tri reçue.
     *
     * En l'absence de valeur exploitable, le tri par disponibilité puis titre
     * est utilisé.
     */
    private static String normalizeSort(String sort) {

        if (sort == null || sort.isBlank()) {
            return SORT_AVAILABILITY_THEN_TITLE;
        }

        return sort.trim();
    }

    // -------------------------------------------------------------------------
    // NORMALISATION DE LA RECHERCHE
    // -------------------------------------------------------------------------

    /**
     * Normalise le terme de recherche afin de garantir une valeur exploitable.
     */
    private static String normalizeQuery(String q) {

        if (q == null) {
            return "";
        }

        return q.trim();
    }

    // -------------------------------------------------------------------------
    // RÉSOLUTION DU TRI POUR UNE RECHERCHE
    // -------------------------------------------------------------------------

    /**
     * Construit le tri Spring Data applicable aux recherches textuelles.
     *
     * Les tris métier bénéficiant d'une requête dédiée restent également
     * définis ici à titre de repli cohérent.
     */
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

    // -------------------------------------------------------------------------
    // RÉSOLUTION DU TRI STANDARD
    // -------------------------------------------------------------------------

    /**
     * Construit le tri Spring Data utilisé pour la consultation du catalogue
     * sans recherche textuelle.
     */
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