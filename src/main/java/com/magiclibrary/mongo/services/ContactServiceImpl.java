package com.magiclibrary.mongo.services;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ContactStatus;
import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import com.magiclibrary.exceptions.custom.ContactAlreadyAnsweredException;
import com.magiclibrary.exceptions.custom.ForbiddenException;
import com.magiclibrary.init.DemoScenarioCodes;
import com.magiclibrary.mongo.documents.ContactDocument;
import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.repositories.ContactMongoRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.NotificationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * SERVICE MONGODB : ContactServiceImpl
 * =============================================================================
 *
 * Implémentation du service de gestion des messages de contact.
 *
 * Responsabilités :
 *      - création d'un message de contact pour l'utilisateur authentifié ;
 *      - consultation globale des messages côté administration ;
 *      - consultation limitée aux messages du membre connecté ;
 *      - enregistrement des réponses administrateur ;
 *      - génération des notifications associées ;
 *      - archivage logique indépendant MEMBRE / ADMIN ;
 *      - restauration des messages archivés ;
 *      - conversion des documents MongoDB en DTO d'affichage.
 *
 * ARCHIVAGE :
 *
 * Le même document MongoDB possède deux états d'archivage indépendants :
 *
 *      archivedByMember
 *          -> contrôle uniquement la visibilité dans l'espace du membre.
 *
 *      archivedByAdmin
 *          -> contrôle uniquement la visibilité dans l'espace administratif.
 *
 * Ainsi :
 *
 *      - un membre peut archiver un message sans le masquer aux admins ;
 *      - un admin peut archiver un message sans le masquer au membre ;
 *      - aucun archivage ne supprime le document ;
 *      - NEW / ANSWERED reste totalement indépendant de l'archivage.
 *
 * Gestion de l'environnement DEMO :
 *
 * Lorsqu'un message est créé pendant l'exécution du profil Spring
 * {@code demo} et que le mécanisme de réinitialisation est explicitement
 * activé, le document reçoit le marqueur :
 *
 *      RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
 *
 * Ce marqueur permet au mécanisme de reset de supprimer uniquement les
 * messages temporaires créés pendant les tests.
 *
 * Sécurité CLIENT :
 *
 * Hors profil {@code demo}, ou lorsque la propriété de reset est désactivée,
 * aucun marqueur temporaire n'est attribué.
 */
@Service
public class ContactServiceImpl implements ContactService {

    private static final String ANSWERED_BADGE_CLASS =
            " bg-gray-50 text-gray-700 ring-gray-200";

    private static final String NEW_BADGE_CLASS =
            " bg-blue-50 text-blue-900 ring-blue-200";

    private static final String MEMBER_ROLE_LABEL = "Membre";
    private static final String ADMIN_ROLE_LABEL = "Admin";
    private static final String UNKNOWN_ROLE_LABEL = "Rôle inconnu";
    private static final String UNKNOWN_ADMIN_LABEL = "Admin introuvable";

    private final ContactMongoRepository contactRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final Environment environment;
    private final boolean demoResetEnabled;

    /**
     * Initialise le service avec les dépendances nécessaires à la gestion des
     * messages Contact et à l'identification sécurisée de l'environnement DEMO.
     *
     * @param contactRepository repository MongoDB des messages Contact
     * @param notificationService service de gestion des notifications
     * @param userRepository repository relationnel des utilisateurs
     * @param environment environnement Spring actif
     * @param demoResetEnabled indique si le mécanisme de reset DEMO est activé
     */
    public ContactServiceImpl(
            ContactMongoRepository contactRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            Environment environment,
            @Value("${magiclibrary.demo.reset.enabled:false}")
            boolean demoResetEnabled
    ) {
        this.contactRepository = contactRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.environment = environment;
        this.demoResetEnabled = demoResetEnabled;
    }

    // =========================================================================
    // CRÉATION
    // =========================================================================

    /**
     * Crée un nouveau message de contact pour l'utilisateur authentifié, puis
     * génère les notifications destinées aux administrateurs actifs.
     *
     * Les deux états d'archivage sont explicitement initialisés à false.
     *
     * @param request données du message à créer
     * @return message créé sous forme de DTO
     */
    @Override
    public ContactResponseDTO createContact(ContactRequestDTO request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new RuntimeException(
                    "Utilisateur connecté introuvable : "
                            + "authentification absente."
            );
        }

        String memberEmail = authentication.getName();

        User memberUser = userRepository.findByEmailUser(memberEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur connecté introuvable pour l'email : "
                                + memberEmail
                ));

        Integer memberUserId = memberUser.getIdUser();

        if (memberUserId == null) {
            throw new RuntimeException(
                    "Utilisateur connecté invalide : "
                            + "identifiant introuvable."
            );
        }

        ContactDocument document = new ContactDocument();

        document.setIdUser(memberUserId);
        document.setNameContact(request.getName());
        document.setEmailContact(memberEmail);
        document.setSubjectContact(request.getSubject());
        document.setContentContact(request.getMessage());

        document.setOriginContact("formulaire-web");
        document.setStatusContact(ContactStatus.NEW.name());
        document.setDateContact(LocalDateTime.now());

        document.setResponseSentContact(false);
        document.setResponseContentContact(null);
        document.setAnsweredByUserId(null);
        document.setUpdatedAtContact(null);

        /*
         * Un nouveau message est obligatoirement actif dans les deux espaces.
         */
        document.setArchivedByMember(false);
        document.setArchivedAtByMember(null);

        document.setArchivedByAdmin(false);
        document.setArchivedAtByAdmin(null);

        applyDemoMarkerIfRequired(document);

        ContactDocument saved = contactRepository.save(document);

        createAdminNotificationsForNewContact(saved);

        return convertToResponseDTO(saved);
    }

    // =========================================================================
    // MÉTHODES HISTORIQUES
    // =========================================================================

    /**
     * Retourne l'ensemble des messages de contact triés du plus récent au plus
     * ancien, indépendamment des états d'archivage.
     *
     * Cette méthode est conservée pour compatibilité pendant la migration.
     *
     * @return liste complète des messages
     */
    @Override
    public List<ContactResponseDTO> getAllContacts() {
        return contactRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .sorted(contactDateDescendingComparator())
                .collect(Collectors.toList());
    }

    /**
     * Retourne tous les messages associés à un utilisateur,
     * indépendamment de leur état d'archivage côté membre.
     *
     * @param idUser identifiant de l'utilisateur
     * @return messages appartenant à cet utilisateur
     */
    @Override
    public List<ContactResponseDTO> getContactsForUser(Integer idUser) {
        return getAllContacts().stream()
                .filter(contact ->
                        Objects.equals(contact.getIdUser(), idUser)
                )
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ESPACE MEMBRE : ACTIFS / ARCHIVÉS
    // =========================================================================

    /**
     * Retourne uniquement les messages actifs dans l'espace du membre.
     */
    @Override
    public List<ContactResponseDTO> getActiveContactsForUser(
            Integer idUser
    ) {
        if (idUser == null) {
            return List.of();
        }

        return contactRepository.findAll()
                .stream()
                .filter(document ->
                        Objects.equals(document.getIdUser(), idUser)
                )
                .filter(document ->
                        !document.isArchivedByMember()
                )
                .map(this::convertToResponseDTO)
                .sorted(contactDateDescendingComparator())
                .collect(Collectors.toList());
    }

    /**
     * Retourne uniquement les messages archivés dans l'espace du membre.
     */
    @Override
    public List<ContactResponseDTO> getArchivedContactsForUser(
            Integer idUser
    ) {
        if (idUser == null) {
            return List.of();
        }

        return contactRepository.findAll()
                .stream()
                .filter(document ->
                        Objects.equals(document.getIdUser(), idUser)
                )
                .filter(ContactDocument::isArchivedByMember)
                .map(this::convertToResponseDTO)
                .sorted(contactDateDescendingComparator())
                .collect(Collectors.toList());
    }

    /**
     * Archive logiquement un message dans l'espace personnel de son auteur.
     *
     * L'opération est idempotente :
     * si le message est déjà archivé côté membre, il est simplement retourné.
     */
    @Override
    public ContactResponseDTO archiveContactForUser(
            String id,
            Integer idUser
    ) {
        ContactDocument document =
                findContactOwnedByUser(id, idUser);

        if (!document.isArchivedByMember()) {
            document.setArchivedByMember(true);
            document.setArchivedAtByMember(LocalDateTime.now());

            document = contactRepository.save(document);
        }

        return convertToResponseDTO(document);
    }

    /**
     * Restaure un message précédemment archivé dans l'espace du membre.
     *
     * L'opération est idempotente :
     * si le message est déjà actif côté membre, il est simplement retourné.
     */
    @Override
    public ContactResponseDTO restoreContactForUser(
            String id,
            Integer idUser
    ) {
        ContactDocument document =
                findContactOwnedByUser(id, idUser);

        if (document.isArchivedByMember()) {
            document.setArchivedByMember(false);
            document.setArchivedAtByMember(null);

            document = contactRepository.save(document);
        }

        return convertToResponseDTO(document);
    }

    // =========================================================================
    // ESPACE ADMIN : ACTIFS / ARCHIVÉS
    // =========================================================================

    /**
     * Retourne uniquement les messages actifs côté administration.
     *
     * L'état d'archivage du membre est volontairement ignoré.
     */
    @Override
    public List<ContactResponseDTO> getActiveContactsForAdmin(
            Integer idRequester
    ) {
        assertAdminRequester(idRequester);

        return contactRepository.findAll()
                .stream()
                .filter(document ->
                        !document.isArchivedByAdmin()
                )
                .map(this::convertToResponseDTO)
                .sorted(contactDateDescendingComparator())
                .collect(Collectors.toList());
    }

    /**
     * Retourne uniquement les messages archivés côté administration.
     *
     * L'état d'archivage du membre est volontairement ignoré.
     */
    @Override
    public List<ContactResponseDTO> getArchivedContactsForAdmin(
            Integer idRequester
    ) {
        assertAdminRequester(idRequester);

        return contactRepository.findAll()
                .stream()
                .filter(ContactDocument::isArchivedByAdmin)
                .map(this::convertToResponseDTO)
                .sorted(contactDateDescendingComparator())
                .collect(Collectors.toList());
    }

    /**
     * Archive logiquement un message uniquement dans l'espace administratif.
     *
     * L'opération est idempotente.
     */
    @Override
    public ContactResponseDTO archiveContactForAdmin(
            String id,
            Integer idRequester
    ) {
        assertAdminRequester(idRequester);

        ContactDocument document = findContactDocument(id);

        if (!document.isArchivedByAdmin()) {
            document.setArchivedByAdmin(true);
            document.setArchivedAtByAdmin(LocalDateTime.now());

            document = contactRepository.save(document);
        }

        return convertToResponseDTO(document);
    }

    /**
     * Restaure un message précédemment archivé côté administration.
     *
     * L'opération est idempotente.
     */
    @Override
    public ContactResponseDTO restoreContactForAdmin(
            String id,
            Integer idRequester
    ) {
        assertAdminRequester(idRequester);

        ContactDocument document = findContactDocument(id);

        if (document.isArchivedByAdmin()) {
            document.setArchivedByAdmin(false);
            document.setArchivedAtByAdmin(null);

            document = contactRepository.save(document);
        }

        return convertToResponseDTO(document);
    }

    // =========================================================================
    // CONSULTATION DÉTAILLÉE
    // =========================================================================

    /**
     * Retourne un message à partir de son identifiant MongoDB.
     *
     * @param id identifiant MongoDB
     * @return message correspondant
     */
    @Override
    public ContactResponseDTO getContactById(String id) {
        return convertToResponseDTO(
                findContactDocument(id)
        );
    }

    /**
     * Retourne un message uniquement s'il appartient à l'utilisateur concerné.
     *
     * @param id identifiant MongoDB du message
     * @param idUser identifiant de l'utilisateur
     * @return message correspondant, ou null si l'accès n'est pas autorisé
     */
    @Override
    public ContactResponseDTO getContactByIdForUser(
            String id,
            Integer idUser
    ) {
        if (id == null || id.isBlank() || idUser == null) {
            return null;
        }

        return contactRepository.findById(id)
                .filter(document ->
                        Objects.equals(document.getIdUser(), idUser)
                )
                .map(this::convertToResponseDTO)
                .orElse(null);
    }

    // =========================================================================
    // RÉPONSE ADMINISTRATEUR
    // =========================================================================

    /**
     * Enregistre une réponse administrateur puis notifie le membre concerné.
     *
     * Les états d'archivage existants sont conservés tels quels.
     *
     * @param id identifiant MongoDB du message
     * @param request contenu de la réponse
     * @return message mis à jour
     */
    @Override
    public ContactResponseDTO replyToContact(
            String id,
            ContactReplyRequestDTO request
    ) {
        ContactDocument document = findContactDocument(id);

        ContactStatus currentStatus =
                ContactStatus.fromValue(document.getStatusContact());

        if (ContactStatus.ANSWERED.equals(currentStatus)
                || document.isResponseSentContact()) {
            throw new ContactAlreadyAnsweredException(
                    "Ce message a déjà reçu une réponse."
            );
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new RuntimeException(
                    "Administrateur connecté introuvable : "
                            + "authentification absente."
            );
        }

        String adminEmail = authentication.getName();

        User adminUser = userRepository
                .findByEmailUserWithRole(adminEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Administrateur connecté introuvable pour l'email : "
                                + adminEmail
                ));

        Integer adminUserId = adminUser.getIdUser();

        if (adminUserId == null) {
            throw new RuntimeException(
                    "Administrateur connecté invalide : "
                            + "identifiant introuvable."
            );
        }

        document.setResponseContentContact(
                request.getResponseContent()
        );
        document.setResponseSentContact(true);
        document.setStatusContact(
                ContactStatus.ANSWERED.name()
        );
        document.setAnsweredByUserId(adminUserId);
        document.setUpdatedAtContact(LocalDateTime.now());

        ContactDocument saved =
                contactRepository.save(document);

        createMemberNotificationForReply(saved);

        return convertToResponseDTO(saved);
    }

    // =========================================================================
    // RECHERCHE / SÉCURITÉ INTERNE
    // =========================================================================

    /**
     * Charge un document Contact ou interrompt l'opération s'il n'existe pas.
     */
    private ContactDocument findContactDocument(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException(
                    "Identifiant du message de contact invalide."
            );
        }

        return contactRepository.findById(id.trim())
                .orElseThrow(() -> new RuntimeException(
                        "Message de contact introuvable : " + id
                ));
    }

    /**
     * Charge un document uniquement s'il appartient au membre demandeur.
     *
     * Le même message d'erreur est utilisé qu'il soit inexistant
     * ou qu'il appartienne à un autre utilisateur afin de ne pas exposer
     * inutilement l'existence d'un document tiers.
     */
    private ContactDocument findContactOwnedByUser(
            String id,
            Integer idUser
    ) {
        if (id == null
                || id.isBlank()
                || idUser == null) {
            throw new RuntimeException(
                    "Message de contact introuvable ou inaccessible."
            );
        }

        return contactRepository.findById(id.trim())
                .filter(document ->
                        Objects.equals(document.getIdUser(), idUser)
                )
                .orElseThrow(() -> new RuntimeException(
                        "Message de contact introuvable ou inaccessible."
                ));
    }

    /**
     * Vérifie que le demandeur transmis au service possède réellement
     * le rôle ADMIN.
     *
     * Le contrôle est volontairement conservé au niveau métier en complément
     * des protections Spring Security des contrôleurs.
     */
    private void assertAdminRequester(Integer idRequester) {
        if (idRequester == null) {
            throw new ForbiddenException(
                    "Accès réservé aux administrateurs."
            );
        }

        User requester = userRepository
                .findByIdUserWithRole(idRequester)
                .orElseThrow(() -> new ForbiddenException(
                        "Administrateur introuvable."
                ));

        boolean isAdmin =
                requester.getRole() != null
                        && requester.getRole().getLabelRole() != null
                        && "ADMIN".equalsIgnoreCase(
                        requester.getRole()
                                .getLabelRole()
                                .trim()
                );

        if (!isAdmin) {
            throw new ForbiddenException(
                    "Accès réservé aux administrateurs."
            );
        }
    }

    // =========================================================================
    // DEMO
    // =========================================================================

    /**
     * Attribue le marqueur temporaire officiel à un message créé pendant
     * l'utilisation de la DEMO.
     */
    private void applyDemoMarkerIfRequired(
            ContactDocument document
    ) {
        if (document == null) {
            return;
        }

        boolean demoProfileActive =
                environment.acceptsProfiles(
                        Profiles.of("demo")
                );

        if (!demoProfileActive || !demoResetEnabled) {
            return;
        }

        document.setDemoScenarioCode(
                DemoScenarioCodes
                        .RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
        );
    }

    // =========================================================================
    // NOTIFICATIONS
    // =========================================================================

    /**
     * Crée une notification pour chaque administrateur actif lorsqu'un nouveau
     * message est reçu.
     */
    private void createAdminNotificationsForNewContact(
            ContactDocument contact
    ) {
        List<User> adminUsers =
                userRepository.findAllWithFilters(
                        "",
                        "ADMIN",
                        true
                );

        for (User admin : adminUsers) {
            NotificationRequestDTO notificationRequest =
                    new NotificationRequestDTO();

            notificationRequest.setIdUser(
                    admin.getIdUser()
            );

            notificationRequest.setTitleNotification(
                    "Nouveau message de contact"
            );

            notificationRequest.setMessageNotification(
                    "Un nouveau message de contact a été reçu : "
                            + contact.getSubjectContact()
            );

            notificationRequest.setTargetLinkNotification(
                    "/admin/messages?selectedContactId="
                            + contact.getId()
            );

            notificationRequest.setTypeNotification(
                    NotificationType.CONTACT
            );

            notificationRequest.setCategoryNotification(
                    NotificationCategory.CONTACT
            );

            notificationRequest.setPriorityNotification(
                    "HIGH"
            );

            notificationService.createSystemNotification(
                    notificationRequest
            );
        }
    }

    /**
     * Crée une notification pour le membre lorsqu'une réponse est apportée
     * à son message.
     */
    private void createMemberNotificationForReply(
            ContactDocument contact
    ) {
        if (contact.getIdUser() == null) {
            return;
        }

        NotificationRequestDTO notificationRequest =
                new NotificationRequestDTO();

        notificationRequest.setIdUser(
                contact.getIdUser()
        );

        notificationRequest.setTitleNotification(
                "Réponse à votre message de contact"
        );

        notificationRequest.setMessageNotification(
                "Une réponse a été apportée à votre demande : "
                        + contact.getSubjectContact()
        );

        notificationRequest.setTargetLinkNotification(
                "/mes-messages-de-contact?selectedContactId="
                        + contact.getId()
        );

        notificationRequest.setTypeNotification(
                NotificationType.CONTACT
        );

        notificationRequest.setCategoryNotification(
                NotificationCategory.CONTACT
        );

        notificationRequest.setPriorityNotification(
                "MEDIUM"
        );

        notificationService.createSystemNotification(
                notificationRequest
        );
    }

    // =========================================================================
    // CONVERSION DTO
    // =========================================================================

    /**
     * Construit le DTO exposé aux interfaces à partir du document MongoDB.
     *
     * Les quatre informations d'archivage sont volontairement propagées
     * jusqu'au DTO :
     *
     * - archivage membre ;
     * - date d'archivage membre ;
     * - archivage administration ;
     * - date d'archivage administration.
     *
     * Les interfaces peuvent ainsi afficher les vues Actifs / Archivés
     * sans mélanger l'état propre au membre et celui propre aux administrateurs.
     */
    private ContactResponseDTO convertToResponseDTO(
            ContactDocument document
    ) {
        ContactStatus status =
                ContactStatus.fromValue(
                        document.getStatusContact()
                );

        boolean answered =
                ContactStatus.ANSWERED.equals(status);

        String statusBadgeClass = answered
                ? ANSWERED_BADGE_CLASS
                : NEW_BADGE_CLASS;

        String senderRoleLabel =
                resolveSenderRoleLabel(
                        document.getIdUser()
                );

        String answeredByAdminLabel =
                resolveAnsweredByAdminLabel(
                        document.getAnsweredByUserId()
                );

        return new ContactResponseDTO(
                document.getId(),
                document.getIdUser(),
                document.getNameContact(),
                document.getEmailContact(),
                document.getSubjectContact(),
                document.getContentContact(),
                document.getOriginContact(),
                status.name(),
                status.getLabel(),
                answered,
                statusBadgeClass,
                senderRoleLabel,
                document.getDateContact(),
                document.isResponseSentContact(),
                document.getResponseContentContact(),
                document.getAnsweredByUserId(),
                answeredByAdminLabel,
                document.getUpdatedAtContact(),

                /*
                 * Archivage indépendant côté membre.
                 */
                document.isArchivedByMember(),
                document.getArchivedAtByMember(),

                /*
                 * Archivage indépendant côté administration.
                 */
                document.isArchivedByAdmin(),
                document.getArchivedAtByAdmin()
        );
    }

    /**
     * Comparateur standard des messages Contact :
     * messages les plus récents en premier.
     */
    private Comparator<ContactResponseDTO>
    contactDateDescendingComparator() {
        return Comparator.comparing(
                ContactResponseDTO::getDate,
                Comparator.nullsLast(
                        Comparator.reverseOrder()
                )
        );
    }

    // =========================================================================
    // LIBELLÉS UTILISATEURS / ADMIN
    // =========================================================================

    /**
     * Détermine le rôle à afficher pour l'auteur du message.
     */
    private String resolveSenderRoleLabel(Integer idUser) {
        if (idUser == null) {
            return MEMBER_ROLE_LABEL;
        }

        return userRepository.findByIdUserWithRole(idUser)
                .map(User::getRole)
                .map(role -> role.getLabelRole())
                .map(this::normalizeSenderRoleLabel)
                .orElse(UNKNOWN_ROLE_LABEL);
    }

    /**
     * Détermine le nom affiché de l'administrateur ayant répondu au message.
     */
    private String resolveAnsweredByAdminLabel(
            Integer answeredByUserId
    ) {
        if (answeredByUserId == null) {
            return null;
        }

        return userRepository
                .findByIdUserWithRole(answeredByUserId)
                .map(this::buildAdminDisplayName)
                .filter(label ->
                        label != null && !label.isBlank()
                )
                .orElse(UNKNOWN_ADMIN_LABEL);
    }

    /**
     * Construit le nom complet affiché d'un administrateur.
     */
    private String buildAdminDisplayName(User user) {
        if (user == null) {
            return null;
        }

        String firstName =
                user.getFirstNameUser() != null
                        ? user.getFirstNameUser().trim()
                        : "";

        String lastName =
                user.getLastNameUser() != null
                        ? user.getLastNameUser().trim()
                        : "";

        String fullName =
                (firstName + " " + lastName).trim();

        return fullName.isBlank()
                ? null
                : fullName;
    }

    /**
     * Harmonise les libellés de rôles destinés à l'affichage.
     */
    private String normalizeSenderRoleLabel(
            String roleLabel
    ) {
        if (roleLabel == null || roleLabel.isBlank()) {
            return UNKNOWN_ROLE_LABEL;
        }

        String normalized = roleLabel.trim();

        if ("ADMIN".equalsIgnoreCase(normalized)
                || "ADMINISTRATEUR".equalsIgnoreCase(normalized)) {
            return ADMIN_ROLE_LABEL;
        }

        if ("MEMBRE".equalsIgnoreCase(normalized)
                || "MEMBER".equalsIgnoreCase(normalized)) {
            return MEMBER_ROLE_LABEL;
        }

        if ("INVITE".equalsIgnoreCase(normalized)
                || "INVITÉ".equalsIgnoreCase(normalized)) {
            return "Invité";
        }

        return normalized;
    }
}