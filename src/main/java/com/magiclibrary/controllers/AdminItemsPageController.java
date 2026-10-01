package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS SPRING MVC / SECURITY
// -----------------------------------------------------------------------------
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.item.ItemRequestDTO;
import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.enums.ItemCondition;
import com.magiclibrary.enums.ItemFormat;
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.LanguageCode;
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.services.ItemService;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;

/**
 * =============================================================================
 * CONTROLLER SSR : ADMINISTRATION DU CATALOGUE
 * =============================================================================
 * Contrôleur réservé aux administrateurs pour la gestion fonctionnelle des
 * objets de la bibliothèque numérique MagicLibrary.
 *
 * Ce contrôleur permet :
 * - l'affichage du formulaire de création d'un objet ;
 * - la création d'un nouvel objet ;
 * - l'affichage du formulaire de modification d'un objet actif ;
 * - la modification d'un objet existant ;
 * - l'archivage logique d'un objet ;
 * - la consultation administrative des objets archivés ;
 * - la restauration d'un objet archivé.
 *
 * Les formulaires de création et de modification reçoivent également les
 * référentiels nécessaires à une saisie contrôlée :
 * - catégories métier disponibles ;
 * - formats supportés ;
 * - langues supportées ;
 * - statuts ;
 * - états matériels.
 *
 * IMPORTANT :
 * aucune suppression physique d'un Item n'est exposée depuis l'interface
 * d'administration.
 *
 * L'archivage repose sur deletedDateItem et permet de préserver :
 * - l'historique du catalogue ;
 * - les anciennes lignes d'emprunt ;
 * - les relations LoanLine -> Item.
 *
 * Un objet encore associé à une ligne d'emprunt ACTIVE ne peut pas être
 * archivé.
 *
 * La restauration effectue l'opération inverse de l'archivage logique :
 * deletedDateItem redevient null sans recréer l'objet ni altérer son statut,
 * sa disponibilité ou son historique.
 *
 * Toutes les routes de ce contrôleur sont strictement réservées au rôle ADMIN.
 * =============================================================================
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminItemsPageController {

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    private final ItemService itemService;

    /**
     * Injection du service métier responsable du catalogue.
     *
     * @param itemService service de gestion des objets
     */
    public AdminItemsPageController(ItemService itemService) {
        this.itemService = itemService;
    }

    // -------------------------------------------------------------------------
    // CRÉATION : AFFICHAGE DU FORMULAIRE
    // -------------------------------------------------------------------------

    /**
     * Affiche le formulaire permettant à un administrateur d'ajouter un objet
     * au catalogue.
     *
     * Un DTO vierge est créé uniquement lorsqu'aucun formulaire n'est déjà
     * présent dans le modèle.
     *
     * @param model modèle Thymeleaf
     * @return template de création d'objet
     */
    @GetMapping("/admin/catalogue/ajouter")
    public String showItemCreatePage(Model model) {

        if (!model.containsAttribute("itemRequestDTO")) {
            model.addAttribute("itemRequestDTO", new ItemRequestDTO());
        }

        populateFormModel(
                model,
                "Ajouter un objet",
                false,
                null
        );

        return "admin/ajout-objet";
    }

    // -------------------------------------------------------------------------
    // CRÉATION : TRAITEMENT DU FORMULAIRE
    // -------------------------------------------------------------------------

    /**
     * Traite la création d'un nouvel objet.
     *
     * En cas d'erreur de validation, le formulaire est réaffiché avec les
     * valeurs précédemment saisies.
     *
     * Après création, l'administrateur est redirigé vers la fiche du nouvel
     * objet.
     *
     * @param itemRequestDTO données saisies
     * @param bindingResult résultat de la validation Bean Validation
     * @param model modèle Thymeleaf
     * @param redirectAttributes attributs flash
     * @return redirection ou formulaire de création
     */
    @PostMapping("/admin/catalogue/ajouter")
    public String createItem(
            @Valid @ModelAttribute("itemRequestDTO") ItemRequestDTO itemRequestDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {

            populateFormModel(
                    model,
                    "Ajouter un objet",
                    false,
                    null
            );

            model.addAttribute(
                    "errorMessage",
                    "Merci de corriger les champs du formulaire."
            );

            return "admin/ajout-objet";
        }

        try {

            ItemResponseDTO createdItem = itemService.createItem(itemRequestDTO);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "L’objet a été ajouté au catalogue avec succès."
            );

            return "redirect:/bibliotheque/item/" + createdItem.getIdItem();

        } catch (IllegalStateException e) {

            populateFormModel(
                    model,
                    "Ajouter un objet",
                    false,
                    null
            );

            model.addAttribute("errorMessage", e.getMessage());

            return "admin/ajout-objet";
        }
    }

    // -------------------------------------------------------------------------
    // MODIFICATION : AFFICHAGE DU FORMULAIRE
    // -------------------------------------------------------------------------

    /**
     * Affiche le formulaire de modification d'un objet actif.
     *
     * Les informations actuelles de l'objet sont converties en ItemRequestDTO
     * afin de préremplir correctement le formulaire.
     *
     * @param id identifiant technique de l'objet
     * @param model modèle Thymeleaf
     * @param redirectAttributes attributs flash
     * @return formulaire de modification ou redirection vers le catalogue
     */
    @GetMapping("/admin/catalogue/{id}/modifier")
    public String showItemEditPage(
            @PathVariable("id") Integer id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            ItemResponseDTO item = itemService.getItemById(id);

            if (!model.containsAttribute("itemRequestDTO")) {
                model.addAttribute(
                        "itemRequestDTO",
                        toRequestDTO(item)
                );
            }

            populateFormModel(
                    model,
                    "Modifier un objet",
                    true,
                    id
            );

            model.addAttribute("item", item);

            return "admin/modifier-objet";

        } catch (ItemNotFoundException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "L’objet demandé n’existe pas ou a déjà été archivé."
            );

            return "redirect:/bibliotheque-numerique";
        }
    }

    // -------------------------------------------------------------------------
    // MODIFICATION : TRAITEMENT DU FORMULAIRE
    // -------------------------------------------------------------------------

    /**
     * Traite la modification d'un objet actif.
     *
     * La couche service conserve les données techniques historiques et met
     * automatiquement à jour :
     * - updatedAtItem ;
     * - availableItem selon le statut sélectionné.
     *
     * En cas d'erreur de validation ou d'erreur métier, le contexte complet
     * nécessaire au template de modification est réinjecté dans le modèle.
     *
     * @param id identifiant technique de l'objet
     * @param itemRequestDTO nouvelles données fonctionnelles
     * @param bindingResult résultat de validation
     * @param model modèle Thymeleaf
     * @param redirectAttributes attributs flash
     * @return redirection ou formulaire de modification
     */
    @PostMapping("/admin/catalogue/{id}/modifier")
    public String updateItem(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("itemRequestDTO") ItemRequestDTO itemRequestDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {

            try {

                populateEditFormModel(
                        model,
                        id
                );

            } catch (ItemNotFoundException e) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "L’objet demandé n’existe pas ou a déjà été archivé."
                );

                return "redirect:/bibliotheque-numerique";
            }

            model.addAttribute(
                    "errorMessage",
                    "Merci de corriger les champs du formulaire."
            );

            return "admin/modifier-objet";
        }

        try {

            ItemResponseDTO updatedItem =
                    itemService.updateItem(id, itemRequestDTO);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "L’objet a été modifié avec succès."
            );

            return "redirect:/bibliotheque/item/" + updatedItem.getIdItem();

        } catch (ItemNotFoundException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "L’objet demandé n’existe pas ou a déjà été archivé."
            );

            return "redirect:/bibliotheque-numerique";

        } catch (IllegalStateException e) {

            try {

                populateEditFormModel(
                        model,
                        id
                );

            } catch (ItemNotFoundException notFoundException) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "L’objet demandé n’existe pas ou a déjà été archivé."
                );

                return "redirect:/bibliotheque-numerique";
            }

            model.addAttribute("errorMessage", e.getMessage());

            return "admin/modifier-objet";
        }
    }

    // -------------------------------------------------------------------------
    // ARCHIVAGE LOGIQUE
    // -------------------------------------------------------------------------

    /**
     * Archive un objet actif.
     *
     * L'opération :
     * - ne supprime aucune ligne SQL ;
     * - conserve les références historiques ;
     * - retire l'objet du catalogue actif ;
     * - est refusée si une LoanLine ACTIVE référence encore l'objet.
     *
     * @param id identifiant technique de l'objet
     * @param redirectAttributes attributs flash
     * @return redirection vers le catalogue
     */
    @PostMapping("/admin/catalogue/{id}/archiver")
    public String archiveItem(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            itemService.archiveItem(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "L’objet a été archivé avec succès."
            );

        } catch (ItemNotFoundException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "L’objet demandé n’existe pas ou a déjà été archivé."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/bibliotheque-numerique";
    }

    // -------------------------------------------------------------------------
    // ARCHIVES : CONSULTATION
    // -------------------------------------------------------------------------

    /**
     * Affiche la liste administrative des objets archivés.
     *
     * Cette page est séparée du catalogue actif afin qu'un objet archivé reste
     * invisible dans les consultations normales tout en pouvant être retrouvé
     * par un administrateur.
     *
     * Les objets sont fournis par le service selon l'ordre métier suivant :
     * - date d'archivage la plus récente ;
     * - titre ;
     * - identifiant technique.
     *
     * @param model modèle Thymeleaf
     * @return template d'administration des objets archivés
     */
    @GetMapping("/admin/catalogue/archives")
    public String showArchivedItemsPage(Model model) {

        model.addAttribute(
                "archivedItems",
                itemService.getArchivedItems()
        );

        model.addAttribute(
                "pageTitle",
                "Objets archivés"
        );

        return "admin/objets-archives";
    }

    // -------------------------------------------------------------------------
    // ARCHIVES : RESTAURATION
    // -------------------------------------------------------------------------

    /**
     * Restaure un objet précédemment archivé.
     *
     * La restauration :
     * - ne crée aucune nouvelle ligne ;
     * - conserve l'identifiant technique ;
     * - conserve le statut et la disponibilité existants ;
     * - conserve l'historique de l'objet ;
     * - remet deletedDateItem à null.
     *
     * Après restauration, l'objet disparaît naturellement de la liste des
     * archives et redevient visible dans le catalogue actif.
     *
     * @param id identifiant technique de l'objet à restaurer
     * @param redirectAttributes attributs flash
     * @return redirection vers la liste des objets archivés
     */
    @PostMapping("/admin/catalogue/{id}/restaurer")
    public String restoreItem(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            itemService.restoreItem(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "L’objet a été restauré avec succès."
            );

        } catch (ItemNotFoundException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "L’objet demandé n’existe pas ou n’est plus archivé."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/catalogue/archives";
    }

    // -------------------------------------------------------------------------
    // PRÉPARATION DU MODÈLE DES FORMULAIRES
    // -------------------------------------------------------------------------

    /**
     * Centralise les données communes nécessaires aux formulaires de création
     * et de modification.
     *
     * Les référentiels sont fournis par leur source métier appropriée :
     *
     * - catégories :
     *   récupérées depuis le service afin que la liste soit nettoyée et
     *   contrôlée avant son exposition à l'interface ;
     *
     * - formats :
     *   issus de ItemFormat ;
     *
     * - langues :
     *   issues de LanguageCode ;
     *
     * - statuts et états :
     *   issus directement de leurs ENUM métier.
     *
     * Cette centralisation garantit également que les listes sont à nouveau
     * présentes lorsque le formulaire est réaffiché après une erreur.
     *
     * @param model modèle Thymeleaf
     * @param pageTitle titre de la page
     * @param editMode true pour une modification, false pour une création
     * @param itemId identifiant de l'objet en modification
     */
    private void populateFormModel(
            Model model,
            String pageTitle,
            boolean editMode,
            Integer itemId
    ) {

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("editMode", editMode);
        model.addAttribute("itemId", itemId);

        model.addAttribute(
                "itemCategories",
                itemService.getAvailableCategories()
        );

        model.addAttribute(
                "itemFormats",
                ItemFormat.values()
        );

        model.addAttribute(
                "languageCodes",
                LanguageCode.values()
        );

        model.addAttribute(
                "itemStatuses",
                ItemStatus.values()
        );

        model.addAttribute(
                "itemConditions",
                ItemCondition.values()
        );
    }

    // -------------------------------------------------------------------------
    // PRÉPARATION DU CONTEXTE SPÉCIFIQUE AU FORMULAIRE DE MODIFICATION
    // -------------------------------------------------------------------------

    /**
     * Réinjecte dans le modèle toutes les données nécessaires au template
     * modifier-objet.html lorsqu'une modification doit être réaffichée après
     * une erreur.
     *
     * Le DTO soumis par l'utilisateur reste déjà présent dans le modèle grâce
     * au binding Spring MVC.
     *
     * Cette méthode recharge donc uniquement :
     * - les référentiels communs du formulaire ;
     * - l'objet actuellement enregistré utilisé notamment par l'en-tête et les
     *   actions contextuelles du template.
     *
     * @param model modèle Thymeleaf
     * @param id identifiant de l'objet en cours de modification
     * @throws ItemNotFoundException si l'objet n'existe plus ou a été archivé
     */
    private void populateEditFormModel(
            Model model,
            Integer id
    ) {

        populateFormModel(
                model,
                "Modifier un objet",
                true,
                id
        );

        model.addAttribute(
                "item",
                itemService.getItemById(id)
        );
    }

    // -------------------------------------------------------------------------
    // CONVERSION RESPONSE DTO -> REQUEST DTO POUR L'ÉDITION
    // -------------------------------------------------------------------------

    /**
     * Construit un ItemRequestDTO à partir des données d'un objet existant.
     *
     * Cette conversion est utilisée uniquement pour préremplir le formulaire
     * SSR de modification.
     *
     * Les données techniques telles que l'identifiant ou les dates ne sont
     * volontairement pas copiées dans le DTO de saisie.
     *
     * newCategoryItem n'est pas prérempli :
     * il ne sert que lorsqu'un administrateur choisit explicitement de créer
     * une nouvelle catégorie.
     *
     * @param item objet actuellement enregistré
     * @return DTO prêt à être utilisé par le formulaire
     */
    private static ItemRequestDTO toRequestDTO(ItemResponseDTO item) {

        ItemRequestDTO request = new ItemRequestDTO();

        request.setTitleItem(item.getTitleItem());
        request.setCategoryItem(item.getCategoryItem());
        request.setAuthorItem(item.getAuthorItem());
        request.setPublisherItem(item.getPublisherItem());
        request.setPublishYearItem(item.getPublishYearItem());
        request.setEditionItem(item.getEditionItem());
        request.setIsbnItem(item.getIsbnItem());
        request.setPageCountItem(item.getPageCountItem());

        request.setDescriptionItem(item.getDescriptionItem());
        request.setTagsItem(item.getTagsItem());
        request.setFormatItem(item.getFormatItem());
        request.setLanguageItem(item.getLanguageItem());

        if (item.getConditionItem() != null
                && !item.getConditionItem().isBlank()) {

            request.setConditionItem(
                    ItemCondition.valueOf(item.getConditionItem())
            );
        }

        if (item.getStatusItem() != null
                && !item.getStatusItem().isBlank()) {

            request.setStatusItem(
                    ItemStatus.valueOf(item.getStatusItem())
            );
        }

        request.setCoverUrlItem(item.getCoverUrlItem());

        return request;
    }
}