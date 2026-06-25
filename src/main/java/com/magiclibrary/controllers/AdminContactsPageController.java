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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.services.ContactService;

/**
 * Contrôleur SSR réservé à l'administration des messages de contact.
 *
 * Cette classe gère l'affichage paginé des messages, la sélection d'un message,
 * la recherche, l'autocomplétion et l'envoi d'une réponse administrateur.
 *
 * Les données manipulées proviennent du module CONTACT stocké dans MongoDB.
 */
@Controller
public class AdminContactsPageController {

    /*
     * Paramètres d'affichage utilisés par la page SSR des messages.
     * Ils centralisent la pagination, la limite d'autocomplétion
     * et le format de date présenté dans l'interface.
     */
    private static final int CONTACTS_PAGE_SIZE = 9;
    private static final int CONTACTS_SUGGEST_LIMIT = 8;
    private static final DateTimeFormatter CONTACT_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ContactService contactService;

    public AdminContactsPageController(ContactService contactService) {
        this.contactService = contactService;
    }

    /*
     * Affiche la page d'administration des messages de contact.
     *
     * La méthode conserve les paramètres de navigation afin de permettre
     * un retour cohérent entre les messages de contact et les notifications.
     */
    @GetMapping("/admin/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public String showContactsPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedContactId", required = false) String selectedContactId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "notifPage", required = false) Integer notifPage,
            @RequestParam(name = "notifSize", required = false) Integer notifSize,
            @RequestParam(name = "notifQ", required = false) String notifQ,
            @RequestParam(name = "notifSelectedNotificationId", required = false) Integer notifSelectedNotificationId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : CONTACTS_PAGE_SIZE;
        String resolvedQuery = q == null ? "" : q.trim();
        String resolvedFrom = from == null ? "" : from.trim();
        int resolvedNotifPage = notifPage != null ? Math.max(notifPage, 0) : 0;
        int resolvedNotifSize = notifSize != null && notifSize > 0 ? notifSize : CONTACTS_PAGE_SIZE;
        String resolvedNotifQuery = notifQ == null ? "" : notifQ.trim();

        List<ContactResponseDTO> allContacts = contactService.getAllContacts();

        ContactResponseDTO selectedContact = null;

        if (selectedContactId != null && !selectedContactId.isBlank()) {
            selectedContact = allContacts.stream()
                    .filter(contact -> Objects.equals(contact.getId(), selectedContactId.trim()))
                    .findFirst()
                    .orElse(null);
        }

        List<ContactResponseDTO> filteredContacts = resolvedQuery.isEmpty()
                ? allContacts
                : allContacts.stream()
                .filter(contact -> matchesContactSearch(contact, resolvedQuery))
                .toList();

        int resolvedPage = resolveContactPageIndex(filteredContacts, selectedContact, safePage, safeSize);

        Page<ContactResponseDTO> contactsPage = toContactsPage(filteredContacts, resolvedPage, safeSize);

        String resolvedSelectedContactId = selectedContact != null ? selectedContact.getId() : null;

        model.addAttribute("contacts", contactsPage.getContent());
        model.addAttribute("selectedContact", selectedContact);
        model.addAttribute("q", resolvedQuery);
        model.addAttribute("from", resolvedFrom);
        model.addAttribute("notifPage", resolvedNotifPage);
        model.addAttribute("notifSize", resolvedNotifSize);
        model.addAttribute("notifQ", resolvedNotifQuery);
        model.addAttribute("notifSelectedNotificationId", notifSelectedNotificationId);
        model.addAttribute("selectedContactId", resolvedSelectedContactId);
        model.addAttribute("pageTitle", "Messages de contact");
        model.addAttribute("activePage", "admin-contacts");

        model.addAttribute("currentPage", contactsPage.getNumber());
        model.addAttribute("pageSize", contactsPage.getSize());
        model.addAttribute("totalPages", contactsPage.getTotalPages());
        model.addAttribute("totalElements", contactsPage.getTotalElements());
        model.addAttribute("hasPrevious", contactsPage.hasPrevious());
        model.addAttribute("hasNext", contactsPage.hasNext());
        model.addAttribute("isFirst", contactsPage.isFirst());
        model.addAttribute("isLast", contactsPage.isLast());
        model.addAttribute("paginationEnabled", contactsPage.getTotalElements() > safeSize);

        return "admin/messages";
    }

    /*
     * Traite la réponse administrateur à un message de contact.
     *
     * Les paramètres de navigation sont réinjectés dans la redirection
     * afin de conserver le contexte d'affichage après l'action POST.
     */
    @PostMapping("/admin/messages/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public String respondToContact(
            @RequestParam("id") String id,
            @RequestParam("responseContent") String responseContent,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "notifPage", required = false) Integer notifPage,
            @RequestParam(name = "notifSize", required = false) Integer notifSize,
            @RequestParam(name = "notifQ", required = false) String notifQ,
            @RequestParam(name = "notifSelectedNotificationId", required = false) Integer notifSelectedNotificationId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            RedirectAttributes redirectAttributes
    ) {
        String trimmedResponse = responseContent != null ? responseContent.trim() : "";
        String resolvedFrom = from == null ? "" : from.trim();
        int resolvedNotifPage = notifPage != null ? Math.max(notifPage, 0) : 0;
        int resolvedNotifSize = notifSize != null && notifSize > 0 ? notifSize : CONTACTS_PAGE_SIZE;
        String resolvedNotifQuery = notifQ == null ? "" : notifQ.trim();

        if (trimmedResponse.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "La réponse administrateur est obligatoire.");
            redirectAttributes.addAttribute("selectedContactId", id);

            if (q != null && !q.trim().isEmpty()) {
                redirectAttributes.addAttribute("q", q.trim());
            }

            if (!resolvedFrom.isEmpty()) {
                redirectAttributes.addAttribute("from", resolvedFrom);
            }

            if (notifPage != null) {
                redirectAttributes.addAttribute("notifPage", resolvedNotifPage);
            }

            if (notifSize != null) {
                redirectAttributes.addAttribute("notifSize", resolvedNotifSize);
            }

            if (!resolvedNotifQuery.isEmpty()) {
                redirectAttributes.addAttribute("notifQ", resolvedNotifQuery);
            }

            if (notifSelectedNotificationId != null) {
                redirectAttributes.addAttribute("notifSelectedNotificationId", notifSelectedNotificationId);
            }

            redirectAttributes.addAttribute("page", Math.max(page, 0));
            redirectAttributes.addAttribute("size", size > 0 ? size : CONTACTS_PAGE_SIZE);

            return "redirect:/admin/messages";
        }

        ContactReplyRequestDTO requestDTO = new ContactReplyRequestDTO();
        requestDTO.setResponseContent(trimmedResponse);

        try {
            contactService.replyToContact(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Réponse envoyée avec succès.");
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "Impossible d’envoyer la réponse.";
            redirectAttributes.addFlashAttribute("errorMessage", message);
        }

        redirectAttributes.addAttribute("selectedContactId", id);

        if (q != null && !q.trim().isEmpty()) {
            redirectAttributes.addAttribute("q", q.trim());
        }

        if (!resolvedFrom.isEmpty()) {
            redirectAttributes.addAttribute("from", resolvedFrom);
        }

        if (notifPage != null) {
            redirectAttributes.addAttribute("notifPage", resolvedNotifPage);
        }

        if (notifSize != null) {
            redirectAttributes.addAttribute("notifSize", resolvedNotifSize);
        }

        if (!resolvedNotifQuery.isEmpty()) {
            redirectAttributes.addAttribute("notifQ", resolvedNotifQuery);
        }

        if (notifSelectedNotificationId != null) {
            redirectAttributes.addAttribute("notifSelectedNotificationId", notifSelectedNotificationId);
        }

        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("size", size > 0 ? size : CONTACTS_PAGE_SIZE);

        return "redirect:/admin/messages";
    }

    /*
     * Fournit les suggestions de messages pour l'autocomplétion côté SSR.
     * Le résultat est volontairement limité pour préserver un affichage léger.
     */
    @GetMapping(value = "/admin/messages/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContactSuggestResponse>> suggestContacts(
            @RequestParam(name = "q", required = false) String q
    ) {
        String normalizedQuery = normalizeSearchValue(q);

        if (normalizedQuery.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<ContactSuggestResponse> suggestions = contactService.getAllContacts().stream()
                .filter(contact -> matchesContactSearch(contact, normalizedQuery))
                .limit(CONTACTS_SUGGEST_LIMIT)
                .map(this::toSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    /*
     * Transforme une liste de contacts en page Spring.
     * La pagination est effectuée côté mémoire car les contacts proviennent
     * du service MongoDB sous forme de liste déjà chargée.
     */
    private Page<ContactResponseDTO> toContactsPage(List<ContactResponseDTO> contacts, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : CONTACTS_PAGE_SIZE;
        int start = safePage * safeSize;

        if (start >= contacts.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(safePage, safeSize), contacts.size());
        }

        int end = Math.min(start + safeSize, contacts.size());
        List<ContactResponseDTO> content = contacts.subList(start, end);

        return new PageImpl<>(content, PageRequest.of(safePage, safeSize), contacts.size());
    }

    /*
     * Détermine la page à afficher lorsque l'utilisateur sélectionne un message.
     * Si le message sélectionné est trouvé, la pagination est ajustée pour
     * que ce message reste visible dans la liste.
     */
    private int resolveContactPageIndex(
            List<ContactResponseDTO> contacts,
            ContactResponseDTO selectedContact,
            int requestedPage,
            int pageSize
    ) {
        int safeRequestedPage = Math.max(requestedPage, 0);
        int safePageSize = pageSize > 0 ? pageSize : CONTACTS_PAGE_SIZE;

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
     * Convertit un contact complet en réponse réduite pour l'autocomplétion.
     */
    private ContactSuggestResponse toSuggestResponse(ContactResponseDTO contact) {
        String date = contact.getDate() != null
                ? contact.getDate().format(CONTACT_DATE_DISPLAY_FORMATTER)
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

    private boolean matchesContactSearch(ContactResponseDTO contact, String query) {
        String normalizedQuery = normalizeSearchValue(query);
        String haystack = buildContactSearchHaystack(contact);

        return !normalizedQuery.isEmpty() && haystack.contains(normalizedQuery);
    }

    /*
     * Construit la chaîne de recherche d'un message de contact.
     * Elle regroupe les champs utiles à la recherche administrateur :
     * identité, email, contenu, statut, rôle, réponse et dates.
     */
    private String buildContactSearchHaystack(ContactResponseDTO contact) {
        LocalDateTime dateContact = contact.getDate();

        return normalizeSearchValue(
                String.join(" ",
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
                        dateContact != null ? dateContact.format(CONTACT_DATE_DISPLAY_FORMATTER) : "",
                        dateContact != null ? dateContact.toLocalDate().toString() : "",
                        contact.isResponseSent()
                                ? "repondu répondu answered traite traité"
                                : "nouveau new en attente non repondu non répondu"
                )
        );
    }

    /*
     * Normalise une valeur textuelle pour la recherche.
     * La méthode uniformise la casse et remplace les caractères accentués
     * afin de rendre la recherche plus tolérante aux saisies utilisateur.
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

    /*
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