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
// Configuration, environnement, déclaration du service et transactions
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
// Constantes officielles des scénarios DEMO
import com.magiclibrary.init.DemoScenarioCodes;
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
 *
 * Implémente les opérations métier pour la gestion des notifications.
 *
 * Rôle :
 *      - récupération des notifications d’un utilisateur ;
 *      - récupération paginée des notifications d’un utilisateur ;
 *      - récupération paginée de toutes les notifications pour
 *        l’administration ;
 *      - création d’une notification par un administrateur ;
 *      - création d’une notification système automatique ;
 *      - marquage d’une notification comme lue.
 *
 * Règles métier :
 *      - seul le propriétaire ou un ADMIN peut marquer une notification comme
 *        lue ;
 *      - les dates et indicateurs système sont générés côté backend ;
 *      - l’utilisateur destinataire doit exister ;
 *      - les contrôles de rôle sont effectués côté service.
 *
 * Gestion de l’environnement DEMO :
 *      Lorsqu’une notification est créée pendant l’exécution du profil
 *      {@code demo} et que le mécanisme de réinitialisation est explicitement
 *      activé, elle reçoit le marqueur :
 *
 *          RECRUITER_DEMO_CREATED_NOTIFICATIONS
 *
 *      Ce marqueur permet de supprimer sélectivement les notifications
 *      temporaires produites pendant les tests fonctionnels, notamment :
 *          - les notifications générées lors de l’envoi d’un message Contact ;
 *          - les notifications générées après une réponse administrateur ;
 *          - les notifications créées manuellement par un administrateur ;
 *          - les autres notifications système produites pendant une session
 *            de démonstration.
 *
 * Sécurité CLIENT :
 *      Hors profil {@code demo}, ou lorsque
 *      {@code magiclibrary.demo.reset.enabled=false}, aucun marqueur temporaire
 *      n’est attribué. Les notifications de la future instance CLIENT
 *      conservent donc un {@code demoScenarioCode} null et ne peuvent pas être
 *      ciblées par le reset DEMO.
 *
 * Important :
 *      Les notifications canoniques du scénario recruteur ne sont pas créées
 *      par cette classe pendant la reconstruction. Elles sont préparées
 *      directement par le service relationnel DEMO avec leur marqueur
 *      canonique propre.
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Environment environment;
    private final boolean demoResetEnabled;

    /**
     * Constructeur avec injection des dépendances nécessaires pour gérer les
     * notifications et identifier de manière sécurisée l’environnement DEMO.
     *
     * @param notificationRepository repository JPA pour les notifications
     * @param userRepository repository JPA pour les utilisateurs
     * @param environment environnement Spring actif
     * @param demoResetEnabled indique si le mécanisme de reset DEMO est activé
     */
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            Environment environment,
            @Value("${magiclibrary.demo.reset.enabled:false}")
            boolean demoResetEnabled
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.environment = environment;
        this.demoResetEnabled = demoResetEnabled;
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
    public List<NotificationResponseDTO> getNotificationsForUser(
            Integer idUser
    ) {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable avec l'id : " + idUser
                ));

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
    public Page<NotificationResponseDTO> getNotificationsForUserPaged(
            Integer idUser,
            int page,
            int size
    ) {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable avec l'id : " + idUser
                ));

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findByUserOrderByDateNotificationDesc(
                                user,
                                pageable
                        );

        List<NotificationResponseDTO> content =
                notificationsPage.getContent()
                        .stream()
                        .map(NotificationMapper::toResponseDTO)
                        .collect(Collectors.toList());

        return new PageImpl<>(
                content,
                pageable,
                notificationsPage.getTotalElements()
        );
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
    public Page<NotificationResponseDTO> getAllNotificationsPaged(
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findAllByOrderByDateNotificationDesc(pageable);

        List<NotificationResponseDTO> content =
                notificationsPage.getContent()
                        .stream()
                        .map(NotificationMapper::toResponseDTO)
                        .collect(Collectors.toList());

        return new PageImpl<>(
                content,
                pageable,
                notificationsPage.getTotalElements()
        );
    }

    // -------------------------------------------------------------------------
    // POST : Création d’une notification par un ADMIN
    // -------------------------------------------------------------------------

    /**
     * Crée une notification pour un utilisateur cible.
     *
     * Règles métier :
     *      - seul un ADMIN peut créer une notification ;
     *      - l’utilisateur cible doit exister ;
     *      - dateNotification et readNotification sont gérées côté backend ;
     *      - en environnement DEMO, la notification reçoit le marqueur
     *        temporaire officiel.
     *
     * @param requestDTO DTO de création contenant l’id de l’utilisateur cible
     * @param idRequester identifiant de l’utilisateur requérant
     * @return notification créée
     * @throws IllegalArgumentException si le DTO est invalide
     * @throws UserNotFoundException si le requérant ou la cible est introuvable
     * @throws ForbiddenException si le requérant n’est pas ADMIN
     */
    @Override
    public NotificationResponseDTO createNotification(
            NotificationRequestDTO requestDTO,
            Integer idRequester
    ) {
        validateNotificationRequest(requestDTO);

        User requester = userRepository.findById(idRequester)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur requérant introuvable avec l'id : "
                                + idRequester
                ));

        if (!isAdmin(requester)) {
            throw new ForbiddenException(
                    "Accès interdit : ADMIN uniquement."
            );
        }

        User target = findTargetUser(requestDTO.getIdUser());

        Notification notification =
                NotificationMapper.toEntity(requestDTO, target);

        initializeSystemFields(notification);
        applyDemoMarkerIfRequired(notification);

        Notification saved =
                notificationRepository.save(notification);

        return NotificationMapper.toResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // POST : Création d’une notification système automatique
    // -------------------------------------------------------------------------

    /**
     * Crée une notification système automatique pour un utilisateur cible.
     *
     * Règles métier :
     *      - aucun requérant ADMIN n’est exigé ;
     *      - l’utilisateur cible doit exister ;
     *      - dateNotification et readNotification sont gérées côté backend ;
     *      - en environnement DEMO, la notification reçoit le marqueur
     *        temporaire officiel.
     *
     * @param requestDTO DTO de création contenant l’id de l’utilisateur cible
     * @return notification créée
     * @throws IllegalArgumentException si le DTO est invalide
     * @throws UserNotFoundException si l’utilisateur cible est introuvable
     */
    @Override
    public NotificationResponseDTO createSystemNotification(
            NotificationRequestDTO requestDTO
    ) {
        validateNotificationRequest(requestDTO);

        User target = findTargetUser(requestDTO.getIdUser());

        Notification notification =
                NotificationMapper.toEntity(requestDTO, target);

        initializeSystemFields(notification);
        applyDemoMarkerIfRequired(notification);

        Notification saved =
                notificationRepository.save(notification);

        return NotificationMapper.toResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // PUT : Marquer une notification comme lue
    // -------------------------------------------------------------------------

    /**
     * Marque une notification comme lue.
     *
     * Règles métier :
     *      - seul le propriétaire ou un ADMIN peut effectuer l’action ;
     *      - la date de création n’est pas modifiée ;
     *      - le marqueur DEMO éventuellement présent est conservé.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant de l’utilisateur effectuant l’action
     * @return notification mise à jour
     * @throws NotificationNotFoundException si la notification n’existe pas
     * @throws UserNotFoundException si le requérant n’existe pas
     * @throws ForbiddenException si le requérant n’est ni propriétaire ni ADMIN
     */
    @Override
    public NotificationResponseDTO markAsRead(
            Integer idNotification,
            Integer idRequester
    ) {
        Notification notification =
                notificationRepository.findById(idNotification)
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        "Notification introuvable avec l'id : "
                                                + idNotification
                                )
                        );

        User requester = userRepository.findById(idRequester)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur requérant introuvable avec l'id : "
                                + idRequester
                ));

        Integer ownerId = notification.getUser() != null
                ? notification.getUser().getIdUser()
                : null;

        if (ownerId == null) {
            throw new IllegalStateException(
                    "Notification invalide : aucun propriétaire associé."
            );
        }

        if (!ownerId.equals(idRequester) && !isAdmin(requester)) {
            throw new ForbiddenException(
                    "Accès interdit à cette notification."
            );
        }

        notification.setReadNotification(true);

        Notification saved =
                notificationRepository.save(notification);

        return NotificationMapper.toResponseDTO(saved);
    }

    // -------------------------------------------------------------------------
    // OUTILS DE VALIDATION ET DE CRÉATION
    // -------------------------------------------------------------------------

    /**
     * Vérifie qu’une demande de création contient un utilisateur cible.
     *
     * @param requestDTO demande à contrôler
     */
    private void validateNotificationRequest(
            NotificationRequestDTO requestDTO
    ) {
        if (requestDTO == null || requestDTO.getIdUser() == null) {
            throw new IllegalArgumentException(
                    "L'identifiant utilisateur cible est obligatoire."
            );
        }
    }

    /**
     * Résout l’utilisateur destinataire d’une notification.
     *
     * @param idUser identifiant de la cible
     * @return utilisateur cible
     */
    private User findTargetUser(Integer idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur cible introuvable avec l'id : "
                                + idUser
                ));
    }

    /**
     * Initialise les champs système obligatoires d’une notification.
     *
     * @param notification notification en cours de création
     */
    private void initializeSystemFields(Notification notification) {
        notification.setDateNotification(LocalDateTime.now());
        notification.setReadNotification(false);
    }

    /**
     * Détermine si un utilisateur possède le rôle ADMIN.
     *
     * @param user utilisateur à contrôler
     * @return true si l’utilisateur est administrateur
     */
    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getLabelRole() != null
                && user.getRole()
                .getLabelRole()
                .equalsIgnoreCase("ADMIN");
    }

    /**
     * Attribue le marqueur temporaire officiel à une notification créée
     * pendant l’utilisation fonctionnelle de la DEMO.
     *
     * <p>Les deux protections doivent être réunies :</p>
     *
     * <ul>
     *     <li>le profil Spring {@code demo} est actif ;</li>
     *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
     *     {@code true}.</li>
     * </ul>
     *
     * <p>En dehors de cette configuration, le champ
     * {@code demoScenarioCode} reste inchangé et normalement null.</p>
     *
     * @param notification notification en cours de création
     */
    private void applyDemoMarkerIfRequired(
            Notification notification
    ) {
        if (notification == null) {
            return;
        }

        boolean demoProfileActive =
                environment.acceptsProfiles(Profiles.of("demo"));

        if (!demoProfileActive || !demoResetEnabled) {
            return;
        }

        notification.setDemoScenarioCode(
                DemoScenarioCodes
                        .RECRUITER_DEMO_CREATED_NOTIFICATIONS
        );
    }
}