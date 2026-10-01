package com.magiclibrary.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.entities.User;
import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.services.ContactService;
import com.magiclibrary.repositories.interfaces.UserRepository;

/**
 * =============================================================================
 * CONTRÔLEUR SSR ADMIN : MESSAGES DE CONTACT
 * =============================================================================
 *
 * Contrôleur réservé à l'administration des messages de contact.
 *
 * Cette classe gère :
 *
 * - les messages actifs ;
 * - les messages archivés ;
 * - la pagination ;
 * - la sélection d'un message ;
 * - la recherche ;
 * - l'autocomplétion ;
 * - la réponse administrative ;
 * - l'archivage logique ;
 * - la restauration.
 *
 * IMPORTANT :
 *
 * L'archivage effectué ici concerne uniquement l'espace ADMIN.
 *
 * Il ne modifie jamais :
 *
 * - l'archivage éventuellement effectué par le membre ;
 * - le statut métier NEW / ANSWERED ;
 * - le contenu du message ;
 * - la réponse administrative.
 *
 * Aucun message n'est supprimé physiquement.
 * =============================================================================
 */
@Controller
public class AdminContactsPageController {

    // -------------------------------------------------------------------------
    // PARAMÈTRES D'AFFICHAGE
    // -------------------------------------------------------------------------

    private static final int CONTACTS_PAGE_SIZE = 9;
    private static final int CONTACTS_SUGGEST_LIMIT = 8;

    private static final String CONTACT_BOX_ACTIVE = "active";
    private static final String CONTACT_BOX_ARCHIVED = "archived";

    private static final DateTimeFormatter CONTACT_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    private final ContactService contactService;
    private final UserRepository userRepository;

    public AdminContactsPageController(
            ContactService contactService,
            UserRepository userRepository
    ) {
        this.contactService = contactService;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // PAGE PRINCIPALE
    // =========================================================================

    /**
     * Affiche la page d'administration des messages de contact.
     *
     * Deux boîtes sont disponibles :
     *
     * - active   : messages actifs côté administration ;
     * - archived : messages archivés côté administration.
     *
     * La recherche, la sélection et la pagination restent strictement
     * cantonnées à la boîte actuellement affichée.
     *
     * Les paramètres provenant éventuellement de la page Notifications sont
     * conservés afin de ne pas casser la navigation existante.
     */
    @GetMapping("/admin/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public String showContactsPage(
            @RequestParam(name = "box", required = false, defaultValue = "active")
            String box,

            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "selectedContactId", required = false)
            String selectedContactId,

            @RequestParam(name = "from", required = false)
            String from,

            @RequestParam(name = "notifPage", required = false)
            Integer notifPage,

            @RequestParam(name = "notifSize", required = false)
            Integer notifSize,

            @RequestParam(name = "notifQ", required = false)
            String notifQ,

            @RequestParam(name = "notifSelectedNotificationId", required = false)
            Integer notifSelectedNotificationId,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            Authentication authentication,
            Model model
    ) {
        String resolvedBox = normalizeContactBox(box);

        int safePage = Math.max(page, 0);
        int safeSize = size > 0
                ? size
                : CONTACTS_PAGE_SIZE;

        String resolvedQuery = q == null
                ? ""
                : q.trim();

        String resolvedFrom = from == null
                ? ""
                : from.trim();

        int resolvedNotifPage = notifPage != null
                ? Math.max(notifPage, 0)
                : 0;

        int resolvedNotifSize =
                notifSize != null && notifSize > 0
                        ? notifSize
                        : CONTACTS_PAGE_SIZE;

        String resolvedNotifQuery = notifQ == null
                ? ""
                : notifQ.trim();

        /*
         * L'identité ADMIN est résolue à partir de l'authentification Spring.
         *
         * Le service effectue ensuite son propre contrôle du rôle ADMIN.
         */
        Integer currentAdminId =
                resolveCurrentAdminUserId(authentication);

        /*
         * On charge uniquement la boîte demandée.
         *
         * L'archivage effectué par le membre n'intervient jamais ici.
         */
        List<ContactResponseDTO> allContacts =
                loadContactsForBox(
                        resolvedBox,
                        currentAdminId
                );

        ContactResponseDTO selectedContact = null;

        if (selectedContactId != null
                && !selectedContactId.isBlank()) {

            String normalizedSelectedContactId =
                    selectedContactId.trim();

            selectedContact = allContacts.stream()
                    .filter(contact ->
                            Objects.equals(
                                    contact.getId(),
                                    normalizedSelectedContactId
                            )
                    )
                    .findFirst()
                    .orElse(null);
        }

        /*
         * La recherche s'effectue uniquement dans la boîte actuellement
         * sélectionnée.
         */
        List<ContactResponseDTO> filteredContacts =
                resolvedQuery.isEmpty()
                        ? allContacts
                        : allContacts.stream()
                        .filter(contact ->
                                matchesContactSearch(
                                        contact,
                                        resolvedQuery
                                )
                        )
                        .toList();

        int resolvedPage =
                resolveContactPageIndex(
                        filteredContacts,
                        selectedContact,
                        safePage,
                        safeSize
                );

        Page<ContactResponseDTO> contactsPage =
                toContactsPage(
                        filteredContacts,
                        resolvedPage,
                        safeSize
                );

        String resolvedSelectedContactId =
                selectedContact != null
                        ? selectedContact.getId()
                        : null;

        // ---------------------------------------------------------------------
        // DONNÉES MÉTIER
        // ---------------------------------------------------------------------

        model.addAttribute(
                "contacts",
                contactsPage.getContent()
        );

        model.addAttribute(
                "selectedContact",
                selectedContact
        );

        model.addAttribute(
                "selectedContactId",
                resolvedSelectedContactId
        );

        // ---------------------------------------------------------------------
        // BOÎTE ACTIVE / ARCHIVÉE
        // ---------------------------------------------------------------------

        model.addAttribute(
                "box",
                resolvedBox
        );

        model.addAttribute(
                "isActiveBox",
                CONTACT_BOX_ACTIVE.equals(resolvedBox)
        );

        model.addAttribute(
                "isArchivedBox",
                CONTACT_BOX_ARCHIVED.equals(resolvedBox)
        );

        // ---------------------------------------------------------------------
        // RECHERCHE / NAVIGATION
        // ---------------------------------------------------------------------

        model.addAttribute(
                "q",
                resolvedQuery
        );

        model.addAttribute(
                "from",
                resolvedFrom
        );

        model.addAttribute(
                "notifPage",
                resolvedNotifPage
        );

        model.addAttribute(
                "notifSize",
                resolvedNotifSize
        );

        model.addAttribute(
                "notifQ",
                resolvedNotifQuery
        );

        model.addAttribute(
                "notifSelectedNotificationId",
                notifSelectedNotificationId
        );

        model.addAttribute(
                "pageTitle",
                "Messages de contact"
        );

        model.addAttribute(
                "activePage",
                "admin-contacts"
        );

        // ---------------------------------------------------------------------
        // PAGINATION
        // ---------------------------------------------------------------------

        model.addAttribute(
                "currentPage",
                contactsPage.getNumber()
        );

        model.addAttribute(
                "pageSize",
                contactsPage.getSize()
        );

        model.addAttribute(
                "totalPages",
                contactsPage.getTotalPages()
        );

        model.addAttribute(
                "totalElements",
                contactsPage.getTotalElements()
        );

        model.addAttribute(
                "hasPrevious",
                contactsPage.hasPrevious()
        );

        model.addAttribute(
                "hasNext",
                contactsPage.hasNext()
        );

        model.addAttribute(
                "isFirst",
                contactsPage.isFirst()
        );

        model.addAttribute(
                "isLast",
                contactsPage.isLast()
        );

        model.addAttribute(
                "paginationEnabled",
                contactsPage.getTotalElements() > safeSize
        );

        return "admin/messages";
    }

    // =========================================================================
    // RÉPONSE ADMINISTRATEUR
    // =========================================================================

    /**
     * Traite la réponse administrateur à un message de contact.
     *
     * La boîte actuelle est conservée afin qu'une réponse effectuée depuis
     * une vue donnée n'oblige pas l'utilisateur à revenir automatiquement
     * dans une autre vue.
     */
    @PostMapping("/admin/messages/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public String respondToContact(
            @RequestParam("id")
            String id,

            @RequestParam("responseContent")
            String responseContent,

            @RequestParam(name = "box", required = false, defaultValue = "active")
            String box,

            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "from", required = false)
            String from,

            @RequestParam(name = "notifPage", required = false)
            Integer notifPage,

            @RequestParam(name = "notifSize", required = false)
            Integer notifSize,

            @RequestParam(name = "notifQ", required = false)
            String notifQ,

            @RequestParam(name = "notifSelectedNotificationId", required = false)
            Integer notifSelectedNotificationId,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            RedirectAttributes redirectAttributes
    ) {
        String trimmedResponse =
                responseContent != null
                        ? responseContent.trim()
                        : "";

        if (trimmedResponse.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "La réponse administrateur est obligatoire."
            );

            addContactNavigationAttributes(
                    redirectAttributes,
                    box,
                    q,
                    from,
                    notifPage,
                    notifSize,
                    notifQ,
                    notifSelectedNotificationId,
                    page,
                    size,
                    id
            );

            return "redirect:/admin/messages";
        }

        ContactReplyRequestDTO requestDTO =
                new ContactReplyRequestDTO();

        requestDTO.setResponseContent(
                trimmedResponse
        );

        try {
            contactService.replyToContact(
                    id,
                    requestDTO
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Réponse envoyée avec succès."
            );

        } catch (RuntimeException ex) {
            String message =
                    ex.getMessage() != null
                            && !ex.getMessage().isBlank()
                            ? ex.getMessage()
                            : "Impossible d’envoyer la réponse.";

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    message
            );
        }

        addContactNavigationAttributes(
                redirectAttributes,
                box,
                q,
                from,
                notifPage,
                notifSize,
                notifQ,
                notifSelectedNotificationId,
                page,
                size,
                id
        );

        return "redirect:/admin/messages";
    }

    // =========================================================================
    // ARCHIVAGE ADMIN
    // =========================================================================

    /**
     * Archive un message uniquement dans l'espace administratif.
     *
     * Le membre conserve exactement son propre état d'archivage.
     */
    @PostMapping("/admin/messages/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public String archiveContact(
            @RequestParam("id")
            String id,

            @RequestParam(name = "box", required = false, defaultValue = "active")
            String box,

            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "from", required = false)
            String from,

            @RequestParam(name = "notifPage", required = false)
            Integer notifPage,

            @RequestParam(name = "notifSize", required = false)
            Integer notifSize,

            @RequestParam(name = "notifQ", required = false)
            String notifQ,

            @RequestParam(name = "notifSelectedNotificationId", required = false)
            Integer notifSelectedNotificationId,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean success = false;

        try {
            Integer currentAdminId =
                    resolveCurrentAdminUserId(authentication);

            contactService.archiveContactForAdmin(
                    id,
                    currentAdminId
            );

            success = true;

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Message archivé avec succès."
            );

        } catch (RuntimeException ex) {
            String message =
                    ex.getMessage() != null
                            && !ex.getMessage().isBlank()
                            ? ex.getMessage()
                            : "Impossible d’archiver ce message.";

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    message
            );
        }

        /*
         * Après un archivage réussi, le message quitte immédiatement
         * la boîte Actifs : on ne conserve donc plus sa sélection.
         *
         * En cas d'erreur, la sélection est conservée.
         */
        String selectedContactId =
                success
                        ? null
                        : id;

        addContactNavigationAttributes(
                redirectAttributes,
                box,
                q,
                from,
                notifPage,
                notifSize,
                notifQ,
                notifSelectedNotificationId,
                page,
                size,
                selectedContactId
        );

        return "redirect:/admin/messages";
    }

    // =========================================================================
    // RESTAURATION ADMIN
    // =========================================================================

    /**
     * Restaure un message archivé côté administration.
     *
     * Le message revient dans la boîte Actifs de l'administration.
     * Aucun état côté membre n'est modifié.
     */
    @PostMapping("/admin/messages/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public String restoreContact(
            @RequestParam("id")
            String id,

            @RequestParam(name = "box", required = false, defaultValue = "archived")
            String box,

            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "from", required = false)
            String from,

            @RequestParam(name = "notifPage", required = false)
            Integer notifPage,

            @RequestParam(name = "notifSize", required = false)
            Integer notifSize,

            @RequestParam(name = "notifQ", required = false)
            String notifQ,

            @RequestParam(name = "notifSelectedNotificationId", required = false)
            Integer notifSelectedNotificationId,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean success = false;

        try {
            Integer currentAdminId =
                    resolveCurrentAdminUserId(authentication);

            contactService.restoreContactForAdmin(
                    id,
                    currentAdminId
            );

            success = true;

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Message restauré avec succès."
            );

        } catch (RuntimeException ex) {
            String message =
                    ex.getMessage() != null
                            && !ex.getMessage().isBlank()
                            ? ex.getMessage()
                            : "Impossible de restaurer ce message.";

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    message
            );
        }

        /*
         * Après restauration, le message quitte la boîte Archivés.
         *
         * On retire donc sa sélection si l'opération a réussi.
         */
        String selectedContactId =
                success
                        ? null
                        : id;

        addContactNavigationAttributes(
                redirectAttributes,
                box,
                q,
                from,
                notifPage,
                notifSize,
                notifQ,
                notifSelectedNotificationId,
                page,
                size,
                selectedContactId
        );

        return "redirect:/admin/messages";
    }

    // =========================================================================
    // AUTOCOMPLÉTION
    // =========================================================================

    /**
     * Fournit les suggestions de messages pour l'autocomplétion.
     *
     * La recherche est volontairement limitée à la boîte actuellement
     * affichée : les archives ne doivent donc pas apparaître dans les
     * suggestions de la boîte Actifs, et inversement.
     */
    @GetMapping(
            value = "/admin/messages/suggest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContactSuggestResponse>> suggestContacts(
            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "box", required = false, defaultValue = "active")
            String box,

            Authentication authentication
    ) {
        String normalizedQuery =
                normalizeSearchValue(q);

        if (normalizedQuery.isEmpty()) {
            return ResponseEntity.ok(
                    List.of()
            );
        }

        String resolvedBox =
                normalizeContactBox(box);

        Integer currentAdminId =
                resolveCurrentAdminUserId(authentication);

        List<ContactSuggestResponse> suggestions =
                loadContactsForBox(
                        resolvedBox,
                        currentAdminId
                )
                        .stream()
                        .filter(contact ->
                                matchesContactSearch(
                                        contact,
                                        normalizedQuery
                                )
                        )
                        .limit(CONTACTS_SUGGEST_LIMIT)
                        .map(this::toSuggestResponse)
                        .toList();

        return ResponseEntity.ok(
                suggestions
        );
    }

    // =========================================================================
    // CHARGEMENT DES BOÎTES
    // =========================================================================

    /**
     * Charge la liste correspondant à la boîte ADMIN demandée.
     */
    private List<ContactResponseDTO> loadContactsForBox(
            String box,
            Integer currentAdminId
    ) {
        if (CONTACT_BOX_ARCHIVED.equals(
                normalizeContactBox(box)
        )) {
            return contactService
                    .getArchivedContactsForAdmin(
                            currentAdminId
                    );
        }

        return contactService
                .getActiveContactsForAdmin(
                        currentAdminId
                );
    }

    /**
     * Normalise la valeur de la boîte.
     *
     * Toute valeur inconnue revient volontairement à la boîte Actifs.
     */
    private String normalizeContactBox(String box) {
        if (box != null
                && CONTACT_BOX_ARCHIVED.equalsIgnoreCase(
                box.trim()
        )) {
            return CONTACT_BOX_ARCHIVED;
        }

        return CONTACT_BOX_ACTIVE;
    }

    // =========================================================================
    // ADMIN CONNECTÉ
    // =========================================================================

    /**
     * Résout l'identifiant SQL de l'administrateur actuellement connecté.
     *
     * Le rôle est ensuite de nouveau vérifié dans ContactServiceImpl.
     */
    private Integer resolveCurrentAdminUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "Administrateur connecté introuvable."
            );
        }

        String adminEmail =
                authentication.getName().trim();

        User adminUser =
                userRepository
                        .findByEmailUserWithRole(adminEmail)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Administrateur connecté introuvable pour l'email : "
                                                + adminEmail
                                )
                        );

        if (adminUser.getIdUser() == null) {
            throw new IllegalStateException(
                    "Administrateur connecté invalide : identifiant introuvable."
            );
        }

        return adminUser.getIdUser();
    }

    // =========================================================================
    // REDIRECTIONS / CONSERVATION DU CONTEXTE
    // =========================================================================

    /**
     * Centralise la conservation des paramètres de navigation.
     *
     * Cette méthode évite de perdre :
     *
     * - la boîte Actifs / Archivés ;
     * - la recherche ;
     * - la pagination ;
     * - le contexte provenant éventuellement des Notifications ;
     * - la sélection courante lorsque celle-ci doit être conservée.
     */
    private void addContactNavigationAttributes(
            RedirectAttributes redirectAttributes,
            String box,
            String q,
            String from,
            Integer notifPage,
            Integer notifSize,
            String notifQ,
            Integer notifSelectedNotificationId,
            int page,
            int size,
            String selectedContactId
    ) {
        redirectAttributes.addAttribute(
                "box",
                normalizeContactBox(box)
        );

        if (selectedContactId != null
                && !selectedContactId.isBlank()) {

            redirectAttributes.addAttribute(
                    "selectedContactId",
                    selectedContactId.trim()
            );
        }

        if (q != null
                && !q.trim().isEmpty()) {

            redirectAttributes.addAttribute(
                    "q",
                    q.trim()
            );
        }

        if (from != null
                && !from.trim().isEmpty()) {

            redirectAttributes.addAttribute(
                    "from",
                    from.trim()
            );
        }

        if (notifPage != null) {
            redirectAttributes.addAttribute(
                    "notifPage",
                    Math.max(notifPage, 0)
            );
        }

        if (notifSize != null) {
            redirectAttributes.addAttribute(
                    "notifSize",
                    notifSize > 0
                            ? notifSize
                            : CONTACTS_PAGE_SIZE
            );
        }

        if (notifQ != null
                && !notifQ.trim().isEmpty()) {

            redirectAttributes.addAttribute(
                    "notifQ",
                    notifQ.trim()
            );
        }

        if (notifSelectedNotificationId != null) {
            redirectAttributes.addAttribute(
                    "notifSelectedNotificationId",
                    notifSelectedNotificationId
            );
        }

        redirectAttributes.addAttribute(
                "page",
                Math.max(page, 0)
        );

        redirectAttributes.addAttribute(
                "size",
                size > 0
                        ? size
                        : CONTACTS_PAGE_SIZE
        );
    }

    // =========================================================================
    // PAGINATION
    // =========================================================================

    /**
     * Transforme une liste de contacts en page Spring.
     *
     * La pagination reste effectuée en mémoire puisque le service Contact
     * fournit actuellement des listes issues de MongoDB.
     */
    private Page<ContactResponseDTO> toContactsPage(
            List<ContactResponseDTO> contacts,
            int page,
            int size
    ) {
        int safePage =
                Math.max(page, 0);

        int safeSize =
                size > 0
                        ? size
                        : CONTACTS_PAGE_SIZE;

        int start =
                safePage * safeSize;

        if (start >= contacts.size()) {
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(
                            safePage,
                            safeSize
                    ),
                    contacts.size()
            );
        }

        int end =
                Math.min(
                        start + safeSize,
                        contacts.size()
                );

        List<ContactResponseDTO> content =
                contacts.subList(
                        start,
                        end
                );

        return new PageImpl<>(
                content,
                PageRequest.of(
                        safePage,
                        safeSize
                ),
                contacts.size()
        );
    }

    /**
     * Détermine la page à afficher lorsqu'un message est sélectionné.
     *
     * Si le message est présent dans la liste filtrée, sa page est calculée
     * automatiquement afin qu'il reste visible.
     */
    private int resolveContactPageIndex(
            List<ContactResponseDTO> contacts,
            ContactResponseDTO selectedContact,
            int requestedPage,
            int pageSize
    ) {
        int safeRequestedPage =
                Math.max(requestedPage, 0);

        int safePageSize =
                pageSize > 0
                        ? pageSize
                        : CONTACTS_PAGE_SIZE;

        if (selectedContact != null) {
            for (int i = 0; i < contacts.size(); i++) {
                ContactResponseDTO contact =
                        contacts.get(i);

                if (Objects.equals(
                        contact.getId(),
                        selectedContact.getId()
                )) {
                    return i / safePageSize;
                }
            }
        }

        if (contacts.isEmpty()) {
            return 0;
        }

        int lastPage =
                (contacts.size() - 1)
                        / safePageSize;

        return Math.min(
                safeRequestedPage,
                lastPage
        );
    }

    // =========================================================================
    // AUTOCOMPLÉTION : DTO
    // =========================================================================

    /**
     * Convertit un contact complet en réponse réduite pour l'autocomplétion.
     */
    private ContactSuggestResponse toSuggestResponse(
            ContactResponseDTO contact
    ) {
        String date =
                contact.getDate() != null
                        ? contact.getDate().format(
                        CONTACT_DATE_DISPLAY_FORMATTER
                )
                        : null;

        return new ContactSuggestResponse(
                contact.getId(),
                contact.getIdUser(),
                contact.getSubject(),
                contact.getEmail(),
                contact.getStatusLabel(),
                contact.isResponseSent(),
                date
        );
    }

    // =========================================================================
    // RECHERCHE
    // =========================================================================

    private boolean matchesContactSearch(
            ContactResponseDTO contact,
            String query
    ) {
        String normalizedQuery =
                normalizeSearchValue(query);

        String haystack =
                buildContactSearchHaystack(contact);

        return !normalizedQuery.isEmpty()
                && haystack.contains(
                normalizedQuery
        );
    }

    /**
     * Construit la chaîne de recherche d'un message de contact.
     *
     * Elle regroupe :
     *
     * - identité ;
     * - email ;
     * - sujet ;
     * - contenu ;
     * - statut ;
     * - rôle ;
     * - réponse ;
     * - administrateur ayant répondu ;
     * - dates ;
     * - état d'archivage administratif.
     */
    private String buildContactSearchHaystack(
            ContactResponseDTO contact
    ) {
        LocalDateTime dateContact =
                contact.getDate();

        LocalDateTime archivedAt =
                contact.getArchivedAtByAdmin();

        return normalizeSearchValue(
                String.join(
                        " ",
                        safeValue(contact.getId()),
                        safeValue(contact.getIdUser()),
                        safeValue(contact.getName()),
                        safeValue(contact.getEmail()),
                        safeValue(contact.getSubject()),
                        safeValue(contact.getContent()),
                        safeValue(contact.getOrigin()),
                        safeValue(contact.getStatus()),
                        safeValue(contact.getStatusLabel()),
                        safeValue(contact.getSenderRoleLabel()),
                        safeValue(contact.getResponseContent()),
                        safeValue(contact.getAnsweredByUserId()),
                        safeValue(contact.getAnsweredByAdminLabel()),

                        dateContact != null
                                ? dateContact.format(
                                CONTACT_DATE_DISPLAY_FORMATTER
                        )
                                : "",

                        dateContact != null
                                ? dateContact
                                .toLocalDate()
                                .toString()
                                : "",

                        archivedAt != null
                                ? archivedAt.format(
                                CONTACT_DATE_DISPLAY_FORMATTER
                        )
                                : "",

                        archivedAt != null
                                ? archivedAt
                                .toLocalDate()
                                .toString()
                                : "",

                        contact.isResponseSent()
                                ? "repondu répondu answered traite traité"
                                : "nouveau new en attente non repondu non répondu",

                        contact.isArchivedByAdmin()
                                ? "archive archivé archived"
                                : "actif active"
                )
        );
    }

    /**
     * Normalise une valeur textuelle pour la recherche.
     *
     * La casse et les principaux caractères accentués sont uniformisés afin
     * de conserver une recherche tolérante.
     */
    private String normalizeSearchValue(
            String value
    ) {
        if (value == null) {
            return "";
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        normalized = normalized
                .replace('à', 'a')
                .replace('â', 'a')
                .replace('ä', 'a')
                .replace('ç', 'c')
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('ë', 'e')
                .replace('î', 'i')
                .replace('ï', 'i')
                .replace('ô', 'o')
                .replace('ö', 'o')
                .replace('ù', 'u')
                .replace('û', 'u')
                .replace('ü', 'u');

        return normalized;
    }

    private String safeValue(
            Object value
    ) {
        return value == null
                ? ""
                : String.valueOf(value);
    }

    // =========================================================================
    // DTO INTERNE AUTOCOMPLÉTION
    // =========================================================================

    /**
     * DTO interne utilisé uniquement pour exposer les suggestions
     * de messages de contact au format JSON.
     */
    public static final class ContactSuggestResponse {

        private String id;
        private Integer idUser;
        private String subject;
        private String email;
        private String status;
        private Boolean responseSent;
        private String date;

        public ContactSuggestResponse() {
        }

        public ContactSuggestResponse(
                String id,
                Integer idUser,
                String subject,
                String email,
                String status,
                Boolean responseSent,
                String date
        ) {
            this.id = id;
            this.idUser = idUser;
            this.subject = subject;
            this.email = email;
            this.status = status;
            this.responseSent = responseSent;
            this.date = date;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getIdUser() {
            return idUser;
        }

        public void setIdUser(Integer idUser) {
            this.idUser = idUser;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getResponseSent() {
            return responseSent;
        }

        public void setResponseSent(Boolean responseSent) {
            this.responseSent = responseSent;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}