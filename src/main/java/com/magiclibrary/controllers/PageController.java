package com.magiclibrary.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private static final int NOTIFICATIONS_SUGGEST_LIMIT = 8;
    private static final int CONTACT_MESSAGES_PAGE_SIZE = 9;
    private static final int CONTACT_MESSAGES_SUGGEST_LIMIT = 8;

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

    public PageController(
            UserRepository userRepository,
            ContactService contactService,
            LoanService loanService,
            LoanLineService loanLineService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.contactService = contactService;
        this.loanService = loanService;
        this.loanLineService = loanLineService;
        this.notificationService = notificationService;
    }

    /*
     * Affiche la page de connexion ou redirige l'utilisateur déjà authentifié
     * vers l'accueil de l'application.
     */
    @GetMapping({"/", "/login"})
    public String loginPage(Authentication authentication, HttpServletResponse response) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/accueil";
        }

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        return "login";
    }

    @GetMapping("/accueil")
    public String accueilPage(Model model) {
        model.addAttribute("activePage", "accueil");
        return "accueil";
    }

    @GetMapping("/mentions-legales")
    public String legalNoticePage(Model model) {
        model.addAttribute("activePage", "mentions-legales");
        return "mentions-legales";
    }

    @GetMapping("/confidentialite")
    public String confidentialitePage(Model model) {
        model.addAttribute("activePage", "confidentialite");
        return "confidentialite";
    }

    @GetMapping("/accessibilite")
    public String accessibilitePage(Model model) {
        model.addAttribute("activePage", "accessibilite");
        model.addAttribute("pageTitle", "Accessibilité");
        return "accessibilite";
    }

    @GetMapping("/cgu")
    public String cguPage(Model model) {
        model.addAttribute("activePage", "cgu");
        model.addAttribute("pageTitle", "Conditions générales d’utilisation (CGU)");
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
    public String contactPage(Authentication authentication, Model model) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/messages";
        }

        model.addAttribute("activePage", "contact");

        String email = authentication != null ? authentication.getName() : null;
        model.addAttribute("prefillEmail", email);

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
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "message", required = false) String message,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (isAdmin(authentication)) {
            return "redirect:/admin/messages";
        }

        String authEmail = authentication.getName();

        User user = userRepository.findByEmailUser(authEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        if (subject == null || subject.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sujet et message sont obligatoires.");
            return "redirect:/contact";
        }

        ContactRequestDTO requestDTO = new ContactRequestDTO();
        requestDTO.setIdUser(user.getIdUser());
        requestDTO.setName(name);
        requestDTO.setEmail(authEmail);
        requestDTO.setSubject(subject);
        requestDTO.setMessage(message);

        contactService.createContact(requestDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Message envoyé. Nous te répondrons dès que possible.");
        return "redirect:/contact";
    }

    /*
     * Affiche les messages de contact du membre connecté.
     *
     * La méthode gère la recherche, la pagination et la sélection
     * du message actif dans l'interface.
     */
    @GetMapping("/mes-messages-de-contact")
    public String myContactMessagesPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedContactId", required = false) String selectedContactId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Authentication authentication,
            Model model
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : CONTACT_MESSAGES_PAGE_SIZE;
        String resolvedQuery = q == null ? "" : q.trim();

        List<ContactResponseDTO> memberContacts = contactService.getContactsForUser(user.getIdUser());

        List<ContactResponseDTO> filteredContacts = resolvedQuery.isEmpty()
                ? memberContacts
                : memberContacts.stream()
                .filter(contact -> matchesMyContactMessageSearch(contact, resolvedQuery))
                .toList();

        ContactResponseDTO selectedContact = null;

        if (selectedContactId != null && !selectedContactId.isBlank()) {
            selectedContact = contactService.getContactByIdForUser(selectedContactId.trim(), user.getIdUser());

            if (selectedContact != null) {
                final String selectedId = selectedContact.getId();
                boolean existsInFilteredList = filteredContacts.stream()
                        .anyMatch(contact -> Objects.equals(contact.getId(), selectedId));

                if (!existsInFilteredList) {
                    selectedContact = null;
                }
            }
        }

        int resolvedPage = resolveContactPageIndex(filteredContacts, selectedContact, safePage, safeSize);
        Page<ContactResponseDTO> contactsPage = toContactsPage(filteredContacts, resolvedPage, safeSize);

        if (selectedContact == null && !contactsPage.getContent().isEmpty()) {
            selectedContact = contactsPage.getContent().get(0);
        }

        String resolvedSelectedContactId = selectedContact != null ? selectedContact.getId() : null;

        model.addAttribute("contacts", contactsPage.getContent());
        model.addAttribute("selectedContact", selectedContact);
        model.addAttribute("selectedContactId", resolvedSelectedContactId);
        model.addAttribute("q", resolvedQuery);
        model.addAttribute("pageTitle", "Mes messages de contact");
        model.addAttribute("activePage", "mes-messages-de-contact");

        model.addAttribute("currentPage", contactsPage.getNumber());
        model.addAttribute("pageSize", contactsPage.getSize());
        model.addAttribute("totalPages", contactsPage.getTotalPages());
        model.addAttribute("totalElements", contactsPage.getTotalElements());
        model.addAttribute("hasPrevious", contactsPage.hasPrevious());
        model.addAttribute("hasNext", contactsPage.hasNext());
        model.addAttribute("isFirst", contactsPage.isFirst());
        model.addAttribute("isLast", contactsPage.isLast());
        model.addAttribute("paginationEnabled", contactsPage.getTotalElements() > safeSize);

        return "mes-messages-de-contact";
    }

    /*
     * Fournit les suggestions pour l'autocomplétion des messages
     * de contact du membre connecté.
     */
    @GetMapping(value = "/mes-messages-de-contact/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<MyContactMessageSuggestResponse>> suggestMyContactMessages(
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

        List<MyContactMessageSuggestResponse> suggestions = contactService.getContactsForUser(user.getIdUser()).stream()
                .filter(contact -> matchesMyContactMessageSearch(contact, normalizedQuery))
                .limit(CONTACT_MESSAGES_SUGGEST_LIMIT)
                .map(this::toMyContactMessageSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
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

    /*
     * Affiche les notifications du membre connecté.
     */
    @GetMapping("/mes-notifications")
    public String myNotificationsPage(
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

        Page<NotificationResponseDTO> notificationsPage =
                notificationService.getNotificationsForUserPaged(user.getIdUser(), safePage, safeSize);

        List<NotificationResponseDTO> notifications = notificationsPage.getContent();
        boolean paginationEnabled = notificationsPage.getTotalElements() > NOTIFICATIONS_PAGE_SIZE;

        model.addAttribute("notifications", notifications);
        model.addAttribute("q", q);
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
     */
    @GetMapping(value = "/mes-notifications/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<MyNotificationSuggestResponse>> suggestMyNotifications(
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

        List<MyNotificationSuggestResponse> suggestions = notificationService.getNotificationsForUser(user.getIdUser())
                .stream()
                .filter(notification -> matchesNotificationSuggestion(notification, normalizedQuery))
                .limit(NOTIFICATIONS_SUGGEST_LIMIT)
                .map(this::toMyNotificationSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    /*
     * Marque une notification comme lue depuis la page membre
     * puis redirige vers la liste des notifications.
     */
    @PostMapping("/mes-notifications/{id}/read")
    public String markNotificationAsReadFromPage(
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

        notificationService.markAsRead(idNotification, user.getIdUser());

        redirectAttributes.addFlashAttribute("successMessage", "Notification marquée comme lue.");

        StringBuilder redirectUrl = new StringBuilder("redirect:/mes-notifications?page=")
                .append(Math.max(page, 0))
                .append("&size=")
                .append(size > 0 ? size : NOTIFICATIONS_PAGE_SIZE);

        if (q != null && !q.trim().isEmpty()) {
            redirectAttributes.addAttribute("q", q.trim());
            return "redirect:/mes-notifications?page=" + Math.max(page, 0) + "&size=" + (size > 0 ? size : NOTIFICATIONS_PAGE_SIZE);
        }

        return redirectUrl.toString();
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

    private MyNotificationSuggestResponse toMyNotificationSuggestResponse(NotificationResponseDTO notification) {
        String dateLabel = notification.getDateNotification() != null
                ? notification.getDateNotification().format(NOTIFICATION_DATE_DISPLAY_FORMATTER)
                : null;

        String readLabel = Boolean.TRUE.equals(notification.getReadNotification()) ? "Lue" : "Non lue";

        return new MyNotificationSuggestResponse(
                notification.getIdNotification(),
                notification.getTitleNotification(),
                notification.getCategoryNotification() != null ? notification.getCategoryNotification().name() : null,
                notification.getTypeNotification() != null ? notification.getTypeNotification().name() : null,
                notification.getPriorityNotification(),
                readLabel,
                dateLabel
        );
    }

    private MyContactMessageSuggestResponse toMyContactMessageSuggestResponse(ContactResponseDTO contact) {
        String dateLabel = contact.getDate() != null
                ? contact.getDate().format(CONTACT_DATE_DISPLAY_FORMATTER)
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
     * utilisée par l'autocomplétion.
     */
    private boolean matchesNotificationSuggestion(NotificationResponseDTO notification, String normalizedQuery) {
        String haystack = normalizeSearchValue(
                String.join(" ",
                        safeValue(notification.getIdNotification()),
                        safeValue(notification.getTitleNotification()),
                        safeValue(notification.getMessageNotification()),
                        safeValue(notification.getTargetLinkNotification()),
                        notification.getCategoryNotification() != null ? notification.getCategoryNotification().name() : "",
                        notification.getTypeNotification() != null ? notification.getTypeNotification().name() : "",
                        safeValue(notification.getPriorityNotification()),
                        Boolean.TRUE.equals(notification.getReadNotification()) ? "lue lu read" : "non lue non lu unread",
                        notification.getDateNotification() != null
                                ? notification.getDateNotification().format(NOTIFICATION_DATE_DISPLAY_FORMATTER)
                                : ""
                )
        );

        return haystack.contains(normalizedQuery);
    }

    private boolean matchesMyContactMessageSearch(ContactResponseDTO contact, String query) {
        String normalizedQuery = normalizeSearchValue(query);

        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String searchableText = buildMyContactMessageSearchableText(contact);

        return searchableText.contains(normalizedQuery);
    }

    /*
     * Construit le texte de recherche des messages de contact du membre.
     */
    private String buildMyContactMessageSearchableText(ContactResponseDTO contact) {
        return normalizeSearchValue(
                String.join(" ",
                        "message",
                        "contact",
                        safeValue(contact.getId()),
                        safeValue(contact.getSubject()),
                        safeValue(contact.getContent()),
                        safeValue(contact.getStatus()),
                        safeValue(contact.getStatusLabel()),
                        safeValue(contact.getResponseContent()),
                        safeValue(contact.getAnsweredByAdminLabel()),
                        contact.getDate() != null ? contact.getDate().format(CONTACT_DATE_DISPLAY_FORMATTER) : "",
                        contact.getDate() != null ? contact.getDate().toLocalDate().toString() : "",
                        contact.isResponseSent()
                                ? "repondu répondu answered traite traité"
                                : "nouveau new en attente non repondu non répondu"
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
    private Page<LoanResponseDTO> toPage(List<LoanResponseDTO> loans, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : LOANS_PAGE_SIZE;
        int start = safePage * safeSize;

        if (start >= loans.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(safePage, safeSize), loans.size());
        }

        int end = Math.min(start + safeSize, loans.size());
        List<LoanResponseDTO> content = loans.subList(start, end);

        return new PageImpl<>(content, PageRequest.of(safePage, safeSize), loans.size());
    }

    /*
     * Transforme une liste de messages de contact en page Spring.
     */
    private Page<ContactResponseDTO> toContactsPage(List<ContactResponseDTO> contacts, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : CONTACT_MESSAGES_PAGE_SIZE;
        int start = safePage * safeSize;

        if (start >= contacts.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(safePage, safeSize), contacts.size());
        }

        int end = Math.min(start + safeSize, contacts.size());
        List<ContactResponseDTO> content = contacts.subList(start, end);

        return new PageImpl<>(content, PageRequest.of(safePage, safeSize), contacts.size());
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
        int safeRequestedPage = Math.max(requestedPage, 0);
        int safePageSize = pageSize > 0 ? pageSize : CONTACT_MESSAGES_PAGE_SIZE;

        if (selectedContact != null) {
            for (int i = 0; i < contacts.size(); i++) {
                ContactResponseDTO contact = contacts.get(i);

                if (Objects.equals(contact.getId(), selectedContact.getId())) {
                    return i / safePageSize;
                }
            }
        }

        if (contacts.isEmpty()) {
            return 0;
        }

        int lastPage = (contacts.size() - 1) / safePageSize;
        return Math.min(safeRequestedPage, lastPage);
    }

    /*
     * Vérifie si l'utilisateur authentifié possède le rôle administrateur.
     * Cette vérification centralise la règle d'accès utilisée par les pages SSR.
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(Objects::nonNull)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ADMIN"));
    }

    /*
     * Normalise les textes utilisés dans les recherches internes.
     */
    private String normalizeSearchValue(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

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
        return value == null ? "" : String.valueOf(value);
    }

    private String safeComparableText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String formatDateTimeValue(LocalDateTime value) {
        return value == null ? "" : value.format(LOAN_DATE_TIME_DISPLAY_FORMATTER);
    }

    private String formatDateValue(LocalDate value) {
        return value == null ? "" : value.format(LOAN_DATE_DISPLAY_FORMATTER);
    }

    private static LocalDateTime safeStartDateTime(LoanResponseDTO loan) {
        return loan != null ? loan.getStartDateLoan() : null;
    }

    private static LocalDate safeDueDate(LoanResponseDTO loan) {
        return loan != null ? loan.getDueDateLoan() : null;
    }

    private static Integer safeLoanId(LoanResponseDTO loan) {
        return loan != null ? loan.getIdLoan() : null;
    }

    /*
     * Construit le résumé des objets associés à un emprunt.
     */
    private String buildLoanItemSummary(String firstTitle, int totalItems) {
        if (totalItems <= 0) {
            return "Aucun objet associé";
        }

        String safeFirstTitle = firstTitle != null && !firstTitle.isBlank()
                ? firstTitle
                : "Objet sans titre";

        if (totalItems == 1) {
            return safeFirstTitle;
        }

        return safeFirstTitle + " + " + (totalItems - 1) + " autre" + (totalItems - 1 > 1 ? "s" : "");
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

        public MyLoanSuggestResponse(Integer idLoan, String status, String origin) {
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