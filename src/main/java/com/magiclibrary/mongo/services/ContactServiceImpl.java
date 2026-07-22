package com.magiclibrary.mongo.services;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ContactStatus;
import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import com.magiclibrary.exceptions.custom.ContactAlreadyAnsweredException;
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
 *      - conversion des documents MongoDB en DTO d'affichage.
 *
 * Gestion de l'environnement DEMO :
 *      Lorsqu'un message est créé pendant l'exécution du profil Spring
 *      {@code demo} et que le mécanisme de réinitialisation est explicitement
 *      activé, le document reçoit le marqueur :
 *
 *          RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
 *
 *      Ce marqueur permet au mécanisme de reset de supprimer uniquement les
 *      messages temporaires créés pendant les tests, sans dépendre de leur
 *      email, sujet, contenu, date, statut ou identifiant MongoDB.
 *
 * Sécurité CLIENT :
 *      Hors profil {@code demo}, ou lorsque la propriété de reset est
 *      désactivée, aucun marqueur temporaire n'est attribué. Les documents
 *      créés dans la future instance CLIENT conservent donc un
 *      {@code demoScenarioCode} null et ne peuvent pas être ciblés par le
 *      mécanisme de réinitialisation DEMO.
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

    /**
     * Crée un nouveau message de contact pour l'utilisateur authentifié, puis
     * génère les notifications destinées aux administrateurs actifs.
     *
     * <p>Dans l'environnement DEMO explicitement activé, le message reçoit un
     * marqueur temporaire afin de pouvoir être supprimé sélectivement lors de
     * la prochaine réinitialisation.</p>
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

        applyDemoMarkerIfRequired(document);

        ContactDocument saved = contactRepository.save(document);

        createAdminNotificationsForNewContact(saved);

        return convertToResponseDTO(saved);
    }

    /**
     * Retourne l'ensemble des messages de contact triés du plus récent au plus
     * ancien.
     *
     * @return liste complète des messages
     */
    @Override
    public List<ContactResponseDTO> getAllContacts() {
        return contactRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .sorted(Comparator.comparing(
                        ContactResponseDTO::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retourne uniquement les messages associés à un utilisateur.
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

    /**
     * Retourne un message à partir de son identifiant MongoDB.
     *
     * @param id identifiant MongoDB
     * @return message correspondant
     */
    @Override
    public ContactResponseDTO getContactById(String id) {
        ContactDocument doc = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Message de contact introuvable : " + id
                ));

        return convertToResponseDTO(doc);
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
                .filter(doc ->
                        Objects.equals(doc.getIdUser(), idUser)
                )
                .map(this::convertToResponseDTO)
                .orElse(null);
    }

    /**
     * Enregistre une réponse administrateur puis notifie le membre concerné.
     *
     * <p>Le marqueur DEMO existant du document est conservé lors de la mise à
     * jour. Un message temporaire reste donc identifiable après sa réponse et
     * pourra être supprimé au prochain reset.</p>
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
        ContactDocument doc = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Message de contact introuvable : " + id
                ));

        ContactStatus currentStatus =
                ContactStatus.fromValue(doc.getStatusContact());

        if (ContactStatus.ANSWERED.equals(currentStatus)
                || doc.isResponseSentContact()) {
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

        doc.setResponseContentContact(request.getResponseContent());
        doc.setResponseSentContact(true);
        doc.setStatusContact(ContactStatus.ANSWERED.name());
        doc.setAnsweredByUserId(adminUserId);
        doc.setUpdatedAtContact(LocalDateTime.now());

        ContactDocument saved = contactRepository.save(doc);

        createMemberNotificationForReply(saved);

        return convertToResponseDTO(saved);
    }

    /**
     * Attribue le marqueur temporaire officiel à un message créé pendant
     * l'utilisation de la DEMO.
     *
     * <p>Les deux conditions doivent être réunies :</p>
     *
     * <ul>
     *     <li>le profil Spring {@code demo} est actif ;</li>
     *     <li>la propriété {@code magiclibrary.demo.reset.enabled} vaut
     *     {@code true}.</li>
     * </ul>
     *
     * <p>En dehors de cette configuration, aucun marqueur n'est attribué.</p>
     *
     * @param document document Contact en cours de création
     */
    private void applyDemoMarkerIfRequired(ContactDocument document) {
        if (document == null) {
            return;
        }

        boolean demoProfileActive =
                environment.acceptsProfiles(Profiles.of("demo"));

        if (!demoProfileActive || !demoResetEnabled) {
            return;
        }

        document.setDemoScenarioCode(
                DemoScenarioCodes
                        .RECRUITER_DEMO_CREATED_CONTACT_MESSAGES
        );
    }

    /**
     * Crée une notification pour chaque administrateur actif lorsqu'un nouveau
     * message est reçu.
     *
     * @param contact message nouvellement enregistré
     */
    private void createAdminNotificationsForNewContact(
            ContactDocument contact
    ) {
        List<User> adminUsers =
                userRepository.findAllWithFilters("", "ADMIN", true);

        for (User admin : adminUsers) {
            NotificationRequestDTO notificationRequest =
                    new NotificationRequestDTO();

            notificationRequest.setIdUser(admin.getIdUser());
            notificationRequest.setTitleNotification(
                    "Nouveau message de contact"
            );
            notificationRequest.setMessageNotification(
                    "Un nouveau message de contact a été reçu : "
                            + contact.getSubjectContact()
            );
            notificationRequest.setTargetLinkNotification(
                    "/admin/messages?selectedContactId=" + contact.getId()
            );
            notificationRequest.setTypeNotification(
                    NotificationType.CONTACT
            );
            notificationRequest.setCategoryNotification(
                    NotificationCategory.CONTACT
            );
            notificationRequest.setPriorityNotification("HIGH");

            notificationService.createSystemNotification(
                    notificationRequest
            );
        }
    }

    /**
     * Crée une notification pour le membre lorsqu'une réponse est apportée à
     * son message.
     *
     * @param contact message ayant reçu une réponse
     */
    private void createMemberNotificationForReply(
            ContactDocument contact
    ) {
        if (contact.getIdUser() == null) {
            return;
        }

        NotificationRequestDTO notificationRequest =
                new NotificationRequestDTO();

        notificationRequest.setIdUser(contact.getIdUser());
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
        notificationRequest.setPriorityNotification("MEDIUM");

        notificationService.createSystemNotification(
                notificationRequest
        );
    }

    /**
     * Construit le DTO exposé aux interfaces à partir du document MongoDB.
     *
     * @param d document source
     * @return DTO destiné à l'affichage
     */
    private ContactResponseDTO convertToResponseDTO(
            ContactDocument d
    ) {
        ContactStatus status =
                ContactStatus.fromValue(d.getStatusContact());

        boolean answered =
                ContactStatus.ANSWERED.equals(status);

        String statusBadgeClass = answered
                ? ANSWERED_BADGE_CLASS
                : NEW_BADGE_CLASS;

        String senderRoleLabel =
                resolveSenderRoleLabel(d.getIdUser());

        String answeredByAdminLabel =
                resolveAnsweredByAdminLabel(
                        d.getAnsweredByUserId()
                );

        return new ContactResponseDTO(
                d.getId(),
                d.getIdUser(),
                d.getNameContact(),
                d.getEmailContact(),
                d.getSubjectContact(),
                d.getContentContact(),
                d.getOriginContact(),
                status.name(),
                status.getLabel(),
                answered,
                statusBadgeClass,
                senderRoleLabel,
                d.getDateContact(),
                d.isResponseSentContact(),
                d.getResponseContentContact(),
                d.getAnsweredByUserId(),
                answeredByAdminLabel,
                d.getUpdatedAtContact()
        );
    }

    /**
     * Détermine le rôle à afficher pour l'auteur du message.
     *
     * @param idUser identifiant de l'auteur
     * @return libellé de rôle normalisé
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
     *
     * @param answeredByUserId identifiant SQL de l'administrateur
     * @return nom complet, ou null si aucune réponse n'existe
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
     *
     * @param user utilisateur administrateur
     * @return nom complet ou null
     */
    private String buildAdminDisplayName(User user) {
        if (user == null) {
            return null;
        }

        String firstName = user.getFirstNameUser() != null
                ? user.getFirstNameUser().trim()
                : "";

        String lastName = user.getLastNameUser() != null
                ? user.getLastNameUser().trim()
                : "";

        String fullName =
                (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }

    /**
     * Harmonise les libellés de rôles destinés à l'affichage.
     *
     * @param roleLabel libellé issu de la base
     * @return libellé normalisé
     */
    private String normalizeSenderRoleLabel(String roleLabel) {
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