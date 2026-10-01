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
 *      - récupération historique des notifications d’un utilisateur ;
 *      - récupération paginée historique ;
 *      - récupération des notifications reçues et non archivées ;
 *      - récupération des notifications archivées ;
 *      - récupération de l'historique partagé des notifications envoyées
 *        manuellement par les administrateurs ;
 *      - création d’une notification par un administrateur ;
 *      - création d’une notification système automatique ;
 *      - marquage d’une notification comme lue ;
 *      - archivage logique d'une notification par son destinataire ;
 *      - restauration d'une notification archivée.
 *
 * =============================================================================
 * RÈGLES DE CLOISONNEMENT
 * =============================================================================
 *
 * REÇUES
 * ------
 * Une notification reçue appartient exclusivement à son destinataire.
 * Seules les notifications non archivées sont exposées dans cette vue.
 *
 * ARCHIVÉES
 * ----------
 * Une notification archivée reste la propriété de son destinataire.
 * Seul celui-ci peut l'archiver ou la restaurer.
 *
 * ENVOYÉES
 * ---------
 * Une notification manuelle créée par un ADMIN mémorise désormais
 * l'administrateur expéditeur via sentByUser.
 *
 * La vue administrative "Envoyées" est un historique partagé entre les
 * administrateurs et contient toutes les notifications possédant un
 * expéditeur humain.
 *
 * L'archivage effectué par le destinataire n'a aucun effet sur cet historique.
 *
 * =============================================================================
 * SÉCURITÉ
 * =============================================================================
 *
 * - seul le propriétaire peut marquer une notification comme lue ;
 * - seul le propriétaire peut archiver ou restaurer sa notification ;
 * - seul un ADMIN peut créer une notification manuelle ;
 * - seul un ADMIN peut consulter l'historique partagé des envois ;
 * - l'expéditeur d'une notification manuelle est déterminé exclusivement
 *   côté backend à partir de l'identité du requérant ;
 * - aucune donnée fournie par le navigateur ne permet de choisir l'expéditeur.
 *
 * =============================================================================
 * GESTION DE L’ENVIRONNEMENT DEMO
 * =============================================================================
 *
 * Lorsqu’une notification est créée pendant l’exécution du profil {@code demo}
 * et que le mécanisme de réinitialisation est explicitement activé, elle reçoit
 * le marqueur :
 *
 *      RECRUITER_DEMO_CREATED_NOTIFICATIONS
 *
 * Ce marqueur permet de supprimer sélectivement les notifications temporaires
 * produites pendant les tests fonctionnels.
 *
 * Hors profil {@code demo}, ou lorsque
 * {@code magiclibrary.demo.reset.enabled=false}, aucun marqueur temporaire
 * n’est attribué.
 *
 * Les notifications réelles CLIENT conservent donc un demoScenarioCode null.
 *
 * Important :
 * les notifications canoniques du scénario recruteur ne sont pas créées
 * par cette classe pendant la reconstruction. Elles sont préparées directement
 * par le service relationnel DEMO avec leur marqueur canonique propre.
 * =============================================================================
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

    // =========================================================================
    // GET : MÉTHODES HISTORIQUES
    // =========================================================================

    /**
     * Récupère toutes les notifications associées à un utilisateur.
     *
     * Cette méthode historique ne distingue pas les notifications actives
     * des notifications archivées.
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
        User user = findUserById(
                idUser,
                "Utilisateur introuvable avec l'id : "
        );

        return notificationRepository
                .findByUserOrderByDateNotificationDesc(user)
                .stream()
                .map(NotificationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les notifications d’un utilisateur avec pagination.
     *
     * Cette méthode historique est temporairement conservée pour compatibilité.
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
        User user = findUserById(
                idUser,
                "Utilisateur introuvable avec l'id : "
        );

        Pageable pageable = buildPageable(page, size);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findByUserOrderByDateNotificationDesc(
                                user,
                                pageable
                        );

        return toResponsePage(
                notificationsPage,
                pageable
        );
    }

    /**
     * Récupère toutes les notifications avec pagination.
     *
     * Cette méthode historique est conservée temporairement mais ne doit pas
     * être utilisée pour contourner les boîtes sécurisées.
     *
     * @param page index de page demandé
     * @param size taille de page demandée
     * @return page de NotificationResponseDTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getAllNotificationsPaged(
            int page,
            int size
    ) {
        Pageable pageable = buildPageable(page, size);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findAllByOrderByDateNotificationDesc(
                                pageable
                        );

        return toResponsePage(
                notificationsPage,
                pageable
        );
    }

    // =========================================================================
    // GET : BOÎTE "REÇUES"
    // =========================================================================

    /**
     * Récupère uniquement les notifications non archivées appartenant
     * à un utilisateur.
     *
     * @param idUser identifiant du destinataire
     * @param page index de page demandé
     * @param size taille de page demandée
     * @return page des notifications reçues et actives
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getReceivedNotificationsForUserPaged(
            Integer idUser,
            int page,
            int size
    ) {
        User user = findUserById(
                idUser,
                "Utilisateur introuvable avec l'id : "
        );

        Pageable pageable = buildPageable(page, size);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findByUserAndArchivedByRecipientFalseOrderByDateNotificationDesc(
                                user,
                                pageable
                        );

        return toResponsePage(
                notificationsPage,
                pageable
        );
    }

    // =========================================================================
    // GET : BOÎTE "ARCHIVÉES"
    // =========================================================================

    /**
     * Récupère uniquement les notifications archivées appartenant
     * à un utilisateur.
     *
     * @param idUser identifiant du destinataire
     * @param page index de page demandé
     * @param size taille de page demandée
     * @return page des notifications archivées
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getArchivedNotificationsForUserPaged(
            Integer idUser,
            int page,
            int size
    ) {
        User user = findUserById(
                idUser,
                "Utilisateur introuvable avec l'id : "
        );

        Pageable pageable = buildPageable(page, size);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findByUserAndArchivedByRecipientTrueOrderByDateNotificationDesc(
                                user,
                                pageable
                        );

        return toResponsePage(
                notificationsPage,
                pageable
        );
    }

    // =========================================================================
    // GET : BOÎTE ADMIN "ENVOYÉES"
    // =========================================================================

    /**
     * Récupère l'historique partagé des notifications manuelles
     * envoyées par les administrateurs.
     *
     * Le requérant doit lui-même être ADMIN.
     *
     * L'historique ne dépend pas de l'état d'archivage du destinataire.
     *
     * @param idRequester identifiant de l'administrateur consultant
     * @param page index de page demandé
     * @param size taille de page demandée
     * @return page des notifications envoyées manuellement
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getSentNotificationsPaged(
            Integer idRequester,
            int page,
            int size
    ) {
        User requester = findRequester(idRequester);

        if (!isAdmin(requester)) {
            throw new ForbiddenException(
                    "Accès interdit : ADMIN uniquement."
            );
        }

        Pageable pageable = buildPageable(page, size);

        Page<Notification> notificationsPage =
                notificationRepository
                        .findBySentByUserIsNotNullOrderByDateNotificationDesc(
                                pageable
                        );

        return toResponsePage(
                notificationsPage,
                pageable
        );
    }

    // =========================================================================
    // POST : CRÉATION D’UNE NOTIFICATION PAR UN ADMIN
    // =========================================================================

    /**
     * Crée une notification manuelle pour un utilisateur cible.
     *
     * Règles métier :
     * - seul un ADMIN peut créer une notification manuelle ;
     * - l’utilisateur cible doit exister ;
     * - l'expéditeur est déterminé exclusivement côté backend ;
     * - dateNotification et readNotification sont gérées côté backend ;
     * - toute nouvelle notification est initialement non archivée ;
     * - en environnement DEMO, la notification reçoit le marqueur temporaire.
     *
     * @param requestDTO DTO de création
     * @param idRequester identifiant de l’administrateur requérant
     * @return notification créée
     */
    @Override
    public NotificationResponseDTO createNotification(
            NotificationRequestDTO requestDTO,
            Integer idRequester
    ) {
        validateNotificationRequest(requestDTO);

        User requester = findRequester(idRequester);

        if (!isAdmin(requester)) {
            throw new ForbiddenException(
                    "Accès interdit : ADMIN uniquement."
            );
        }

        User target =
                findTargetUser(
                        requestDTO.getIdUser()
                );

        Notification notification =
                NotificationMapper.toEntity(
                        requestDTO,
                        target
                );

        /*
         * L'expéditeur est déterminé exclusivement depuis l'utilisateur
         * requérant déjà contrôlé côté backend.
         *
         * Aucun champ du formulaire ni du NotificationRequestDTO
         * ne permet de choisir ou de falsifier cette identité.
         */
        notification.setSentByUser(requester);

        initializeSystemFields(notification);
        applyDemoMarkerIfRequired(notification);

        Notification saved =
                notificationRepository.save(
                        notification
                );

        return NotificationMapper.toResponseDTO(
                saved
        );
    }

    // =========================================================================
    // POST : CRÉATION D’UNE NOTIFICATION SYSTÈME AUTOMATIQUE
    // =========================================================================

    /**
     * Crée une notification système automatique.
     *
     * Une notification système ne possède pas d'expéditeur humain.
     *
     * @param requestDTO DTO de création contenant l’id utilisateur cible
     * @return notification créée
     */
    @Override
    public NotificationResponseDTO createSystemNotification(
            NotificationRequestDTO requestDTO
    ) {
        validateNotificationRequest(requestDTO);

        User target =
                findTargetUser(
                        requestDTO.getIdUser()
                );

        Notification notification =
                NotificationMapper.toEntity(
                        requestDTO,
                        target
                );

        /*
         * Une notification système automatique n'est attribuée
         * artificiellement à aucun utilisateur humain.
         */
        notification.setSentByUser(null);

        initializeSystemFields(notification);
        applyDemoMarkerIfRequired(notification);

        Notification saved =
                notificationRepository.save(
                        notification
                );

        return NotificationMapper.toResponseDTO(
                saved
        );
    }

    // =========================================================================
    // PUT : MARQUER UNE NOTIFICATION COMME LUE
    // =========================================================================

    /**
     * Marque une notification comme lue.
     *
     * Seul le destinataire propriétaire peut effectuer cette action.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant de l’utilisateur effectuant l’action
     * @return notification mise à jour
     */
    @Override
    public NotificationResponseDTO markAsRead(
            Integer idNotification,
            Integer idRequester
    ) {
        Notification notification =
                findNotificationById(
                        idNotification
                );

        User requester =
                findRequester(
                        idRequester
                );

        assertNotificationOwner(
                notification,
                requester
        );

        notification.setReadNotification(
                true
        );

        Notification saved =
                notificationRepository.save(
                        notification
                );

        return NotificationMapper.toResponseDTO(
                saved
        );
    }

    // =========================================================================
    // PUT : ARCHIVER UNE NOTIFICATION
    // =========================================================================

    /**
     * Archive logiquement une notification pour son destinataire.
     *
     * L'opération ne supprime aucune donnée.
     *
     * Une notification archivée :
     * - disparaît de Reçues ;
     * - apparaît dans Archivées ;
     * - reste présente dans l'historique Envoyées si elle possède un expéditeur.
     *
     * Seul le destinataire propriétaire peut effectuer l'action.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant du destinataire
     * @return notification archivée
     */
    @Override
    public NotificationResponseDTO archiveNotification(
            Integer idNotification,
            Integer idRequester
    ) {
        Notification notification =
                findNotificationById(
                        idNotification
                );

        User requester =
                findRequester(
                        idRequester
                );

        assertNotificationOwner(
                notification,
                requester
        );

        if (Boolean.TRUE.equals(
                notification.getArchivedByRecipient()
        )) {
            throw new IllegalStateException(
                    "Cette notification est déjà archivée."
            );
        }

        notification.setArchivedByRecipient(
                true
        );

        notification.setArchivedAtByRecipient(
                LocalDateTime.now()
        );

        Notification saved =
                notificationRepository.save(
                        notification
                );

        return NotificationMapper.toResponseDTO(
                saved
        );
    }

    // =========================================================================
    // PUT : RESTAURER UNE NOTIFICATION ARCHIVÉE
    // =========================================================================

    /**
     * Restaure une notification précédemment archivée.
     *
     * Seul le destinataire propriétaire peut effectuer l'action.
     *
     * @param idNotification identifiant de la notification
     * @param idRequester identifiant du destinataire
     * @return notification restaurée
     */
    @Override
    public NotificationResponseDTO restoreNotification(
            Integer idNotification,
            Integer idRequester
    ) {
        Notification notification =
                findNotificationById(
                        idNotification
                );

        User requester =
                findRequester(
                        idRequester
                );

        assertNotificationOwner(
                notification,
                requester
        );

        if (!Boolean.TRUE.equals(
                notification.getArchivedByRecipient()
        )) {
            throw new IllegalStateException(
                    "Cette notification n'est pas archivée."
            );
        }

        notification.setArchivedByRecipient(
                false
        );

        notification.setArchivedAtByRecipient(
                null
        );

        Notification saved =
                notificationRepository.save(
                        notification
                );

        return NotificationMapper.toResponseDTO(
                saved
        );
    }

    // =========================================================================
    // OUTILS : VALIDATION ET RÉSOLUTION DES ENTITÉS
    // =========================================================================

    /**
     * Vérifie qu’une demande de création contient un utilisateur cible.
     *
     * @param requestDTO demande à contrôler
     */
    private void validateNotificationRequest(
            NotificationRequestDTO requestDTO
    ) {
        if (requestDTO == null
                || requestDTO.getIdUser() == null) {
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
    private User findTargetUser(
            Integer idUser
    ) {
        return userRepository
                .findById(idUser)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Utilisateur cible introuvable avec l'id : "
                                        + idUser
                        )
                );
    }

    /**
     * Résout un utilisateur requérant.
     *
     * @param idRequester identifiant du requérant
     * @return utilisateur requérant
     */
    private User findRequester(
            Integer idRequester
    ) {
        if (idRequester == null) {
            throw new UserNotFoundException(
                    "Utilisateur requérant introuvable."
            );
        }

        return userRepository
                .findById(idRequester)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Utilisateur requérant introuvable avec l'id : "
                                        + idRequester
                        )
                );
    }

    /**
     * Résout un utilisateur générique par identifiant.
     *
     * @param idUser identifiant utilisateur
     * @param errorPrefix préfixe du message d'erreur
     * @return utilisateur
     */
    private User findUserById(
            Integer idUser,
            String errorPrefix
    ) {
        if (idUser == null) {
            throw new UserNotFoundException(
                    errorPrefix + "null"
            );
        }

        return userRepository
                .findById(idUser)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                errorPrefix + idUser
                        )
                );
    }

    /**
     * Résout une notification par identifiant.
     *
     * @param idNotification identifiant notification
     * @return notification persistée
     */
    private Notification findNotificationById(
            Integer idNotification
    ) {
        if (idNotification == null) {
            throw new NotificationNotFoundException(
                    "Notification introuvable."
            );
        }

        return notificationRepository
                .findById(idNotification)
                .orElseThrow(() ->
                        new NotificationNotFoundException(
                                "Notification introuvable avec l'id : "
                                        + idNotification
                        )
                );
    }

    /**
     * Vérifie que le requérant est bien le destinataire propriétaire
     * de la notification.
     *
     * Cette protection est utilisée pour :
     * - le marquage comme lu ;
     * - l'archivage ;
     * - la restauration.
     *
     * @param notification notification concernée
     * @param requester utilisateur effectuant l'action
     */
    private void assertNotificationOwner(
            Notification notification,
            User requester
    ) {
        Integer ownerId =
                notification != null
                        && notification.getUser() != null
                        ? notification
                        .getUser()
                        .getIdUser()
                        : null;

        Integer requesterId =
                requester != null
                        ? requester.getIdUser()
                        : null;

        if (ownerId == null) {
            throw new IllegalStateException(
                    "Notification invalide : aucun propriétaire associé."
            );
        }

        if (requesterId == null
                || !ownerId.equals(requesterId)) {
            throw new ForbiddenException(
                    "Accès interdit à cette notification."
            );
        }
    }

    // =========================================================================
    // OUTILS : PAGINATION ET MAPPING
    // =========================================================================

    /**
     * Normalise les paramètres de pagination.
     *
     * @param page index demandé
     * @param size taille demandée
     * @return Pageable sécurisé
     */
    private Pageable buildPageable(
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
                        : 9;

        return PageRequest.of(
                safePage,
                safeSize
        );
    }

    /**
     * Convertit une page d'entités Notification en page de DTO.
     *
     * @param notificationsPage page JPA source
     * @param pageable pagination appliquée
     * @return page de DTO
     */
    private Page<NotificationResponseDTO> toResponsePage(
            Page<Notification> notificationsPage,
            Pageable pageable
    ) {
        List<NotificationResponseDTO> content =
                notificationsPage
                        .getContent()
                        .stream()
                        .map(NotificationMapper::toResponseDTO)
                        .collect(Collectors.toList());

        return new PageImpl<>(
                content,
                pageable,
                notificationsPage.getTotalElements()
        );
    }

    // =========================================================================
    // OUTILS : INITIALISATION DES CHAMPS SYSTÈME
    // =========================================================================

    /**
     * Initialise les champs système obligatoires d’une nouvelle notification.
     *
     * Toute nouvelle notification :
     * - est non lue ;
     * - est non archivée ;
     * - ne possède aucune date d'archivage.
     *
     * @param notification notification en cours de création
     */
    private void initializeSystemFields(
            Notification notification
    ) {
        notification.setDateNotification(
                LocalDateTime.now()
        );

        notification.setReadNotification(
                false
        );

        notification.setArchivedByRecipient(
                false
        );

        notification.setArchivedAtByRecipient(
                null
        );
    }

    /**
     * Détermine si un utilisateur possède le rôle ADMIN.
     *
     * @param user utilisateur à contrôler
     * @return true si l’utilisateur est administrateur
     */
    private boolean isAdmin(
            User user
    ) {
        return user != null
                && user.getRole() != null
                && user.getRole().getLabelRole() != null
                && user.getRole()
                .getLabelRole()
                .equalsIgnoreCase("ADMIN");
    }

    // =========================================================================
    // OUTIL : MARQUEUR DEMO
    // =========================================================================

    /**
     * Attribue le marqueur temporaire officiel à une notification créée
     * pendant l’utilisation fonctionnelle de la DEMO.
     *
     * Les deux protections doivent être réunies :
     *
     * - le profil Spring {@code demo} est actif ;
     * - la propriété {@code magiclibrary.demo.reset.enabled} vaut true.
     *
     * En dehors de cette configuration, demoScenarioCode reste inchangé
     * et normalement null.
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
                environment.acceptsProfiles(
                        Profiles.of("demo")
                );

        if (!demoProfileActive
                || !demoResetEnabled) {
            return;
        }

        notification.setDemoScenarioCode(
                DemoScenarioCodes
                        .RECRUITER_DEMO_CREATED_NOTIFICATIONS
        );
    }
}