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
 * =============================================================================
 * CONTROLEUR SSR : AdminNotificationsPageController
 * =============================================================================
 *
 * Contrôleur réservé à l'espace Notifications de l'administration.
 *
 * L'interface distingue désormais trois boîtes fonctionnelles :
 *
 * REÇUES
 *      Notifications dont l'administrateur authentifié est le destinataire
 *      et qui n'ont pas été archivées.
 *
 * ENVOYÉES
 *      Historique partagé des notifications manuelles envoyées par l'équipe
 *      administrative.
 *
 * ARCHIVÉES
 *      Notifications reçues puis archivées logiquement par l'administrateur
 *      authentifié.
 *
 * ---------------------------------------------------------------------------
 * PRINCIPES DE SÉCURITÉ
 * ---------------------------------------------------------------------------
 *
 * - toutes les routes sont réservées au rôle ADMIN ;
 * - les boîtes Reçues et Archivées sont strictement rattachées à
 *   l'utilisateur authentifié ;
 * - l'historique Envoyées passe également par le service métier qui contrôle
 *   le rôle ADMIN ;
 * - une notification ne peut être marquée comme lue que par son destinataire ;
 * - une notification ne peut être archivée/restaurée que par son destinataire ;
 * - aucune suppression physique n'est exposée ici.
 *
 * ---------------------------------------------------------------------------
 * RECHERCHE
 * ---------------------------------------------------------------------------
 *
 * La recherche et les suggestions sont toujours limitées à la boîte active.
 *
 * Une recherche effectuée dans "Reçues" ne peut donc jamais exposer une
 * notification appartenant uniquement aux archives ou à l'historique
 * administratif des envois.
 * =============================================================================
 */
@Controller
public class AdminNotificationsPageController {

    // =========================================================================
    // CONSTANTES
    // =========================================================================

    /**
     * Taille par défaut d'une page de notifications.
     */
    private static final int NOTIFICATIONS_PAGE_SIZE = 9;

    /**
     * Taille des lots utilisés lorsqu'il est nécessaire de charger l'ensemble
     * d'une boîte pour la recherche ou l'autocomplétion.
     */
    private static final int NOTIFICATIONS_FETCH_BATCH_SIZE = 200;

    /**
     * Nombre maximal de suggestions proposées dans l'autocomplétion.
     */
    private static final int NOTIFICATIONS_SUGGEST_LIMIT = 8;

    /**
     * Format d'affichage utilisé dans les suggestions et le moteur de recherche.
     */
    private static final DateTimeFormatter NOTIFICATION_DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================================
    // DÉPENDANCES
    // =========================================================================

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Injection des dépendances par constructeur.
     *
     * @param notificationService service métier des notifications
     * @param userRepository repository permettant de résoudre l'utilisateur
     *                       actuellement authentifié
     */
    public AdminNotificationsPageController(
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // GET : PAGE PRINCIPALE DES NOTIFICATIONS
    // =========================================================================

    /**
     * Affiche la page d'administration des notifications.
     *
     * Le paramètre {@code box} détermine la boîte actuellement consultée :
     *
     * - received : Reçues ;
     * - sent : Envoyées ;
     * - archived : Archivées.
     *
     * Toute valeur inconnue est ramenée de manière défensive vers la boîte
     * Reçues.
     *
     * @param box boîte demandée
     * @param q recherche éventuelle
     * @param selectedNotificationId notification sélectionnée depuis une
     *                               suggestion
     * @param page index de page
     * @param size taille de page
     * @param authentication contexte Spring Security
     * @param model modèle Thymeleaf
     * @return template admin/notifications
     */
    @GetMapping("/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String showNotificationsPage(
            @RequestParam(name = "box", required = false, defaultValue = "received")
            String box,
            @RequestParam(name = "q", required = false)
            String q,
            @RequestParam(name = "selectedNotificationId", required = false)
            Integer selectedNotificationId,
            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,
            Authentication authentication,
            Model model
    ) {
        int safePage =
                Math.max(page, 0);

        int safeSize =
                size > 0
                        ? size
                        : NOTIFICATIONS_PAGE_SIZE;

        String resolvedQuery =
                q == null
                        ? ""
                        : q.trim();

        NotificationBox resolvedBox =
                NotificationBox.fromRequestValue(box);

        User currentUser =
                resolveCurrentUser(authentication);

        Integer currentUserId =
                currentUser.getIdUser();

        Page<NotificationResponseDTO> notificationsPage;

        /*
         * Lorsqu'une suggestion précise a été sélectionnée, on recherche
         * exclusivement cette notification dans la boîte actuellement active.
         *
         * Cela évite qu'un identifiant appartenant à une autre boîte puisse
         * être utilisé pour contourner le cloisonnement fonctionnel.
         */
        if (selectedNotificationId != null) {

            NotificationResponseDTO selectedNotification =
                    fetchNotificationsForBox(
                            resolvedBox,
                            currentUserId
                    )
                            .stream()
                            .filter(notification ->
                                    Objects.equals(
                                            notification.getIdNotification(),
                                            selectedNotificationId
                                    )
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new NotificationNotFoundException(
                                            "Notification introuvable dans cette boîte avec l'id : "
                                                    + selectedNotificationId
                                    )
                            );

            List<NotificationResponseDTO> selectedNotifications =
                    List.of(selectedNotification);

            notificationsPage =
                    new PageImpl<>(
                            selectedNotifications,
                            PageRequest.of(0, safeSize),
                            selectedNotifications.size()
                    );

        } else if (resolvedQuery.isEmpty()) {

            /*
             * Sans recherche, on utilise directement la pagination SQL
             * correspondant à la boîte active.
             */
            notificationsPage =
                    loadNotificationsPage(
                            resolvedBox,
                            currentUserId,
                            safePage,
                            safeSize
                    );

        } else {

            /*
             * Pour la recherche multicritère actuelle, l'ensemble de la boîte
             * concernée est chargé puis filtré.
             *
             * Ce comportement préserve le moteur de recherche historique tout
             * en respectant strictement le nouveau cloisonnement.
             */
            List<NotificationResponseDTO> filteredNotifications =
                    fetchNotificationsForBox(
                            resolvedBox,
                            currentUserId
                    )
                            .stream()
                            .filter(notification ->
                                    matchesNotificationSearch(
                                            notification,
                                            resolvedQuery
                                    )
                            )
                            .toList();

            int start =
                    Math.min(
                            safePage * safeSize,
                            filteredNotifications.size()
                    );

            int end =
                    Math.min(
                            start + safeSize,
                            filteredNotifications.size()
                    );

            List<NotificationResponseDTO> pageContent =
                    filteredNotifications.subList(
                            start,
                            end
                    );

            notificationsPage =
                    new PageImpl<>(
                            pageContent,
                            PageRequest.of(safePage, safeSize),
                            filteredNotifications.size()
                    );
        }

        List<NotificationResponseDTO> notifications =
                notificationsPage.getContent();

        /*
         * Indicateur conservé temporairement pour compatibilité avec
         * le template actuel.
         *
         * Il signifie désormais simplement :
         * "l'utilisateur connecté est-il le destinataire de cette notification ?"
         */
        List<Boolean> currentUserNotifications =
                notifications.stream()
                        .map(notification ->
                                currentUserId != null
                                        && Objects.equals(
                                        currentUserId,
                                        notification.getIdUser()
                                )
                        )
                        .toList();

        boolean paginationEnabled =
                notificationsPage.getTotalElements()
                        > safeSize;

        // ---------------------------------------------------------------------
        // DONNÉES MÉTIER
        // ---------------------------------------------------------------------

        model.addAttribute(
                "notifications",
                notifications
        );

        model.addAttribute(
                "currentUserNotifications",
                currentUserNotifications
        );

        model.addAttribute(
                "currentUserId",
                currentUserId
        );

        // ---------------------------------------------------------------------
        // ÉTAT DE LA BOÎTE ACTIVE
        // ---------------------------------------------------------------------

        model.addAttribute(
                "box",
                resolvedBox.getRequestValue()
        );

        model.addAttribute(
                "boxLabel",
                resolvedBox.getLabel()
        );

        model.addAttribute(
                "isReceivedBox",
                resolvedBox == NotificationBox.RECEIVED
        );

        model.addAttribute(
                "isSentBox",
                resolvedBox == NotificationBox.SENT
        );

        model.addAttribute(
                "isArchivedBox",
                resolvedBox == NotificationBox.ARCHIVED
        );

        // ---------------------------------------------------------------------
        // RECHERCHE / SÉLECTION
        // ---------------------------------------------------------------------

        model.addAttribute(
                "q",
                resolvedQuery
        );

        model.addAttribute(
                "selectedNotificationId",
                selectedNotificationId
        );

        // ---------------------------------------------------------------------
        // MÉTADONNÉES DE PAGE
        // ---------------------------------------------------------------------

        model.addAttribute(
                "pageTitle",
                "Notifications"
        );

        model.addAttribute(
                "activePage",
                "admin-notifications"
        );

        // ---------------------------------------------------------------------
        // PAGINATION
        // ---------------------------------------------------------------------

        model.addAttribute(
                "currentPage",
                notificationsPage.getNumber()
        );

        model.addAttribute(
                "pageSize",
                notificationsPage.getSize()
        );

        model.addAttribute(
                "totalPages",
                notificationsPage.getTotalPages()
        );

        model.addAttribute(
                "totalElements",
                notificationsPage.getTotalElements()
        );

        model.addAttribute(
                "hasPrevious",
                notificationsPage.hasPrevious()
        );

        model.addAttribute(
                "hasNext",
                notificationsPage.hasNext()
        );

        model.addAttribute(
                "isFirst",
                notificationsPage.isFirst()
        );

        model.addAttribute(
                "isLast",
                notificationsPage.isLast()
        );

        model.addAttribute(
                "paginationEnabled",
                paginationEnabled
        );

        return "admin/notifications";
    }

    // =========================================================================
    // GET : OUVERTURE DU DÉTAIL LIÉ À UNE NOTIFICATION
    // =========================================================================

    /**
     * Ouvre la ressource ciblée par une notification.
     *
     * Dans les boîtes Reçues et Archivées, la notification appartient au
     * destinataire authentifié et peut donc être marquée comme lue.
     *
     * Dans la boîte Envoyées, le statut lu/non lu appartient au destinataire
     * réel de la notification. L'administrateur consultant l'historique ne doit
     * donc jamais modifier cet état.
     *
     * @param idNotification identifiant de la notification
     * @param box boîte active
     * @param page page courante
     * @param size taille de page
     * @param q recherche courante
     * @param selectedNotificationId sélection éventuelle
     * @param authentication contexte de sécurité
     * @param redirectAttributes attributs de redirection
     * @return redirection vers la ressource cible ou retour aux notifications
     */
    @GetMapping("/admin/notifications/{id}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public String openNotificationTarget(
            @PathVariable("id")
            Integer idNotification,
            @RequestParam(name = "box", required = false, defaultValue = "received")
            String box,
            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,
            @RequestParam(name = "q", required = false)
            String q,
            @RequestParam(name = "selectedNotificationId", required = false)
            Integer selectedNotificationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        int safePage =
                Math.max(page, 0);

        int safeSize =
                size > 0
                        ? size
                        : NOTIFICATIONS_PAGE_SIZE;

        String resolvedQuery =
                q == null
                        ? ""
                        : q.trim();

        NotificationBox resolvedBox =
                NotificationBox.fromRequestValue(box);

        User currentUser =
                resolveCurrentUser(authentication);

        Integer currentUserId =
                currentUser.getIdUser();

        /*
         * L'accès à la notification est vérifié à l'intérieur de la boîte
         * actuellement consultée.
         */
        NotificationResponseDTO notification =
                fetchNotificationsForBox(
                        resolvedBox,
                        currentUserId
                )
                        .stream()
                        .filter(item ->
                                Objects.equals(
                                        item.getIdNotification(),
                                        idNotification
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        "Notification introuvable dans cette boîte avec l'id : "
                                                + idNotification
                                )
                        );

        /*
         * L'historique Envoyées est consultatif.
         *
         * Le statut lu/non lu correspond au destinataire et ne doit donc jamais
         * être modifié par l'administrateur qui consulte l'historique.
         */
        if (resolvedBox != NotificationBox.SENT) {
            notificationService.markAsRead(
                    idNotification,
                    currentUserId
            );
        }

        String targetLink =
                notification.getTargetLinkNotification() != null
                        ? notification
                        .getTargetLinkNotification()
                        .trim()
                        : "";

        if (targetLink.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Aucun détail disponible pour cette notification."
            );

            addNavigationAttributes(
                    redirectAttributes,
                    resolvedBox,
                    safePage,
                    safeSize,
                    resolvedQuery,
                    selectedNotificationId,
                    true
            );

            return "redirect:/admin/notifications";
        }

        return buildNotificationOpenRedirect(
                notification,
                targetLink,
                resolvedBox,
                safePage,
                safeSize,
                resolvedQuery,
                selectedNotificationId
        );
    }

    // =========================================================================
    // GET JSON : SUGGESTIONS
    // =========================================================================

    /**
     * Fournit les suggestions utilisées par l'autocomplétion.
     *
     * Les suggestions sont strictement limitées à la boîte active.
     *
     * @param box boîte active
     * @param q recherche
     * @param authentication contexte de sécurité
     * @return suggestions correspondant à la recherche
     */
    @GetMapping(
            value = "/admin/notifications/suggest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationSuggestResponse>>
    suggestNotifications(
            @RequestParam(name = "box", required = false, defaultValue = "received")
            String box,
            @RequestParam(name = "q", required = false)
            String q,
            Authentication authentication
    ) {
        String normalizedQuery =
                normalizeSearchValue(q);

        if (normalizedQuery.isEmpty()) {
            return ResponseEntity.ok(
                    List.of()
            );
        }

        NotificationBox resolvedBox =
                NotificationBox.fromRequestValue(box);

        User currentUser =
                resolveCurrentUser(authentication);

        Integer currentUserId =
                currentUser.getIdUser();

        List<NotificationSuggestResponse> suggestions =
                fetchNotificationsForBox(
                        resolvedBox,
                        currentUserId
                )
                        .stream()
                        .filter(notification ->
                                matchesNotificationSearch(
                                        notification,
                                        normalizedQuery
                                )
                        )
                        .limit(NOTIFICATIONS_SUGGEST_LIMIT)
                        .map(this::toSuggestResponse)
                        .toList();

        return ResponseEntity.ok(
                suggestions
        );
    }

    // =========================================================================
    // POST : MARQUAGE COMME LUE
    // =========================================================================

    /**
     * Marque explicitement une notification reçue comme lue.
     *
     * Cette opération est interdite depuis la boîte Envoyées car l'état de
     * lecture appartient au destinataire de la notification.
     *
     * @param idNotification notification concernée
     * @param box boîte active
     * @param page page courante
     * @param size taille de page
     * @param q recherche courante
     * @param selectedNotificationId sélection éventuelle
     * @param authentication contexte de sécurité
     * @param redirectAttributes attributs de redirection
     * @return retour vers la boîte active
     */
    @PostMapping("/admin/notifications/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public String markNotificationAsRead(
            @PathVariable("id")
            Integer idNotification,
            @RequestParam(name = "box", required = false, defaultValue = "received")
            String box,
            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,
            @RequestParam(name = "q", required = false)
            String q,
            @RequestParam(name = "selectedNotificationId", required = false)
            Integer selectedNotificationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        NotificationBox resolvedBox =
                NotificationBox.fromRequestValue(box);

        User currentUser =
                resolveCurrentUser(authentication);

        /*
         * Protection fonctionnelle supplémentaire.
         *
         * Même si le service protège déjà le propriétaire, la boîte Envoyées
         * ne doit jamais proposer une mutation du statut lu/non lu.
         */
        if (resolvedBox == NotificationBox.SENT) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Le statut lu/non lu d’une notification envoyée appartient à son destinataire."
            );

            addNavigationAttributes(
                    redirectAttributes,
                    resolvedBox,
                    page,
                    size,
                    q,
                    selectedNotificationId,
                    true
            );

            return "redirect:/admin/notifications";
        }

        notificationService.markAsRead(
                idNotification,
                currentUser.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Notification marquée comme lue."
        );

        addNavigationAttributes(
                redirectAttributes,
                resolvedBox,
                page,
                size,
                q,
                selectedNotificationId,
                true
        );

        return "redirect:/admin/notifications";
    }

    // =========================================================================
    // POST : ARCHIVAGE
    // =========================================================================

    /**
     * Archive logiquement une notification appartenant à l'administrateur
     * authentifié.
     *
     * La notification disparaît alors de Reçues et devient disponible dans
     * Archivées.
     *
     * Aucune suppression physique n'est effectuée.
     *
     * @param idNotification notification à archiver
     * @param page page courante
     * @param size taille de page
     * @param q recherche courante
     * @param authentication contexte de sécurité
     * @param redirectAttributes attributs de redirection
     * @return retour vers la boîte Reçues
     */
    @PostMapping("/admin/notifications/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public String archiveNotification(
            @PathVariable("id")
            Integer idNotification,
            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,
            @RequestParam(name = "q", required = false)
            String q,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser =
                resolveCurrentUser(authentication);

        notificationService.archiveNotification(
                idNotification,
                currentUser.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Notification archivée."
        );

        /*
         * La notification venant de changer de boîte, on ne conserve surtout
         * pas selectedNotificationId : elle n'existe plus dans Reçues.
         */
        addNavigationAttributes(
                redirectAttributes,
                NotificationBox.RECEIVED,
                page,
                size,
                q,
                null,
                false
        );

        return "redirect:/admin/notifications";
    }

    // =========================================================================
    // POST : RESTAURATION
    // =========================================================================

    /**
     * Restaure une notification précédemment archivée.
     *
     * La notification quitte Archivées et retourne dans Reçues.
     *
     * @param idNotification notification à restaurer
     * @param page page courante
     * @param size taille de page
     * @param q recherche courante
     * @param authentication contexte de sécurité
     * @param redirectAttributes attributs de redirection
     * @return retour vers la boîte Archivées
     */
    @PostMapping("/admin/notifications/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public String restoreNotification(
            @PathVariable("id")
            Integer idNotification,
            @RequestParam(name = "page", required = false, defaultValue = "0")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "9")
            int size,
            @RequestParam(name = "q", required = false)
            String q,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser =
                resolveCurrentUser(authentication);

        notificationService.restoreNotification(
                idNotification,
                currentUser.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Notification restaurée dans les notifications reçues."
        );

        /*
         * La notification a quitté la boîte Archivées.
         * On ne conserve donc aucune sélection ciblée.
         */
        addNavigationAttributes(
                redirectAttributes,
                NotificationBox.ARCHIVED,
                page,
                size,
                q,
                null,
                false
        );

        return "redirect:/admin/notifications";
    }

    // =========================================================================
    // CHARGEMENT DES BOÎTES
    // =========================================================================

    /**
     * Charge une page correspondant exactement à la boîte demandée.
     *
     * @param box boîte fonctionnelle
     * @param currentUserId utilisateur authentifié
     * @param page page demandée
     * @param size taille demandée
     * @return page correspondante
     */
    private Page<NotificationResponseDTO> loadNotificationsPage(
            NotificationBox box,
            Integer currentUserId,
            int page,
            int size
    ) {
        int safePage =
                Math.max(page, 0);

        int safeSize =
                size > 0
                        ? size
                        : NOTIFICATIONS_PAGE_SIZE;

        return switch (box) {

            case RECEIVED ->
                    notificationService
                            .getReceivedNotificationsForUserPaged(
                                    currentUserId,
                                    safePage,
                                    safeSize
                            );

            case SENT ->
                    notificationService
                            .getSentNotificationsPaged(
                                    currentUserId,
                                    safePage,
                                    safeSize
                            );

            case ARCHIVED ->
                    notificationService
                            .getArchivedNotificationsForUserPaged(
                                    currentUserId,
                                    safePage,
                                    safeSize
                            );
        };
    }

    /**
     * Charge l'ensemble des notifications appartenant à une boîte.
     *
     * Cette méthode est utilisée uniquement par la recherche et les
     * suggestions afin de conserver leur fonctionnement global actuel.
     *
     * @param box boîte fonctionnelle
     * @param currentUserId utilisateur authentifié
     * @return toutes les notifications accessibles dans cette boîte
     */
    private List<NotificationResponseDTO> fetchNotificationsForBox(
            NotificationBox box,
            Integer currentUserId
    ) {
        List<NotificationResponseDTO> notifications =
                new ArrayList<>();

        int page = 0;

        Page<NotificationResponseDTO> notificationsPage;

        do {
            notificationsPage =
                    loadNotificationsPage(
                            box,
                            currentUserId,
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

    // =========================================================================
    // REDIRECTION VERS LE CONTENU D'UNE NOTIFICATION
    // =========================================================================

    /**
     * Construit la redirection adaptée au type de notification.
     *
     * Les notifications CONTACT conservent les informations nécessaires pour
     * pouvoir revenir ensuite vers le contexte Notifications.
     *
     * Le nom de la boîte active est également transmis afin que le chantier
     * Messages puisse préserver ce contexte.
     */
    private String buildNotificationOpenRedirect(
            NotificationResponseDTO notification,
            String targetLink,
            NotificationBox box,
            int notifPage,
            int notifSize,
            String notifQuery,
            Integer notifSelectedNotificationId
    ) {
        boolean isContactNotification =
                notification.getTypeNotification() != null
                        && "CONTACT".equalsIgnoreCase(
                        notification
                                .getTypeNotification()
                                .name()
                );

        if (isContactNotification) {

            String selectedContactId =
                    extractQueryParameter(
                            targetLink,
                            "selectedContactId"
                    );

            UriComponentsBuilder builder =
                    UriComponentsBuilder
                            .fromPath("/admin/messages")
                            .queryParam(
                                    "from",
                                    "notifications"
                            )
                            .queryParam(
                                    "notifBox",
                                    box.getRequestValue()
                            )
                            .queryParam(
                                    "notifPage",
                                    notifPage
                            )
                            .queryParam(
                                    "notifSize",
                                    notifSize
                            );

            if (!notifQuery.isEmpty()) {
                builder.queryParam(
                        "notifQ",
                        notifQuery
                );
            }

            if (notifSelectedNotificationId != null) {
                builder.queryParam(
                        "notifSelectedNotificationId",
                        notifSelectedNotificationId
                );
            }

            if (selectedContactId != null
                    && !selectedContactId.isBlank()) {

                builder.queryParam(
                        "selectedContactId",
                        selectedContactId.trim()
                );
            }

            return "redirect:"
                    + builder.toUriString();
        }

        return "redirect:"
                + targetLink;
    }

    /**
     * Extrait la valeur d'un paramètre présent dans une URL.
     *
     * @param url URL à examiner
     * @param parameterName paramètre recherché
     * @return valeur du paramètre ou null
     */
    private String extractQueryParameter(
            String url,
            String parameterName
    ) {
        if (url == null
                || url.isBlank()
                || parameterName == null
                || parameterName.isBlank()) {

            return null;
        }

        String token =
                parameterName + "=";

        int startIndex =
                url.indexOf(token);

        if (startIndex < 0) {
            return null;
        }

        int valueStart =
                startIndex + token.length();

        int valueEnd =
                url.indexOf(
                        '&',
                        valueStart
                );

        if (valueEnd < 0) {
            valueEnd =
                    url.length();
        }

        if (valueStart >= valueEnd) {
            return null;
        }

        return url.substring(
                valueStart,
                valueEnd
        );
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    /**
     * Réinjecte les paramètres de navigation après une action POST ou après
     * une tentative d'ouverture sans cible.
     *
     * @param redirectAttributes attributs Spring MVC
     * @param box boîte active
     * @param page page courante
     * @param size taille de page
     * @param q recherche éventuelle
     * @param selectedNotificationId sélection éventuelle
     * @param includeSelected indique si la sélection doit être conservée
     */
    private void addNavigationAttributes(
            RedirectAttributes redirectAttributes,
            NotificationBox box,
            int page,
            int size,
            String q,
            Integer selectedNotificationId,
            boolean includeSelected
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

        if (includeSelected
                && selectedNotificationId != null) {

            redirectAttributes.addAttribute(
                    "selectedNotificationId",
                    selectedNotificationId
            );
        }
    }

    // =========================================================================
    // UTILISATEUR AUTHENTIFIÉ
    // =========================================================================

    /**
     * Résout l'utilisateur authentifié à partir du contexte Spring Security.
     *
     * @param authentication contexte courant
     * @return utilisateur authentifié
     */
    private User resolveCurrentUser(
            Authentication authentication
    ) {
        String email =
                authentication != null
                        ? authentication.getName()
                        : null;

        if (email == null
                || email.isBlank()) {

            throw new UserNotFoundException(
                    "Utilisateur introuvable."
            );
        }

        return userRepository
                .findByEmailUser(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Utilisateur introuvable."
                        )
                );
    }

    // =========================================================================
    // RECHERCHE
    // =========================================================================

    /**
     * Transforme une notification en réponse simplifiée pour
     * l'autocomplétion.
     *
     * Les informations du destinataire et de l'expéditeur sont maintenant
     * également exposées afin de rendre les suggestions de la boîte Envoyées
     * réellement compréhensibles.
     */
    private NotificationSuggestResponse toSuggestResponse(
            NotificationResponseDTO notification
    ) {
        String date =
                notification.getDateNotification() != null
                        ? notification
                        .getDateNotification()
                        .format(
                                NOTIFICATION_DATE_DISPLAY_FORMATTER
                        )
                        : null;

        String recipientName =
                buildDisplayName(
                        notification.getRecipientFirstName(),
                        notification.getRecipientLastName()
                );

        String senderName =
                buildDisplayName(
                        notification.getSenderFirstName(),
                        notification.getSenderLastName()
                );

        return new NotificationSuggestResponse(
                notification.getIdNotification(),
                notification.getIdUser(),
                notification.getTitleNotification(),
                recipientName,
                notification.getRecipientEmail(),
                notification.getSentByUserId(),
                senderName,
                notification.getSenderEmail(),
                notification.getCategoryNotification() != null
                        ? notification
                        .getCategoryNotification()
                        .name()
                        : null,
                notification.getTypeNotification() != null
                        ? notification
                        .getTypeNotification()
                        .name()
                        : null,
                notification.getPriorityNotification(),
                Boolean.TRUE.equals(
                        notification.getReadNotification()
                )
                        ? "Lue"
                        : "Non lue",
                date
        );
    }

    /**
     * Vérifie si une notification correspond à la recherche.
     */
    private boolean matchesNotificationSearch(
            NotificationResponseDTO notification,
            String query
    ) {
        String normalizedQuery =
                normalizeSearchValue(query);

        String haystack =
                buildNotificationSearchHaystack(
                        notification
                );

        return !normalizedQuery.isEmpty()
                && haystack.contains(
                normalizedQuery
        );
    }

    /**
     * Construit la chaîne de recherche multicritère.
     *
     * Elle inclut désormais :
     *
     * - notification ;
     * - destinataire ;
     * - expéditeur ;
     * - contenu ;
     * - statut ;
     * - date ;
     * - archivage.
     */
    private String buildNotificationSearchHaystack(
            NotificationResponseDTO notification
    ) {
        LocalDateTime dateNotification =
                notification.getDateNotification();

        LocalDateTime archivedAt =
                notification.getArchivedAtByRecipient();

        return normalizeSearchValue(
                String.join(
                        " ",

                        // -----------------------------------------------------
                        // IDENTIFIANTS
                        // -----------------------------------------------------

                        safeValue(
                                notification.getIdNotification()
                        ),

                        safeValue(
                                notification.getIdUser()
                        ),

                        safeValue(
                                notification.getSentByUserId()
                        ),

                        // -----------------------------------------------------
                        // DESTINATAIRE
                        // -----------------------------------------------------

                        safeValue(
                                notification.getRecipientFirstName()
                        ),

                        safeValue(
                                notification.getRecipientLastName()
                        ),

                        safeValue(
                                notification.getRecipientEmail()
                        ),

                        // -----------------------------------------------------
                        // EXPÉDITEUR
                        // -----------------------------------------------------

                        safeValue(
                                notification.getSenderFirstName()
                        ),

                        safeValue(
                                notification.getSenderLastName()
                        ),

                        safeValue(
                                notification.getSenderEmail()
                        ),

                        // -----------------------------------------------------
                        // CONTENU
                        // -----------------------------------------------------

                        safeValue(
                                notification.getTitleNotification()
                        ),

                        safeValue(
                                notification.getMessageNotification()
                        ),

                        safeValue(
                                notification.getTargetLinkNotification()
                        ),

                        // -----------------------------------------------------
                        // CLASSIFICATION
                        // -----------------------------------------------------

                        notification.getCategoryNotification() != null
                                ? notification
                                .getCategoryNotification()
                                .name()
                                : "",

                        notification.getTypeNotification() != null
                                ? notification
                                .getTypeNotification()
                                .name()
                                : "",

                        safeValue(
                                notification.getPriorityNotification()
                        ),

                        // -----------------------------------------------------
                        // DATE D'ENVOI
                        // -----------------------------------------------------

                        dateNotification != null
                                ? dateNotification.format(
                                NOTIFICATION_DATE_DISPLAY_FORMATTER
                        )
                                : "",

                        dateNotification != null
                                ? dateNotification
                                .toLocalDate()
                                .toString()
                                : "",

                        // -----------------------------------------------------
                        // LECTURE
                        // -----------------------------------------------------

                        Boolean.TRUE.equals(
                                notification.getReadNotification()
                        )
                                ? "lue lu read traitee traitée"
                                : "non lue non lu unread nouvelle active",

                        // -----------------------------------------------------
                        // ARCHIVAGE
                        // -----------------------------------------------------

                        Boolean.TRUE.equals(
                                notification.getArchivedByRecipient()
                        )
                                ? "archive archivee archivée archived"
                                : "non archive non archivee non archivée",

                        archivedAt != null
                                ? archivedAt.format(
                                NOTIFICATION_DATE_DISPLAY_FORMATTER
                        )
                                : "",

                        archivedAt != null
                                ? archivedAt
                                .toLocalDate()
                                .toString()
                                : ""
                )
        );
    }

    /**
     * Construit un nom lisible à partir du prénom et du nom.
     */
    private String buildDisplayName(
            String firstName,
            String lastName
    ) {
        String safeFirstName =
                firstName != null
                        ? firstName.trim()
                        : "";

        String safeLastName =
                lastName != null
                        ? lastName.trim()
                        : "";

        if (safeFirstName.isEmpty()) {
            return safeLastName;
        }

        if (safeLastName.isEmpty()) {
            return safeFirstName;
        }

        return safeFirstName
                + " "
                + safeLastName;
    }

    /**
     * Normalise une valeur avant comparaison dans les recherches.
     */
    private String normalizeSearchValue(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    /**
     * Transforme une valeur nullable en chaîne exploitable.
     */
    private String safeValue(
            Object value
    ) {
        return value == null
                ? ""
                : String.valueOf(value);
    }

    // =========================================================================
    // ENUM INTERNE : BOÎTES DE NOTIFICATIONS
    // =========================================================================

    /**
     * Boîtes fonctionnelles accessibles depuis la page Notifications.
     *
     * L'enum reste interne au contrôleur car il représente ici uniquement
     * l'état de navigation SSR et non un nouveau concept persistant en base.
     */
    private enum NotificationBox {

        RECEIVED(
                "received",
                "Reçues"
        ),

        SENT(
                "sent",
                "Envoyées"
        ),

        ARCHIVED(
                "archived",
                "Archivées"
        );

        private final String requestValue;
        private final String label;

        NotificationBox(
                String requestValue,
                String label
        ) {
            this.requestValue =
                    requestValue;

            this.label =
                    label;
        }

        public String getRequestValue() {
            return requestValue;
        }

        public String getLabel() {
            return label;
        }

        /**
         * Résout une valeur reçue depuis l'URL.
         *
         * Une valeur absente ou inconnue revient systématiquement vers Reçues.
         */
        public static NotificationBox fromRequestValue(
                String value
        ) {
            if (value == null
                    || value.isBlank()) {

                return RECEIVED;
            }

            String normalized =
                    value.trim();

            for (NotificationBox box
                    : NotificationBox.values()) {

                if (box.requestValue.equalsIgnoreCase(
                        normalized
                )) {
                    return box;
                }
            }

            return RECEIVED;
        }
    }

    // =========================================================================
    // DTO INTERNE : SUGGESTIONS
    // =========================================================================

    /**
     * DTO interne exposé uniquement par le endpoint JSON d'autocomplétion.
     *
     * Il contient désormais suffisamment d'informations pour différencier :
     *
     * - le destinataire ;
     * - l'expéditeur ;
     * - le statut de lecture ;
     * - les métadonnées de notification.
     */
    public static final class NotificationSuggestResponse {

        private Integer idNotification;
        private Integer idUser;
        private String title;

        private String recipientName;
        private String recipientEmail;

        private Integer sentByUserId;
        private String senderName;
        private String senderEmail;

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
                String recipientName,
                String recipientEmail,
                Integer sentByUserId,
                String senderName,
                String senderEmail,
                String category,
                String type,
                String priority,
                String readStatus,
                String date
        ) {
            this.idNotification =
                    idNotification;

            this.idUser =
                    idUser;

            this.title =
                    title;

            this.recipientName =
                    recipientName;

            this.recipientEmail =
                    recipientEmail;

            this.sentByUserId =
                    sentByUserId;

            this.senderName =
                    senderName;

            this.senderEmail =
                    senderEmail;

            this.category =
                    category;

            this.type =
                    type;

            this.priority =
                    priority;

            this.readStatus =
                    readStatus;

            this.date =
                    date;
        }

        public Integer getIdNotification() {
            return idNotification;
        }

        public void setIdNotification(
                Integer idNotification
        ) {
            this.idNotification =
                    idNotification;
        }

        public Integer getIdUser() {
            return idUser;
        }

        public void setIdUser(
                Integer idUser
        ) {
            this.idUser =
                    idUser;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(
                String title
        ) {
            this.title =
                    title;
        }

        public String getRecipientName() {
            return recipientName;
        }

        public void setRecipientName(
                String recipientName
        ) {
            this.recipientName =
                    recipientName;
        }

        public String getRecipientEmail() {
            return recipientEmail;
        }

        public void setRecipientEmail(
                String recipientEmail
        ) {
            this.recipientEmail =
                    recipientEmail;
        }

        public Integer getSentByUserId() {
            return sentByUserId;
        }

        public void setSentByUserId(
                Integer sentByUserId
        ) {
            this.sentByUserId =
                    sentByUserId;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(
                String senderName
        ) {
            this.senderName =
                    senderName;
        }

        public String getSenderEmail() {
            return senderEmail;
        }

        public void setSenderEmail(
                String senderEmail
        ) {
            this.senderEmail =
                    senderEmail;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(
                String category
        ) {
            this.category =
                    category;
        }

        public String getType() {
            return type;
        }

        public void setType(
                String type
        ) {
            this.type =
                    type;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(
                String priority
        ) {
            this.priority =
                    priority;
        }

        public String getReadStatus() {
            return readStatus;
        }

        public void setReadStatus(
                String readStatus
        ) {
            this.readStatus =
                    readStatus;
        }

        public String getDate() {
            return date;
        }

        public void setDate(
                String date
        ) {
            this.date =
                    date;
        }
    }
}