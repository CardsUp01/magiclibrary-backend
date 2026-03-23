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

@Controller
public class AdminContactsPageController {

    private static final int CONTACTS_PAGE_SIZE = 9;
    private static final int CONTACTS_SUGGEST_LIMIT = 8;
    private static final DateTimeFormatter CONTACT_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ContactService contactService;

    public AdminContactsPageController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/admin/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public String showContactsPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedContactId", required = false) String selectedContactId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : CONTACTS_PAGE_SIZE;
        String resolvedQuery = q == null ? "" : q.trim();

        List<ContactResponseDTO> allContacts = contactService.getAllContacts();

        List<ContactResponseDTO> filteredContacts = resolvedQuery.isEmpty()
                ? allContacts
                : allContacts.stream()
                .filter(contact -> matchesContactSearch(contact, resolvedQuery))
                .toList();

        int totalElements = filteredContacts.size();
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / safeSize);

        if (safePage >= totalPages) {
            safePage = Math.max(totalPages - 1, 0);
        }

        int start = Math.min(safePage * safeSize, totalElements);
        int end = Math.min(start + safeSize, totalElements);
        List<ContactResponseDTO> pageContent = filteredContacts.subList(start, end);

        Page<ContactResponseDTO> contactsPage = new PageImpl<>(
                pageContent,
                PageRequest.of(safePage, safeSize),
                totalElements
        );

        ContactResponseDTO selectedContact = null;

        if (selectedContactId != null && !selectedContactId.isBlank()) {
            selectedContact = allContacts.stream()
                    .filter(contact -> Objects.equals(contact.getId(), selectedContactId))
                    .findFirst()
                    .orElse(null);
        }

        model.addAttribute("contacts", contactsPage.getContent());
        model.addAttribute("selectedContact", selectedContact);
        model.addAttribute("q", resolvedQuery);
        model.addAttribute("selectedContactId", selectedContactId);
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

    @PostMapping("/admin/messages/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public String respondToContact(
            @RequestParam("id") String id,
            @RequestParam("responseContent") String responseContent,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            RedirectAttributes redirectAttributes
    ) {
        String trimmedResponse = responseContent != null ? responseContent.trim() : "";

        if (trimmedResponse.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "La réponse administrateur est obligatoire.");
            redirectAttributes.addAttribute("selectedContactId", id);

            if (q != null && !q.trim().isEmpty()) {
                redirectAttributes.addAttribute("q", q.trim());
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

        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("size", size > 0 ? size : CONTACTS_PAGE_SIZE);

        return "redirect:/admin/messages";
    }

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

    private String normalizeSearchValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

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