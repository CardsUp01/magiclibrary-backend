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

import com.magiclibrary.dto.notification.NotificationResponseDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.exceptions.custom.NotificationNotFoundException;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.NotificationService;

@Controller
public class AdminNotificationsPageController {

    private static final int NOTIFICATIONS_PAGE_SIZE = 9;
    private static final int NOTIFICATIONS_FETCH_BATCH_SIZE = 200;
    private static final int NOTIFICATIONS_SUGGEST_LIMIT = 8;
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

    private User resolveCurrentUser(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;

        if (email == null || email.isBlank()) {
            throw new UserNotFoundException("Utilisateur introuvable.");
        }

        return userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable."));
    }

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

    private String normalizeSearchValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

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