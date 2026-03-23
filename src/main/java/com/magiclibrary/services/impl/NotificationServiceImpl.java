package com.magiclibrary.services.impl;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
// Gestion des dates/temps et collections
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
// Déclaration du service Spring et gestion transactionnelle
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// DTO pour notifications
import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.notification.NotificationResponseDTO;
// Entités métier
import com.magiclibrary.entities.Notification;
import com.magiclibrary.entities.User;
// Exceptions métier
import com.magiclibrary.exceptions.custom.ForbiddenException;
import com.magiclibrary.exceptions.custom.NotificationNotFoundException;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
// Mapper pour conversion entité ↔ DTO
import com.magiclibrary.mappers.NotificationMapper;
// Repositories JPA
import com.magiclibrary.repositories.interfaces.NotificationRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
// Interface service
import com.magiclibrary.services.NotificationService;

/**
 * =============================================================================
 * SERVICE IMPLEMENTATION : NotificationServiceImpl
 * =============================================================================
 * Implémente les opérations métier pour la gestion des notifications.
 *
 * Rôle :
 *      - récupération des notifications d’un utilisateur
 *      - récupération paginée des notifications d’un utilisateur
 *      - récupération paginée de toutes les notifications pour l’administration
 *      - création d’une notification par un Administrateur
 *      - création d’une notification système automatique
 *      - marquage d’une notification comme lue
 *
 * Règles métier :
 *      - seul le propriétaire ou un ADMIN peut marquer une notification comme lue
 *      - les dates et flags système sont générés côté backend
 *      - cohérence entre notification et utilisateur
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Constructeur avec injection des dépendances nécessaires pour gérer les notifications.
     *
     * @param notificationRepository repository JPA pour les notifications
     * @param userRepository repository JPA pour les utilisateurs
     */
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // GET : Récupération des notifications d’un utilisateur
    // -------------------------------------------------------------------------

    /**
     * Récupère toutes les notifications associées à un utilisateur.
     *
     * @param idUser identifiant de l’utilisateur
     * @return liste de NotificationResponseDTO triée par date décroissante
     * @throws UserNotFoundException si l’utilisateur n’existe pas
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotificationsForUser(Integer idUser) {

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'id : " + idUser));

        return notificationRepository
                .findByUserOrderByDateNotificationDesc(user)
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les notifications d’un utilisateur avec pagination.
     *
     * @param idUser identifiant de l’utilisateur
     * @param page index de page demandé
     * @param size taille de page demandée
     * @return page de NotificationResponseDTO triée par date décroissante
     * @throws UserNotFoundException si l’utilisateur n’existe pas
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getNotificationsForUserPaged(Integer idUser, int page, int size) {

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'id : " + idUser));

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Notification> notificationsPage =
                notificationRepository.findByUserOrderByDateNotificationDesc(user, pageable);

        List<NotificationResponseDTO> content = notificationsPage.getContent()
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, notificationsPage.getTotalElements());
    }

    /**
     * Récupère toutes les notifications avec pagination pour l’administration.
     *
     * @param page index de page demandé
     * @param size taille de page demandée
     * @return page de NotificationResponseDTO triée par date décroissante
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getAllNotificationsPaged(int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Notification> notificationsPage =
                notificationRepository.findAllByOrderByDateNotificationDesc(pageable);

        List<NotificationResponseDTO> content = notificationsPage.getContent()
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, notificationsPage.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // POST : Création d’une notification (ADMIN uniquement)
    // -------------------------------------------------------------------------

    /**
     * Crée une notification pour un utilisateur cible.
     *
     * Règles métier :
     *      - seul un ADMIN peut créer une notification
     *      - l’utilisateur cible doit exister
     *      - dateNotification et readNotification sont gérées côté backend
     *
     * @param requestDTO DTO de création contenant l’id de l’utilisateur cible
     * @param idRequester identifiant de l’utilisateur requérant (doit être ADMIN)
     * @return NotificationResponseDTO représentant la notification créée
     * @throws IllegalArgumentException si le DTO est invalide
     * @throws UserNotFoundException si utilisateur requérant ou cible introuvable
     * @throws ForbiddenException si l’utilisateur requérant n’est pas ADMIN
     */
    @Override
    public NotificationResponseDTO createNotification(NotificationRequestDTO requestDTO, Integer idRequester) {

        if (requestDTO == null || requestDTO.getIdUser() == null) {
            throw new IllegalArgumentException("L'identifiant utilisateur cible est obligatoire.");
        }

        User requester = userRepository.findById(idRequester)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur requérant introuvable avec l'id : " + idRequester));

        boolean isAdmin = requester.getRole() != null
                && requester.getRole().getLabelRole() != null
                && requester.getRole().getLabelRole().equalsIgnoreCase("ADMIN");

        if (!isAdmin) {
            throw new ForbiddenException("Accès interdit : ADMIN uniquement.");
        }

        User target = userRepository.findById(requestDTO.getIdUser())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur cible introuvable avec l'id : " + requestDTO.getIdUser()));

        Notification notification = NotificationMapper.toEntity(requestDTO, target);

        // Champs système : date et read flag
        notification.setDateNotification(LocalDateTime.now());
        notification.setReadNotification(false);

        Notification saved = notificationRepository.save(notification);

        return NotificationMapper.toResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // POST : Création d’une notification système automatique
    // -------------------------------------------------------------------------

    /**
     * Crée une notification système automatique pour un utilisateur cible.
     *
     * Règles métier :
     *      - aucun requérant ADMIN n’est exigé
     *      - l’utilisateur cible doit exister
     *      - dateNotification et readNotification sont gérées côté backend
     *
     * @param requestDTO DTO de création contenant l’id de l’utilisateur cible
     * @return NotificationResponseDTO représentant la notification créée
     * @throws IllegalArgumentException si le DTO est invalide
     * @throws UserNotFoundException si l’utilisateur cible est introuvable
     */
    @Override
    public NotificationResponseDTO createSystemNotification(NotificationRequestDTO requestDTO) {

        if (requestDTO == null || requestDTO.getIdUser() == null) {
            throw new IllegalArgumentException("L'identifiant utilisateur cible est obligatoire.");
        }

        User target = userRepository.findById(requestDTO.getIdUser())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur cible introuvable avec l'id : " + requestDTO.getIdUser()));

        Notification notification = NotificationMapper.toEntity(requestDTO, target);

        // Champs système : date et read flag
        notification.setDateNotification(LocalDateTime.now());
        notification.setReadNotification(false);

        Notification saved = notificationRepository.save(notification);

        return NotificationMapper.toResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // PUT : Marquer une notification comme lue
    // -------------------------------------------------------------------------

    /**
     * Marque une notification comme lue.
     *
     * Règles métier :
     *      - seul le propriétaire ou un ADMIN peut effectuer l’action
     *      - la date de création de la notification n’est pas modifiée
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant de l’utilisateur effectuant l’action
     * @return NotificationResponseDTO représentant la notification mise à jour
     * @throws NotificationNotFoundException si la notification n’existe pas
     * @throws UserNotFoundException si l’utilisateur requérant n’existe pas
     * @throws ForbiddenException si l’utilisateur n’est ni propriétaire ni ADMIN
     */
    @Override
    public NotificationResponseDTO markAsRead(Integer idNotification, Integer idRequester) {

        Notification notif = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new NotificationNotFoundException("Notification introuvable avec l'id : " + idNotification));

        User requester = userRepository.findById(idRequester)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur requérant introuvable avec l'id : " + idRequester));

        boolean isAdmin = requester.getRole() != null
                && requester.getRole().getLabelRole() != null
                && requester.getRole().getLabelRole().equalsIgnoreCase("ADMIN");

        Integer ownerId = (notif.getUser() != null) ? notif.getUser().getIdUser() : null;

        if (ownerId == null) {
            throw new IllegalStateException("Notification invalide : aucun propriétaire associé.");
        }

        if (!ownerId.equals(idRequester) && !isAdmin) {
            throw new ForbiddenException("Accès interdit à cette notification.");
        }

        notif.setReadNotification(true);

        Notification saved = notificationRepository.save(notif);

        return NotificationMapper.toResponseDTO(saved);
    }
}