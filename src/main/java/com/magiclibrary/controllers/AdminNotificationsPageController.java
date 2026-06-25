package com.magiclibrary.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.magiclibrary.dto.notification.NotificationResponseDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.exceptions.custom.NotificationNotFoundException;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.NotificationService;

/**
 * Contrôleur SSR réservé à l'administration des notifications.
 *
 * Cette classe gère l'affichage paginé des notifications,
 * la recherche, l'autocomplétion, la consultation du contenu ciblé
 * ainsi que le marquage des notifications comme lues.
 */
@Controller
public class AdminNotificationsPageController {

    /*
     * Paramètres utilisés par l'interface SSR des notifications :
     * pagination, chargement complet pour la recherche et limite
     * des suggestions affichées dans l'autocomplétion.
     */
    private static final int NOTIFICATIONS_PAGE_SIZE = 9;
    private static final int NOTIFICATIONS_FETCH_BATCH_SIZE = 200;
    private static final int NOTIFICATIONS_SUGGEST_LIMIT = 8;

    /*
     * Format d'affichage des dates utilisé dans l'interface
     * et les suggestions de notifications.
     */
    private static final DateTimeFormatter NOTIFICATION_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AdminNotificationsPageController(
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /*
     * Affiche la page d'administration des notifications.
     *
     * La méthode gère l'affichage paginé, la recherche, la sélection
     * d'une notification particulière et les indicateurs nécessaires
     * au rendu de la page Thymeleaf.
     */
    @GetMapping("/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String showNotificationsPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedNotificationId", required = false) Integer selectedNotificationId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            Authentication authentication,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : NOTIFICATIONS_PAGE_SIZE;
        String resolvedQuery = q == null ? "" : q.trim();

        User currentUser = resolveCurrentUser(authentication);
        Integer currentUserId = currentUser.getIdUser();

        Page<NotificationResponseDTO> notificationsPage;

        if (selectedNotificationId != null) {
            NotificationResponseDTO selectedNotification = fetchAllNotifications().stream()
                    .filter(notification -> Objects.equals(notification.getIdNotification(), selectedNotificationId))
                    .findFirst()
                    .orElseThrow(() -> new NotificationNotFoundException(
                            "Notification introuvable avec l'id : " + selectedNotificationId
                    ));

            List<NotificationResponseDTO> selectedNotifications = List.of(selectedNotification);

            notificationsPage = new PageImpl<>(
                    selectedNotifications,
                    PageRequest.of(0, safeSize),
                    selectedNotifications.size()
            );
        } else if (resolvedQuery.isEmpty()) {
            notificationsPage = notificationService.getAllNotificationsPaged(safePage, safeSize);
        } else {
            List<NotificationResponseDTO> filteredNotifications = fetchAllNotifications().stream()
                    .filter(notification -> matchesNotificationSearch(notification, resolvedQuery))
                    .toList();

            int start = Math.min(safePage * safeSize, filteredNotifications.size());
            int end = Math.min(start + safeSize, filteredNotifications.size());
            List<NotificationResponseDTO> pageContent = filteredNotifications.subList(start, end);

            notificationsPage = new PageImpl<>(
                    pageContent,
                    PageRequest.of(safePage, safeSize),
                    filteredNotifications.size()
            );
        }

        List<NotificationResponseDTO> notifications = notificationsPage.getContent();

        List<Boolean> currentUserNotifications = notifications.stream()
                .map(notification -> currentUserId != null && Objects.equals(currentUserId, notification.getIdUser()))
                .toList();

        boolean paginationEnabled = notificationsPage.getTotalElements() > safeSize;

        model.addAttribute("notifications", notifications);
        model.addAttribute("currentUserNotifications", currentUserNotifications);
        model.addAttribute("q", resolvedQuery);
        model.addAttribute("selectedNotificationId", selectedNotificationId);
        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("activePage", "admin-notifications");

        model.addAttribute("currentPage", notificationsPage.getNumber());
        model.addAttribute("pageSize", notificationsPage.getSize());
        model.addAttribute("totalPages", notificationsPage.getTotalPages());
        model.addAttribute("totalElements", notificationsPage.getTotalElements());
        model.addAttribute("hasPrevious", notificationsPage.hasPrevious());
        model.addAttribute("hasNext", notificationsPage.hasNext());
        model.addAttribute("isFirst", notificationsPage.isFirst());
        model.addAttribute("isLast", notificationsPage.isLast());
        model.addAttribute("paginationEnabled", paginationEnabled);

        return "admin/notifications";
    }

    /*
     * Ouvre la ressource ciblée par une notification.
     *
     * La notification est marquée comme lue avant la redirection
     * afin de refléter immédiatement sa consultation.
     */
    @GetMapping("/admin/notifications/{id}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public String openNotificationTarget(
            @PathVariable("id") Integer idNotification,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedNotificationId", required = false) Integer selectedNotificationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : NOTIFICATIONS_PAGE_SIZE;
        String resolvedQuery = q == null ? "" : q.trim();

        User currentUser = resolveCurrentUser(authentication);

        NotificationResponseDTO notification = fetchAllNotifications().stream()
                .filter(item -> Objects.equals(item.getIdNotification(), idNotification))
                .findFirst()
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification introuvable avec l'id : " + idNotification
                ));

        notificationService.markAsRead(idNotification, currentUser.getIdUser());

        String targetLink = notification.getTargetLinkNotification() != null
                ? notification.getTargetLinkNotification().trim()
                : "";

        if (targetLink.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Aucun détail disponible pour cette notification.");
            redirectAttributes.addAttribute("page", safePage);
            redirectAttributes.addAttribute("size", safeSize);

            if (!resolvedQuery.isEmpty()) {
                redirectAttributes.addAttribute("q", resolvedQuery);
            }

            if (selectedNotificationId != null) {
                redirectAttributes.addAttribute("selectedNotificationId", selectedNotificationId);
            }

            return "redirect:/admin/notifications";
        }

        return buildNotificationOpenRedirect(
                notification,
                targetLink,
                safePage,
                safeSize,
                resolvedQuery,
                selectedNotificationId
        );
    }

    /*
     * Fournit les suggestions de notifications utilisées
     * par l'autocomplétion de la page d'administration.
     */
    @GetMapping(value = "/admin/notifications/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationSuggestResponse>> suggestNotifications(
            @RequestParam(name = "q", required = false) String q
    ) {
        String normalizedQuery = normalizeSearchValue(q);

        if (normalizedQuery.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<NotificationSuggestResponse> suggestions = fetchAllNotifications().stream()
                .filter(notification -> matchesNotificationSearch(notification, normalizedQuery))
                .limit(NOTIFICATIONS_SUGGEST_LIMIT)
                .map(this::toSuggestResponse)
                .toList();

        return ResponseEntity.ok(suggestions);
    }

    /*
     * Marque explicitement une notification comme lue depuis l'interface.
     * Les paramètres de navigation sont conservés après redirection.
     */
    @PostMapping("/admin/notifications/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public String markNotificationAsRead(
            @PathVariable("id") Integer idNotification,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "9") int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "selectedNotificationId", required = false) Integer selectedNotificationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = resolveCurrentUser(authentication);

        notificationService.markAsRead(idNotification, currentUser.getIdUser());

        redirectAttributes.addFlashAttribute("successMessage", "Notification marquée comme lue.");

        if (selectedNotificationId != null) {
            redirectAttributes.addAttribute("selectedNotificationId", selectedNotificationId);
        }

        if (q != null && !q.trim().isEmpty()) {
            redirectAttributes.addAttribute("q", q.trim());
        }

        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("size", size > 0 ? size : NOTIFICATIONS_PAGE_SIZE);

        return "redirect:/admin/notifications";
    }

    /*
     * Construit la redirection adaptée au type de notification.
     *
     * Les notifications CONTACT conservent le contexte de navigation
     * entre les écrans notifications et messages.
     */
    private String buildNotificationOpenRedirect(
            NotificationResponseDTO notification,
            String targetLink,
            int notifPage,
            int notifSize,
            String notifQuery,
            Integer notifSelectedNotificationId
    ) {
        boolean isContactNotification = notification.getTypeNotification() != null
                && "CONTACT".equalsIgnoreCase(notification.getTypeNotification().name());

        if (isContactNotification) {
            String selectedContactId = extractQueryParameter(targetLink, "selectedContactId");

            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/messages")
                    .queryParam("from", "notifications")
                    .queryParam("notifPage", notifPage)
                    .queryParam("notifSize", notifSize);

            if (!notifQuery.isEmpty()) {
                builder.queryParam("notifQ", notifQuery);
            }

            if (notifSelectedNotificationId != null) {
                builder.queryParam("notifSelectedNotificationId", notifSelectedNotificationId);
            }

            if (selectedContactId != null && !selectedContactId.isBlank()) {
                builder.queryParam("selectedContactId", selectedContactId.trim());
            }

            return "redirect:" + builder.toUriString();
        }

        return "redirect:" + targetLink;
    }

    /*
     * Extrait la valeur d'un paramètre présent dans une URL.
     */
    private String extractQueryParameter(String url, String parameterName) {
        if (url == null || url.isBlank() || parameterName == null || parameterName.isBlank()) {
            return null;
        }

        String token = parameterName + "=";
        int startIndex = url.indexOf(token);

        if (startIndex < 0) {
            return null;
        }

        int valueStart = startIndex + token.length();
        int valueEnd = url.indexOf('&', valueStart);

        if (valueEnd < 0) {
            valueEnd = url.length();
        }

        if (valueStart >= valueEnd) {
            return null;
        }

        return url.substring(valueStart, valueEnd);
    }

    /*
     * Résout l'utilisateur authentifié courant à partir du contexte Spring Security.
     */
    private User resolveCurrentUser(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;

        if (email == null || email.isBlank()) {
            throw new UserNotFoundException("Utilisateur introuvable.");
        }

        return userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));
    }

    /*
     * Charge l'ensemble des notifications en plusieurs lots afin de permettre
     * les recherches et suggestions sur la totalité des données disponibles.
     */
    private List<NotificationResponseDTO> fetchAllNotifications() {
        List<NotificationResponseDTO> allNotifications = new ArrayList<>();

        int page = 0;
        Page<NotificationResponseDTO> notificationsPage;

        do {
            notificationsPage = notificationService.getAllNotificationsPaged(page, NOTIFICATIONS_FETCH_BATCH_SIZE);
            allNotifications.addAll(notificationsPage.getContent());
            page++;
        } while (notificationsPage.hasNext());

        return allNotifications;
    }

    private NotificationSuggestResponse toSuggestResponse(NotificationResponseDTO notification) {
        String date = notification.getDateNotification() != null
                ? notification.getDateNotification().format(NOTIFICATION_DATE_DISPLAY_FORMATTER)
                : null;

        return new NotificationSuggestResponse(
                notification.getIdNotification(),
                notification.getIdUser(),
                notification.getTitleNotification(),
                notification.getCategoryNotification() != null ? notification.getCategoryNotification().name() : null,
                notification.getTypeNotification() != null ? notification.getTypeNotification().name() : null,
                notification.getPriorityNotification(),
                Boolean.TRUE.equals(notification.getReadNotification()) ? "Lue" : "Non lue",
                date
        );
    }

    private boolean matchesNotificationSearch(NotificationResponseDTO notification, String query) {
        String normalizedQuery = normalizeSearchValue(query);
        String haystack = buildNotificationSearchHaystack(notification);

        return !normalizedQuery.isEmpty() && haystack.contains(normalizedQuery);
    }

    /*
     * Construit la chaîne utilisée par le moteur de recherche interne
     * afin de permettre une recherche sur plusieurs attributs simultanément.
     */
    private String buildNotificationSearchHaystack(NotificationResponseDTO notification) {
        LocalDateTime dateNotification = notification.getDateNotification();

        return normalizeSearchValue(
                String.join(" ",
                        safeValue(notification.getIdNotification()),
                        safeValue(notification.getIdUser()),
                        safeValue(notification.getTitleNotification()),
                        safeValue(notification.getMessageNotification()),
                        safeValue(notification.getTargetLinkNotification()),
                        notification.getCategoryNotification() != null ? notification.getCategoryNotification().name() : "",
                        notification.getTypeNotification() != null ? notification.getTypeNotification().name() : "",
                        safeValue(notification.getPriorityNotification()),
                        dateNotification != null ? dateNotification.format(NOTIFICATION_DATE_DISPLAY_FORMATTER) : "",
                        dateNotification != null ? dateNotification.toLocalDate().toString() : "",
                        Boolean.TRUE.equals(notification.getReadNotification())
                                ? "lue lu read traitee traitée"
                                : "non lue non lu unread nouvelle active"
                )
        );
    }

    /*
     * Normalise une valeur textuelle avant comparaison dans les recherches.
     */
    private String normalizeSearchValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /*
     * DTO interne utilisé uniquement pour exposer les suggestions
     * de notifications au format JSON.
     */
    public static final class NotificationSuggestResponse {

        private Integer idNotification;
        private Integer idUser;
        private String title;
        private String category;
        private String type;
        private String priority;
        private String readStatus;
        private String date;

        public NotificationSuggestResponse() {
        }

        public NotificationSuggestResponse(
                Integer idNotification,
                Integer idUser,
                String title,
                String category,
                String type,
                String priority,
                String readStatus,
                String date
        ) {
            this.idNotification = idNotification;
            this.idUser = idUser;
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

        public Integer getIdUser() {
            return idUser;
        }

        public void setIdUser(Integer idUser) {
            this.idUser = idUser;
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
}