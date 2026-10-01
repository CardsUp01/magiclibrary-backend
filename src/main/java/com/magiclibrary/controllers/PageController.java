package com.magiclibrary.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.mongo.dto.ContactRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.services.ContactService;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.LoanLineService;
import com.magiclibrary.services.LoanService;
import com.magiclibrary.services.NotificationService;

/**
 * Contrôleur SSR principal de l'application.
 *
 * Cette classe centralise les pages publiques, les pages membres,
 * les messages de contact, les emprunts personnels, les notifications
 * et les fiches détaillées accessibles depuis l'interface Thymeleaf.
 */
@Controller
public class PageController {

    /*
     * Paramètres de pagination et de suggestion utilisés par les pages membres.
     */
    private static final int LOANS_PAGE_SIZE = 9;
    private static final int NOTIFICATIONS_PAGE_SIZE = 9;
    private static final int NOTIFICATIONS_FETCH_BATCH_SIZE = 200;
    private static final int NOTIFICATIONS_SUGGEST_LIMIT = 8;
    private static final int CONTACT_MESSAGES_PAGE_SIZE = 9;
    private static final int CONTACT_MESSAGES_SUGGEST_LIMIT = 8;

    /*
     * Propriétés cumulatives contrôlant l'affichage de l'action manuelle DEMO.
     */
    private static final String DEMO_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.enabled";

    private static final String DEMO_MANUAL_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.manual-enabled";

    /*
     * Formats d'affichage des dates utilisés dans les pages SSR
     * et les réponses d'autocomplétion.
     */
    private static final DateTimeFormatter NOTIFICATION_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter LOAN_DATE_TIME_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter LOAN_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter CONTACT_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final ContactService contactService;
    private final LoanService loanService;
    private final LoanLineService loanLineService;
    private final NotificationService notificationService;
    private final Environment environment;

    public PageController(
            UserRepository userRepository,
            ContactService contactService,
            LoanService loanService,
            LoanLineService loanLineService,
            NotificationService notificationService,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.contactService = contactService;
        this.loanService = loanService;
        this.loanLineService = loanLineService;
        this.notificationService = notificationService;
        this.environment = environment;
    }

    /*
     * Affiche la page de connexion ou redirige l'utilisateur déjà authentifié
     * vers l'accueil de l'application.
     */
    @GetMapping({"/", "/login"})
    public String loginPage(
            Authentication authentication,
            HttpServletResponse response
    ) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/accueil";
        }

        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader(
                "Pragma",
                "no-cache"
        );
        response.setDateHeader(
                "Expires",
                0
        );

        return "login";
    }

    /*
     * Affiche l'accueil et expose l'action de reset uniquement lorsque les trois
     * protections cumulatives sont satisfaites : profil DEMO, reset global actif
     * et reset manuel explicitement autorisé.
     */
    @GetMapping("/accueil")
    public String accueilPage(Model model) {
        model.addAttribute(
                "activePage",
                "accueil"
        );

        model.addAttribute(
                "demoManualResetEnabled",
                isDemoManualResetEnabled()
        );

        return "accueil";
    }

    @GetMapping("/mentions-legales")
    public String legalNoticePage(Model model) {
        model.addAttribute(
                "activePage",
                "mentions-legales"
        );

        return "mentions-legales";
    }

    @GetMapping("/confidentialite")
    public String confidentialitePage(Model model) {
        model.addAttribute(
                "activePage",
                "confidentialite"
        );

        return "confidentialite";
    }

    @GetMapping("/accessibilite")
    public String accessibilitePage(Model model) {
        model.addAttribute(
                "activePage",
                "accessibilite"
        );

        model.addAttribute(
                "pageTitle",
                "Accessibilité"
        );

        return "accessibilite";
    }

    @GetMapping("/cgu")
    public String cguPage(Model model) {
        model.addAttribute(
                "activePage",
                "cgu"
        );

        model.addAttribute(
                "pageTitle",
                "Conditions générales d’utilisation (CGU)"
        );

        return "cgu";
    }

    /*
     * Affiche le formulaire de contact et préremplit l'email
     * lorsque l'utilisateur est authentifié.
     *
     * Les administrateurs sont redirigés vers la gestion des messages,
     * car le formulaire de contact est destiné aux membres qui souhaitent
     * contacter l'équipe d'administration.
     */
    @GetMapping("/contact")
    public String contactPage(
            Authentication authentication,
            Model model
    ) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/messages";
        }

        model.addAttribute(
                "activePage",
                "contact"
        );

        String email =
                authentication != null
                        ? authentication.getName()
                        : null;

        model.addAttribute(
                "prefillEmail",
                email
        );

        return "contact";
    }

    /*
     * Traite l'envoi d'un message de contact par un utilisateur connecté.
     * Le message est stocké dans le module CONTACT MongoDB.
     *
     * Les administrateurs ne peuvent pas soumettre ce formulaire,
     * même en cas d'appel direct à l'URL de traitement.
     */
    @PostMapping("/contact")
    public String submitContactForm(
            @RequestParam(name = "name", required = false)
            String name,

            @RequestParam(name = "email", required = false)
            String email,

            @RequestParam(name = "subject", required = false)
            String subject,

            @RequestParam(name = "message", required = false)
            String message,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/messages";
        }

        String authEmail =
                authentication.getName();

        User user =
                userRepository.findByEmailUser(authEmail)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        if (subject == null
                || subject.trim().isEmpty()
                || message == null
                || message.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Sujet et message sont obligatoires."
            );

            return "redirect:/contact";
        }

        ContactRequestDTO requestDTO =
                new ContactRequestDTO();

        requestDTO.setIdUser(
                user.getIdUser()
        );

        requestDTO.setName(
                name
        );

        requestDTO.setEmail(
                authEmail
        );

        requestDTO.setSubject(
                subject
        );

        requestDTO.setMessage(
                message
        );

        contactService.createContact(
                requestDTO
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Message envoyé. Nous te répondrons dès que possible."
        );

        return "redirect:/contact";
    }

    // =========================================================================
    // MES MESSAGES DE CONTACT
    // =========================================================================

    /*
     * Affiche les messages de contact du membre connecté.
     *
     * Deux boîtes sont disponibles :
     *
     * - active :
     *      messages non archivés par le membre ;
     *
     * - archived :
     *      messages archivés par le membre.
     *
     * L'état d'archivage administratif est volontairement ignoré ici :
     * le membre possède son propre classement indépendant.
     *
     * Cas particulier :
     *
     * Lorsqu'un lien direct fournit selectedContactId sans fournir box
     * (notamment depuis une notification), le contrôleur détermine
     * automatiquement si le message appartient actuellement à Actifs
     * ou Archivés.
     *
     * Le lien reste ainsi valide même si le membre archive ou restaure
     * le message après la création de la notification.
     */
    @GetMapping("/mes-messages-de-contact")
    public String myContactMessagesPage(
            @RequestParam(name = "box", required = false)
            String box,

            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "selectedContactId", required = false)
            String selectedContactId,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            Authentication authentication,
            Model model
    ) {
        String email =
                authentication.getName();

        User user =
                userRepository.findByEmailUser(email)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        int safePage =
                Math.max(
                        page,
                        0
                );

        int safeSize =
                size > 0
                        ? size
                        : CONTACT_MESSAGES_PAGE_SIZE;

        String resolvedQuery =
                q == null
                        ? ""
                        : q.trim();

        /*
         * Détection intelligente de la boîte lorsqu'un lien direct cible
         * un message sans préciser explicitement Actifs ou Archivés.
         *
         * Exemple :
         *
         * /mes-messages-de-contact?selectedContactId=...
         *
         * C'est notamment le format utilisé par les notifications Contact.
         *
         * IMPORTANT :
         *
         * - si box est explicitement présent dans l'URL, il reste prioritaire ;
         * - le message recherché doit appartenir au membre connecté ;
         * - aucun document tiers n'est utilisé pour déterminer la boîte ;
         * - si le message est introuvable, Actifs reste la valeur défensive.
         */
        MemberContactMessageBox resolvedBox;

        boolean explicitBoxProvided =
                box != null
                        && !box.isBlank();

        if (!explicitBoxProvided
                && selectedContactId != null
                && !selectedContactId.isBlank()) {

            ContactResponseDTO directlySelectedContact =
                    contactService.getContactByIdForUser(
                            selectedContactId.trim(),
                            user.getIdUser()
                    );

            if (directlySelectedContact != null
                    && directlySelectedContact.isArchivedByMember()) {

                resolvedBox =
                        MemberContactMessageBox.ARCHIVED;

            } else {

                resolvedBox =
                        MemberContactMessageBox.ACTIVE;
            }

        } else {

            resolvedBox =
                    MemberContactMessageBox.fromRequestValue(
                            box
                    );
        }

        /*
         * On charge uniquement les messages de la boîte actuellement affichée.
         *
         * Le service applique déjà le cloisonnement par idUser.
         */
        List<ContactResponseDTO> memberContacts =
                loadMyContactMessagesForBox(
                        resolvedBox,
                        user.getIdUser()
                );

        /*
         * La recherche reste limitée à la boîte active.
         */
        List<ContactResponseDTO> filteredContacts =
                resolvedQuery.isEmpty()
                        ? memberContacts
                        : memberContacts.stream()
                        .filter(contact ->
                                matchesMyContactMessageSearch(
                                        contact,
                                        resolvedQuery
                                )
                        )
                        .toList();

        ContactResponseDTO selectedContact =
                null;

        /*
         * La sélection est volontairement recherchée directement dans
         * filteredContacts.
         *
         * Ainsi :
         *
         * - impossible de sélectionner le message d'un autre membre ;
         * - impossible d'afficher un message archivé depuis la boîte Actifs ;
         * - impossible d'afficher un message actif depuis la boîte Archivés ;
         * - impossible d'afficher un résultat ne correspondant pas à la
         *   recherche active.
         */
        if (selectedContactId != null
                && !selectedContactId.isBlank()) {

            String normalizedSelectedContactId =
                    selectedContactId.trim();

            selectedContact =
                    filteredContacts.stream()
                            .filter(contact ->
                                    Objects.equals(
                                            contact.getId(),
                                            normalizedSelectedContactId
                                    )
                            )
                            .findFirst()
                            .orElse(null);
        }

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

        /*
         * Lorsque rien n'est explicitement sélectionné, le premier message
         * visible de la page devient le détail courant.
         *
         * Ce comportement historique est conservé.
         */
        if (selectedContact == null
                && !contactsPage.getContent().isEmpty()) {

            selectedContact =
                    contactsPage.getContent().get(0);
        }

        String resolvedSelectedContactId =
                selectedContact != null
                        ? selectedContact.getId()
                        : null;

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

        model.addAttribute(
                "q",
                resolvedQuery
        );

        /*
         * État Actifs / Archivés utilisé par le template.
         */
        model.addAttribute(
                "box",
                resolvedBox.getRequestValue()
        );

        model.addAttribute(
                "boxLabel",
                resolvedBox.getLabel()
        );

        model.addAttribute(
                "isActiveBox",
                resolvedBox == MemberContactMessageBox.ACTIVE
        );

        model.addAttribute(
                "isArchivedBox",
                resolvedBox == MemberContactMessageBox.ARCHIVED
        );

        model.addAttribute(
                "pageTitle",
                "Mes messages de contact"
        );

        model.addAttribute(
                "activePage",
                "mes-messages-de-contact"
        );

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

        return "mes-messages-de-contact";
    }

    /*
     * Fournit les suggestions pour l'autocomplétion des messages
     * de contact du membre connecté.
     *
     * Les suggestions restent strictement limitées à la boîte courante.
     */
    @GetMapping(
            value = "/mes-messages-de-contact/suggest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<List<MyContactMessageSuggestResponse>>
    suggestMyContactMessages(
            @RequestParam(name = "box", required = false, defaultValue = "active")
            String box,

            @RequestParam(name = "q", required = false)
            String q,

            Authentication authentication
    ) {
        String email =
                authentication.getName();

        User user =
                userRepository.findByEmailUser(email)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        String normalizedQuery =
                normalizeSearchValue(
                        q
                );

        if (normalizedQuery.isEmpty()) {
            return ResponseEntity.ok(
                    List.of()
            );
        }

        MemberContactMessageBox resolvedBox =
                MemberContactMessageBox.fromRequestValue(
                        box
                );

        List<MyContactMessageSuggestResponse> suggestions =
                loadMyContactMessagesForBox(
                        resolvedBox,
                        user.getIdUser()
                )
                        .stream()
                        .filter(contact ->
                                matchesMyContactMessageSearch(
                                        contact,
                                        normalizedQuery
                                )
                        )
                        .limit(
                                CONTACT_MESSAGES_SUGGEST_LIMIT
                        )
                        .map(
                                this::toMyContactMessageSuggestResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                suggestions
        );
    }

    /*
     * Archive logiquement un message dans l'espace du membre connecté.
     *
     * Le service vérifie obligatoirement que le document appartient
     * au membre demandeur.
     *
     * Aucune donnée n'est supprimée.
     */
    @PostMapping("/mes-messages-de-contact/{id}/archive")
    public String archiveMyContactMessage(
            @PathVariable("id")
            String idContact,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            @RequestParam(name = "q", required = false)
            String q,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String email =
                authentication.getName();

        User user =
                userRepository.findByEmailUser(email)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        contactService.archiveContactForUser(
                idContact,
                user.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Message archivé."
        );

        /*
         * Après archivage, on reste dans Actifs.
         *
         * Le message concerné n'y apparaîtra simplement plus.
         */
        addMyContactMessageNavigationAttributes(
                redirectAttributes,
                MemberContactMessageBox.ACTIVE,
                page,
                size,
                q
        );

        return "redirect:/mes-messages-de-contact";
    }

    /*
     * Restaure un message archivé par le membre.
     *
     * Le message quitte Archivés et retourne dans Actifs.
     */
    @PostMapping("/mes-messages-de-contact/{id}/restore")
    public String restoreMyContactMessage(
            @PathVariable("id")
            String idContact,

            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,

            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,

            @RequestParam(name = "q", required = false)
            String q,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String email =
                authentication.getName();

        User user =
                userRepository.findByEmailUser(email)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        contactService.restoreContactForUser(
                idContact,
                user.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Message restauré dans les messages actifs."
        );

        /*
         * Après restauration, on reste dans Archivés.
         *
         * Le message concerné n'y apparaîtra simplement plus.
         */
        addMyContactMessageNavigationAttributes(
                redirectAttributes,
                MemberContactMessageBox.ARCHIVED,
                page,
                size,
                q
        );

        return "redirect:/mes-messages-de-contact";
    }

    /*
     * Charge les messages correspondant à la boîte Contact active
     * du membre connecté.
     */
    private List<ContactResponseDTO> loadMyContactMessagesForBox(
            MemberContactMessageBox box,
            Integer idUser
    ) {
        return switch (box) {

            case ACTIVE ->
                    contactService.getActiveContactsForUser(
                            idUser
                    );

            case ARCHIVED ->
                    contactService.getArchivedContactsForUser(
                            idUser
                    );
        };
    }

    /*
     * Préserve la boîte, la pagination et la recherche après une action POST
     * sur un message de contact du membre.
     */
    private void addMyContactMessageNavigationAttributes(
            RedirectAttributes redirectAttributes,
            MemberContactMessageBox box,
            int page,
            int size,
            String q
    ) {
        redirectAttributes.addAttribute(
                "box",
                box.getRequestValue()
        );

        redirectAttributes.addAttribute(
                "page",
                Math.max(
                        page,
                        0
                )
        );

        redirectAttributes.addAttribute(
                "size",
                size > 0
                        ? size
                        : CONTACT_MESSAGES_PAGE_SIZE
        );

        if (q != null
                && !q.trim().isEmpty()) {

            redirectAttributes.addAttribute(
                    "q",
                    q.trim()
            );
        }
    }

    /*
     * Boîtes disponibles dans l'espace personnel des messages de contact.
     *
     * Elles représentent exclusivement l'état d'archivage choisi
     * par le membre.
     */
    private enum MemberContactMessageBox {

        ACTIVE(
                "active",
                "Actifs"
        ),

        ARCHIVED(
                "archived",
                "Archivés"
        );

        private final String requestValue;
        private final String label;

        MemberContactMessageBox(
                String requestValue,
                String label
        ) {
            this.requestValue = requestValue;
            this.label = label;
        }

        public String getRequestValue() {
            return requestValue;
        }

        public String getLabel() {
            return label;
        }

        /*
         * Toute valeur absente ou inconnue revient défensivement vers Actifs.
         */
        public static MemberContactMessageBox fromRequestValue(
                String value
        ) {
            if (value == null
                    || value.isBlank()) {

                return ACTIVE;
            }

            String normalized =
                    value.trim();

            for (MemberContactMessageBox box
                    : MemberContactMessageBox.values()) {

                if (box.requestValue.equalsIgnoreCase(
                        normalized
                )) {
                    return box;
                }
            }

            return ACTIVE;
        }
    }

    /*
     * Affiche les emprunts du membre connecté.
     *
     * La méthode gère la recherche, le tri, la pagination
     * et le résumé des objets associés à chaque emprunt.
     */
    @GetMapping("/mes-emprunts")
    public String myLoansPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedLoanId", required = false) Integer selectedLoanId,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Authentication authentication,
            Model model
    ) {
        String email = authentication.getName();

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : LOANS_PAGE_SIZE;
        String resolvedSort = sort == null || sort.trim().isEmpty() ? "recent" : sort.trim();
        String resolvedQuery = q == null ? "" : q.trim();

        Page<LoanResponseDTO> loansPage;

        if (resolvedQuery.isEmpty()) {
            loansPage = loanService.getLoansForUserPagedAndSorted(
                    email,
                    resolvedSort,
                    safePage,
                    safeSize
            );
        } else {
            List<LoanResponseDTO> filteredLoans = new ArrayList<>(loanService.getLoansForUser(email));

            filteredLoans.removeIf(loan -> !matchesMyLoanSearch(loan, resolvedQuery));
            sortMyLoans(filteredLoans, resolvedSort);

            loansPage = toPage(filteredLoans, safePage, safeSize);
        }

        List<LoanResponseDTO> loans = loansPage.getContent();

        Map<Integer, Integer> loanItemCounts = new LinkedHashMap<>();
        Map<Integer, String> loanItemSummaries = new LinkedHashMap<>();

        for (LoanResponseDTO loan : loans) {
            Integer idLoan = loan.getIdLoan();
            List<LoanLineResponseDTO> lines = loanLineService.getLoanLinesByLoanId(idLoan);

            int totalItems = 0;
            String firstTitle = null;

            for (LoanLineResponseDTO line : lines) {
                Integer quantity = line.getQuantityLoanLine();
                totalItems += quantity != null && quantity > 0 ? quantity : 0;

                if (firstTitle == null) {
                    String title = line.getTitleItem();
                    if (title != null && !title.trim().isEmpty()) {
                        firstTitle = title.trim();
                    }
                }
            }

            loanItemCounts.put(idLoan, totalItems);
            loanItemSummaries.put(idLoan, buildLoanItemSummary(firstTitle, totalItems));
        }

        boolean paginationEnabled = loansPage.getTotalElements() > safeSize;

        model.addAttribute("loans", loans);
        model.addAttribute("loanItemCounts", loanItemCounts);
        model.addAttribute("loanItemSummaries", loanItemSummaries);
        model.addAttribute("q", resolvedQuery);
        model.addAttribute("selectedLoanId", selectedLoanId);
        model.addAttribute("sort", resolvedSort);
        model.addAttribute("pageTitle", "Mes emprunts");
        model.addAttribute("activePage", "mes-emprunts");

        model.addAttribute("currentPage", loansPage.getNumber());
        model.addAttribute("pageSize", loansPage.getSize());
        model.addAttribute("totalPages", loansPage.getTotalPages());
        model.addAttribute("totalElements", loansPage.getTotalElements());
        model.addAttribute("hasPrevious", loansPage.hasPrevious());
        model.addAttribute("hasNext", loansPage.hasNext());
        model.addAttribute("isFirst", loansPage.isFirst());
        model.addAttribute("isLast", loansPage.isLast());
        model.addAttribute("paginationEnabled", paginationEnabled);

        return "mes-emprunts";
    }

    // =========================================================================
    // MES NOTIFICATIONS
    // =========================================================================

    /*
     * Affiche les notifications du membre connecté.
     *
     * Deux boîtes sont disponibles côté membre :
     *
     * - received : notifications reçues et non archivées ;
     * - archived : notifications archivées par le membre.
     *
     * Contrairement à l'administration, aucun historique "Envoyées"
     * n'est exposé ici car les membres ne créent pas de notifications.
     */
    @GetMapping("/mes-notifications")
    public String myNotificationsPage(
            @RequestParam(name = "box", required = false, defaultValue = "received") String box,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Authentication authentication,
            Model model
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : NOTIFICATIONS_PAGE_SIZE;
        String resolvedQuery = q == null ? "" : q.trim();

        MemberNotificationBox resolvedBox =
                MemberNotificationBox.fromRequestValue(box);

        Page<NotificationResponseDTO> notificationsPage;

        /*
         * Sans recherche, la pagination est directement déléguée à la couche
         * métier et donc à la base de données.
         */
        if (resolvedQuery.isEmpty()) {
            notificationsPage = loadMyNotificationsPage(
                    resolvedBox,
                    user.getIdUser(),
                    safePage,
                    safeSize
            );
        } else {
            /*
             * Pour conserver la recherche multicritère actuelle, toutes les
             * notifications de la boîte active sont chargées puis filtrées.
             *
             * Le cloisonnement reste strict : une recherche dans Reçues ne
             * consulte jamais les archives, et inversement.
             */
            List<NotificationResponseDTO> filteredNotifications =
                    fetchMyNotificationsForBox(
                            resolvedBox,
                            user.getIdUser()
                    ).stream()
                            .filter(notification ->
                                    matchesNotificationSuggestion(
                                            notification,
                                            normalizeSearchValue(resolvedQuery)
                                    )
                            )
                            .toList();

            notificationsPage = toNotificationPage(
                    filteredNotifications,
                    safePage,
                    safeSize
            );
        }

        List<NotificationResponseDTO> notifications =
                notificationsPage.getContent();

        boolean paginationEnabled =
                notificationsPage.getTotalElements() > safeSize;

        model.addAttribute("notifications", notifications);
        model.addAttribute("q", resolvedQuery);

        /*
         * État de navigation utilisé par mes-notifications.html.
         */
        model.addAttribute("box", resolvedBox.getRequestValue());
        model.addAttribute("boxLabel", resolvedBox.getLabel());
        model.addAttribute(
                "isReceivedBox",
                resolvedBox == MemberNotificationBox.RECEIVED
        );
        model.addAttribute(
                "isArchivedBox",
                resolvedBox == MemberNotificationBox.ARCHIVED
        );

        model.addAttribute("pageTitle", "Mes notifications");
        model.addAttribute("activePage", "mes-notifications");

        model.addAttribute("currentPage", notificationsPage.getNumber());
        model.addAttribute("pageSize", notificationsPage.getSize());
        model.addAttribute("totalPages", notificationsPage.getTotalPages());
        model.addAttribute("totalElements", notificationsPage.getTotalElements());
        model.addAttribute("hasPrevious", notificationsPage.hasPrevious());
        model.addAttribute("hasNext", notificationsPage.hasNext());
        model.addAttribute("isFirst", notificationsPage.isFirst());
        model.addAttribute("isLast", notificationsPage.isLast());
        model.addAttribute("paginationEnabled", paginationEnabled);

        return "mes-notifications";
    }

    /*
     * Fournit les suggestions pour l'autocomplétion des notifications
     * du membre connecté.
     *
     * Les suggestions sont limitées à la boîte active.
     */
    @GetMapping(
            value = "/mes-notifications/suggest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<List<MyNotificationSuggestResponse>> suggestMyNotifications(
            @RequestParam(name = "box", required = false, defaultValue = "received") String box,
            @RequestParam(name = "q", required = false) String q,
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        String normalizedQuery = normalizeSearchValue(q);

        if (normalizedQuery.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        MemberNotificationBox resolvedBox =
                MemberNotificationBox.fromRequestValue(box);

        List<MyNotificationSuggestResponse> suggestions =
                fetchMyNotificationsForBox(
                        resolvedBox,
                        user.getIdUser()
                ).stream()
                        .filter(notification ->
                                matchesNotificationSuggestion(
                                        notification,
                                        normalizedQuery
                                )
                        )
                        .limit(NOTIFICATIONS_SUGGEST_LIMIT)
                        .map(this::toMyNotificationSuggestResponse)
                        .toList();

        return ResponseEntity.ok(suggestions);
    }

    /*
     * Marque une notification comme lue depuis la page membre.
     *
     * Cette action reste possible depuis Reçues comme depuis Archivées :
     * l'archivage est indépendant du statut de lecture.
     */
    @PostMapping("/mes-notifications/{id}/read")
    public String markNotificationAsReadFromPage(
            @PathVariable("id") Integer idNotification,
            @RequestParam(name = "box", required = false, defaultValue = "received") String box,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            @RequestParam(name = "q", required = false) String q,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        MemberNotificationBox resolvedBox =
                MemberNotificationBox.fromRequestValue(box);

        notificationService.markAsRead(
                idNotification,
                user.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Notification marquée comme lue."
        );

        addMyNotificationNavigationAttributes(
                redirectAttributes,
                resolvedBox,
                page,
                size,
                q
        );

        return "redirect:/mes-notifications";
    }

    /*
     * Archive logiquement une notification reçue par le membre.
     *
     * Aucune donnée n'est supprimée :
     * la notification quitte simplement Reçues et devient visible
     * dans Archivées.
     */
    @PostMapping("/mes-notifications/{id}/archive")
    public String archiveMyNotification(
            @PathVariable("id") Integer idNotification,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            @RequestParam(name = "q", required = false) String q,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        notificationService.archiveNotification(
                idNotification,
                user.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Notification archivée."
        );

        /*
         * Après archivage, on reste dans Reçues.
         * La notification concernée n'y apparaîtra simplement plus.
         */
        addMyNotificationNavigationAttributes(
                redirectAttributes,
                MemberNotificationBox.RECEIVED,
                page,
                size,
                q
        );

        return "redirect:/mes-notifications";
    }

    /*
     * Restaure une notification archivée par le membre.
     *
     * La notification quitte Archivées et retourne dans Reçues.
     */
    @PostMapping("/mes-notifications/{id}/restore")
    public String restoreMyNotification(
            @PathVariable("id") Integer idNotification,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            @RequestParam(name = "q", required = false) String q,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        notificationService.restoreNotification(
                idNotification,
                user.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Notification restaurée dans les notifications reçues."
        );

        /*
         * Après restauration, on reste dans Archivées.
         * La notification concernée n'y apparaîtra simplement plus.
         */
        addMyNotificationNavigationAttributes(
                redirectAttributes,
                MemberNotificationBox.ARCHIVED,
                page,
                size,
                q
        );

        return "redirect:/mes-notifications";
    }

    /*
     * Charge une page de notifications correspondant à la boîte membre active.
     */
    private Page<NotificationResponseDTO> loadMyNotificationsPage(
            MemberNotificationBox box,
            Integer idUser,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0
                ? size
                : NOTIFICATIONS_PAGE_SIZE;

        return switch (box) {
            case RECEIVED ->
                    notificationService.getReceivedNotificationsForUserPaged(
                            idUser,
                            safePage,
                            safeSize
                    );

            case ARCHIVED ->
                    notificationService.getArchivedNotificationsForUserPaged(
                            idUser,
                            safePage,
                            safeSize
                    );
        };
    }

    /*
     * Charge l'ensemble des notifications accessibles dans la boîte membre
     * active.
     *
     * Cette méthode est utilisée uniquement par la recherche et les
     * suggestions.
     */
    private List<NotificationResponseDTO> fetchMyNotificationsForBox(
            MemberNotificationBox box,
            Integer idUser
    ) {
        List<NotificationResponseDTO> notifications =
                new ArrayList<>();

        int page = 0;
        Page<NotificationResponseDTO> notificationsPage;

        do {
            notificationsPage = loadMyNotificationsPage(
                    box,
                    idUser,
                    page,
                    NOTIFICATIONS_FETCH_BATCH_SIZE
            );

            notifications.addAll(
                    notificationsPage.getContent()
            );

            page++;

        } while (notificationsPage.hasNext());

        return notifications;
    }

    /*
     * Transforme une liste filtrée de notifications en page Spring.
     */
    private Page<NotificationResponseDTO> toNotificationPage(
            List<NotificationResponseDTO> notifications,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0
                ? size
                : NOTIFICATIONS_PAGE_SIZE;

        int start = safePage * safeSize;

        if (start >= notifications.size()) {
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(safePage, safeSize),
                    notifications.size()
            );
        }

        int end = Math.min(
                start + safeSize,
                notifications.size()
        );

        List<NotificationResponseDTO> content =
                notifications.subList(
                        start,
                        end
                );

        return new PageImpl<>(
                content,
                PageRequest.of(safePage, safeSize),
                notifications.size()
        );
    }

    /*
     * Préserve l'état de navigation de la page Mes notifications après
     * une action POST.
     */
    private void addMyNotificationNavigationAttributes(
            RedirectAttributes redirectAttributes,
            MemberNotificationBox box,
            int page,
            int size,
            String q
    ) {
        redirectAttributes.addAttribute(
                "box",
                box.getRequestValue()
        );

        redirectAttributes.addAttribute(
                "page",
                Math.max(page, 0)
        );

        redirectAttributes.addAttribute(
                "size",
                size > 0
                        ? size
                        : NOTIFICATIONS_PAGE_SIZE
        );

        if (q != null
                && !q.trim().isEmpty()) {

            redirectAttributes.addAttribute(
                    "q",
                    q.trim()
            );
        }
    }

    /*
     * Boîtes accessibles depuis l'espace notifications du membre.
     *
     * Aucun état SENT n'existe ici volontairement :
     * les membres sont destinataires des notifications mais n'envoient
     * pas de notifications administratives.
     */
    private enum MemberNotificationBox {

        RECEIVED(
                "received",
                "Reçues"
        ),

        ARCHIVED(
                "archived",
                "Archivées"
        );

        private final String requestValue;
        private final String label;

        MemberNotificationBox(
                String requestValue,
                String label
        ) {
            this.requestValue = requestValue;
            this.label = label;
        }

        public String getRequestValue() {
            return requestValue;
        }

        public String getLabel() {
            return label;
        }

        /*
         * Résout la valeur reçue depuis l'URL.
         *
         * Toute valeur inconnue revient défensivement vers Reçues.
         */
        public static MemberNotificationBox fromRequestValue(
                String value
        ) {
            if (value == null
                    || value.isBlank()) {

                return RECEIVED;
            }

            String normalized =
                    value.trim();

            for (MemberNotificationBox box : MemberNotificationBox.values()) {
                if (box.requestValue.equalsIgnoreCase(normalized)) {
                    return box;
                }
            }

            return RECEIVED;
        }
    }

    /*
     * Fournit les suggestions pour l'autocomplétion des emprunts
     * du membre connecté.
     */
    @GetMapping(value = "/mes-emprunts/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<MyLoanSuggestResponse>> suggestMyLoans(
            @RequestParam(name = "q", required = false) String q,
            Authentication authentication
    ) {
        String email = authentication.getName();

        List<MyLoanSuggestResponse> suggestions = loanService.suggestLoansForUser(email, q).stream()
                .map(this::toMyLoanSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    /*
     * Affiche la fiche détaillée d'un emprunt accessible au membre.
     */
    @GetMapping("/emprunts/{id}")
    public String myLoanDetailPage(
            @PathVariable("id") Integer idLoan,
            Authentication authentication,
            Model model
    ) {
        String email = authentication.getName();
        boolean isAdmin = isAdmin(authentication);

        LoanResponseDTO loan = loanService.getLoanByIdForUser(idLoan, email, isAdmin);
        List<LoanLineResponseDTO> loanLines = loanLineService.getLoanLinesByLoanId(idLoan);

        model.addAttribute("loan", loan);
        model.addAttribute("loanLines", loanLines);
        model.addAttribute("pageTitle", "Fiche emprunt");
        model.addAttribute("activePage", "mes-emprunts");
        model.addAttribute("loanDetailContext", "member");

        return "admin/fiche-emprunt";
    }

    private MyLoanSuggestResponse toMyLoanSuggestResponse(LoanResponseDTO loan) {
        return new MyLoanSuggestResponse(
                loan.getIdLoan(),
                loan.getStatusLoanLabel(),
                loan.getOriginLoanLabel()
        );
    }

    private MyNotificationSuggestResponse toMyNotificationSuggestResponse(
            NotificationResponseDTO notification
    ) {
        String dateLabel =
                notification.getDateNotification() != null
                        ? notification
                        .getDateNotification()
                        .format(
                                NOTIFICATION_DATE_DISPLAY_FORMATTER
                        )
                        : null;

        String readLabel =
                Boolean.TRUE.equals(
                        notification.getReadNotification()
                )
                        ? "Lue"
                        : "Non lue";

        return new MyNotificationSuggestResponse(
                notification.getIdNotification(),
                notification.getTitleNotification(),
                notification.getCategoryNotification() != null
                        ? notification.getCategoryNotification().name()
                        : null,
                notification.getTypeNotification() != null
                        ? notification.getTypeNotification().name()
                        : null,
                notification.getPriorityNotification(),
                readLabel,
                dateLabel
        );
    }

    private MyContactMessageSuggestResponse toMyContactMessageSuggestResponse(
            ContactResponseDTO contact
    ) {
        String dateLabel =
                contact.getDate() != null
                        ? contact
                        .getDate()
                        .format(
                                CONTACT_DATE_DISPLAY_FORMATTER
                        )
                        : null;

        return new MyContactMessageSuggestResponse(
                contact.getId(),
                contact.getSubject(),
                contact.getStatusLabel(),
                contact.isResponseSent(),
                dateLabel
        );
    }

    /*
     * Vérifie si une notification correspond à la recherche normalisée
     * utilisée par l'autocomplétion et par la recherche de la page membre.
     */
    private boolean matchesNotificationSuggestion(
            NotificationResponseDTO notification,
            String normalizedQuery
    ) {
        String haystack = normalizeSearchValue(
                String.join(
                        " ",
                        safeValue(notification.getIdNotification()),
                        safeValue(notification.getTitleNotification()),
                        safeValue(notification.getMessageNotification()),
                        safeValue(notification.getTargetLinkNotification()),

                        notification.getCategoryNotification() != null
                                ? notification.getCategoryNotification().name()
                                : "",

                        notification.getTypeNotification() != null
                                ? notification.getTypeNotification().name()
                                : "",

                        safeValue(notification.getPriorityNotification()),

                        /*
                         * L'identité éventuelle de l'expéditeur humain peut être
                         * recherchée par le membre.
                         */
                        safeValue(notification.getSenderFirstName()),
                        safeValue(notification.getSenderLastName()),
                        safeValue(notification.getSenderEmail()),

                        Boolean.TRUE.equals(notification.getReadNotification())
                                ? "lue lu read"
                                : "non lue non lu unread",

                        Boolean.TRUE.equals(notification.getArchivedByRecipient())
                                ? "archive archivee archivée archived"
                                : "non archive non archivee non archivée",

                        notification.getDateNotification() != null
                                ? notification
                                .getDateNotification()
                                .format(NOTIFICATION_DATE_DISPLAY_FORMATTER)
                                : "",

                        notification.getDateNotification() != null
                                ? notification
                                .getDateNotification()
                                .toLocalDate()
                                .toString()
                                : "",

                        notification.getArchivedAtByRecipient() != null
                                ? notification
                                .getArchivedAtByRecipient()
                                .format(NOTIFICATION_DATE_DISPLAY_FORMATTER)
                                : ""
                )
        );

        return haystack.contains(
                normalizedQuery
        );
    }

    /*
     * Vérifie si un message de contact appartient au résultat de recherche.
     */
    private boolean matchesMyContactMessageSearch(
            ContactResponseDTO contact,
            String query
    ) {
        String normalizedQuery =
                normalizeSearchValue(
                        query
                );

        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String searchableText =
                buildMyContactMessageSearchableText(
                        contact
                );

        return searchableText.contains(
                normalizedQuery
        );
    }

    /*
     * Construit le texte de recherche des messages de contact du membre.
     *
     * Les informations d'archivage membre sont incluses afin de permettre
     * également une recherche par date d'archivage dans la boîte Archivés.
     *
     * L'état d'archivage ADMIN n'est volontairement jamais exposé ici.
     */
    private String buildMyContactMessageSearchableText(
            ContactResponseDTO contact
    ) {
        return normalizeSearchValue(
                String.join(
                        " ",
                        "message",
                        "contact",
                        safeValue(contact.getId()),
                        safeValue(contact.getSubject()),
                        safeValue(contact.getContent()),
                        safeValue(contact.getStatus()),
                        safeValue(contact.getStatusLabel()),
                        safeValue(contact.getResponseContent()),
                        safeValue(contact.getAnsweredByAdminLabel()),

                        contact.getDate() != null
                                ? contact
                                .getDate()
                                .format(CONTACT_DATE_DISPLAY_FORMATTER)
                                : "",

                        contact.getDate() != null
                                ? contact
                                .getDate()
                                .toLocalDate()
                                .toString()
                                : "",

                        contact.getArchivedAtByMember() != null
                                ? contact
                                .getArchivedAtByMember()
                                .format(CONTACT_DATE_DISPLAY_FORMATTER)
                                : "",

                        contact.getArchivedAtByMember() != null
                                ? contact
                                .getArchivedAtByMember()
                                .toLocalDate()
                                .toString()
                                : "",

                        contact.isResponseSent()
                                ? "repondu répondu answered traite traité"
                                : "nouveau new en attente non repondu non répondu",

                        contact.isArchivedByMember()
                                ? "archive archivee archivée archived"
                                : "actif active"
                )
        );
    }

    private boolean matchesMyLoanSearch(LoanResponseDTO loan, String query) {
        String normalizedQuery = normalizeSearchValue(query);

        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String searchableText = buildMyLoanSearchableText(loan);

        return searchableText.contains(normalizedQuery);
    }

    /*
     * Construit le texte de recherche des emprunts du membre.
     * Les titres des objets associés sont inclus pour permettre
     * une recherche plus naturelle depuis l'interface.
     */
    private String buildMyLoanSearchableText(LoanResponseDTO loan) {
        List<LoanLineResponseDTO> lines = loanLineService.getLoanLinesByLoanId(loan.getIdLoan());

        StringBuilder itemTitles = new StringBuilder();

        for (LoanLineResponseDTO line : lines) {
            if (line.getTitleItem() != null && !line.getTitleItem().trim().isEmpty()) {
                if (!itemTitles.isEmpty()) {
                    itemTitles.append(' ');
                }
                itemTitles.append(line.getTitleItem().trim());
            }
        }

        return normalizeSearchValue(
                String.join(" ",
                        "emprunt",
                        safeValue(loan.getIdLoan()),
                        "pret",
                        safeValue(loan.getIdLoan()),
                        "loan",
                        safeValue(loan.getIdLoan()),
                        safeValue(loan.getStatusLoan()),
                        safeValue(loan.getStatusLoanLabel()),
                        safeValue(loan.getOriginLoan()),
                        safeValue(loan.getOriginLoanLabel()),
                        formatDateTimeValue(loan.getStartDateLoan()),
                        formatDateValue(loan.getDueDateLoan()),
                        formatDateTimeValue(loan.getReturnDateLoan()),
                        Boolean.TRUE.equals(loan.getReturnedLoan())
                                ? "retourne rendu restitue retournee returned"
                                : "en cours actif ongoing non retourne",
                        Boolean.TRUE.equals(loan.getOverdueLoan()) ? "retard late overdue" : "",
                        itemTitles.toString()
                )
        );
    }

    /*
     * Trie les emprunts du membre lorsque la recherche est effectuée
     * côté mémoire.
     */
    private void sortMyLoans(List<LoanResponseDTO> loans, String sort) {
        Comparator<LoanResponseDTO> comparator;

        switch (sort) {
            case "oldest":
                comparator = Comparator
                        .comparing(PageController::safeStartDateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(PageController::safeLoanId, Comparator.nullsLast(Integer::compareTo));
                break;

            case "dueSoon":
                comparator = Comparator
                        .comparing(PageController::safeDueDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(PageController::safeStartDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PageController::safeLoanId, Comparator.nullsLast(Comparator.reverseOrder()));
                break;

            case "status":
                comparator = Comparator
                        .comparing((LoanResponseDTO loan) -> safeComparableText(loan.getStatusLoanLabel()))
                        .thenComparing(PageController::safeStartDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PageController::safeLoanId, Comparator.nullsLast(Comparator.reverseOrder()));
                break;

            case "origin":
                comparator = Comparator
                        .comparing((LoanResponseDTO loan) -> safeComparableText(loan.getOriginLoanLabel()))
                        .thenComparing(PageController::safeStartDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PageController::safeLoanId, Comparator.nullsLast(Comparator.reverseOrder()));
                break;

            case "recent":
            default:
                comparator = Comparator
                        .comparing(PageController::safeStartDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PageController::safeLoanId, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }

        loans.sort(comparator);
    }

    /*
     * Transforme une liste d'emprunts en page Spring.
     */
    private Page<LoanResponseDTO> toPage(
            List<LoanResponseDTO> loans,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : LOANS_PAGE_SIZE;
        int start = safePage * safeSize;

        if (start >= loans.size()) {
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(safePage, safeSize),
                    loans.size()
            );
        }

        int end = Math.min(
                start + safeSize,
                loans.size()
        );

        List<LoanResponseDTO> content =
                loans.subList(
                        start,
                        end
                );

        return new PageImpl<>(
                content,
                PageRequest.of(safePage, safeSize),
                loans.size()
        );
    }

    /*
     * Transforme une liste de messages de contact en page Spring.
     */
    private Page<ContactResponseDTO> toContactsPage(
            List<ContactResponseDTO> contacts,
            int page,
            int size
    ) {
        int safePage =
                Math.max(
                        page,
                        0
                );

        int safeSize =
                size > 0
                        ? size
                        : CONTACT_MESSAGES_PAGE_SIZE;

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

    /*
     * Résout la page à afficher pour garder visible le message sélectionné.
     */
    private int resolveContactPageIndex(
            List<ContactResponseDTO> contacts,
            ContactResponseDTO selectedContact,
            int requestedPage,
            int pageSize
    ) {
        int safeRequestedPage =
                Math.max(
                        requestedPage,
                        0
                );

        int safePageSize =
                pageSize > 0
                        ? pageSize
                        : CONTACT_MESSAGES_PAGE_SIZE;

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

    /*
     * Détermine si l'action de réinitialisation manuelle doit être exposée dans
     * l'interface d'accueil.
     *
     * La condition exige simultanément :
     * - le profil Spring demo ;
     * - l'activation générale du mécanisme de reset ;
     * - l'autorisation explicite du déclenchement manuel.
     *
     * Le rôle ADMIN reste contrôlé séparément par Spring Security dans la vue et
     * par le contrôleur dédié au reset.
     */
    private boolean isDemoManualResetEnabled() {
        boolean demoProfileActive =
                Arrays.stream(
                                environment.getActiveProfiles()
                        )
                        .anyMatch(
                                "demo"::equals
                        );

        boolean resetEnabled =
                environment.getProperty(
                        DEMO_RESET_ENABLED_PROPERTY,
                        Boolean.class,
                        false
                );

        boolean manualResetEnabled =
                environment.getProperty(
                        DEMO_MANUAL_RESET_ENABLED_PROPERTY,
                        Boolean.class,
                        false
                );

        return demoProfileActive
                && resetEnabled
                && manualResetEnabled;
    }

    /*
     * Vérifie si l'utilisateur authentifié possède le rôle administrateur.
     * Cette vérification centralise la règle d'accès utilisée par les pages SSR.
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {

            return false;
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .map(authority ->
                        authority.getAuthority()
                )
                .filter(
                        Objects::nonNull
                )
                .anyMatch(authority ->
                        authority.equals("ROLE_ADMIN")
                                || authority.equals("ADMIN")
                );
    }

    /*
     * Normalise les textes utilisés dans les recherches internes.
     */
    private String normalizeSearchValue(String value) {
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

    private String safeValue(Object value) {
        return value == null
                ? ""
                : String.valueOf(value);
    }

    private String safeComparableText(String value) {
        return value == null
                ? ""
                : value.toLowerCase(
                Locale.ROOT
        );
    }

    private String formatDateTimeValue(LocalDateTime value) {
        return value == null
                ? ""
                : value.format(
                LOAN_DATE_TIME_DISPLAY_FORMATTER
        );
    }

    private String formatDateValue(LocalDate value) {
        return value == null
                ? ""
                : value.format(
                LOAN_DATE_DISPLAY_FORMATTER
        );
    }

    private static LocalDateTime safeStartDateTime(
            LoanResponseDTO loan
    ) {
        return loan != null
                ? loan.getStartDateLoan()
                : null;
    }

    private static LocalDate safeDueDate(
            LoanResponseDTO loan
    ) {
        return loan != null
                ? loan.getDueDateLoan()
                : null;
    }

    private static Integer safeLoanId(
            LoanResponseDTO loan
    ) {
        return loan != null
                ? loan.getIdLoan()
                : null;
    }

    /*
     * Construit le résumé des objets associés à un emprunt.
     */
    private String buildLoanItemSummary(
            String firstTitle,
            int totalItems
    ) {
        if (totalItems <= 0) {
            return "Aucun objet associé";
        }

        String safeFirstTitle =
                firstTitle != null
                        && !firstTitle.isBlank()
                        ? firstTitle
                        : "Objet sans titre";

        if (totalItems == 1) {
            return safeFirstTitle;
        }

        return safeFirstTitle
                + " + "
                + (totalItems - 1)
                + " autre"
                + (
                totalItems - 1 > 1
                        ? "s"
                        : ""
        );
    }

    /*
     * DTO interne utilisé pour les suggestions d'emprunts du membre.
     */
    public static final class MyLoanSuggestResponse {

        private Integer idLoan;
        private String status;
        private String origin;

        public MyLoanSuggestResponse() {
        }

        public MyLoanSuggestResponse(
                Integer idLoan,
                String status,
                String origin
        ) {
            this.idLoan = idLoan;
            this.status = status;
            this.origin = origin;
        }

        public Integer getIdLoan() {
            return idLoan;
        }

        public void setIdLoan(Integer idLoan) {
            this.idLoan = idLoan;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }
    }

    /*
     * DTO interne utilisé pour les suggestions de notifications du membre.
     */
    public static final class MyNotificationSuggestResponse {

        private Integer idNotification;
        private String title;
        private String category;
        private String type;
        private String priority;
        private String readStatus;
        private String date;

        public MyNotificationSuggestResponse() {
        }

        public MyNotificationSuggestResponse(
                Integer idNotification,
                String title,
                String category,
                String type,
                String priority,
                String readStatus,
                String date
        ) {
            this.idNotification = idNotification;
            this.title = title;
            this.category = category;
            this.type = type;
            this.priority = priority;
            this.readStatus = readStatus;
            this.date = date;
        }

        public Integer getIdNotification() {
            return idNotification;
        }

        public void setIdNotification(Integer idNotification) {
            this.idNotification = idNotification;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getReadStatus() {
            return readStatus;
        }

        public void setReadStatus(String readStatus) {
            this.readStatus = readStatus;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    /*
     * DTO interne utilisé pour les suggestions de messages de contact du membre.
     */
    public static final class MyContactMessageSuggestResponse {

        private String id;
        private String subject;
        private String status;
        private Boolean responseSent;
        private String date;

        public MyContactMessageSuggestResponse() {
        }

        public MyContactMessageSuggestResponse(
                String id,
                String subject,
                String status,
                Boolean responseSent,
                String date
        ) {
            this.id = id;
            this.subject = subject;
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

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
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