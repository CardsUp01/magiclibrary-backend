package com.magiclibrary.mongo.services;

import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ContactStatus;
import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import com.magiclibrary.exceptions.custom.ContactAlreadyAnsweredException;
import com.magiclibrary.mongo.documents.ContactDocument;
import com.magiclibrary.mongo.dto.ContactReplyRequestDTO;
import com.magiclibrary.mongo.dto.ContactRequestDTO;
import com.magiclibrary.mongo.dto.ContactResponseDTO;
import com.magiclibrary.mongo.repositories.ContactMongoRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des messages de contact.
 *
 * Cette classe gère la création, la consultation et le traitement
 * des messages stockés dans MongoDB, ainsi que les notifications
 * associées aux administrateurs et aux membres.
 */
@Service
public class ContactServiceImpl implements ContactService {

    private static final String ANSWERED_BADGE_CLASS = " bg-gray-50 text-gray-700 ring-gray-200";
    private static final String NEW_BADGE_CLASS = " bg-blue-50 text-blue-900 ring-blue-200";
    private static final String MEMBER_ROLE_LABEL = "Membre";
    private static final String ADMIN_ROLE_LABEL = "Admin";
    private static final String UNKNOWN_ROLE_LABEL = "Rôle inconnu";
    private static final String UNKNOWN_ADMIN_LABEL = "Admin introuvable";

    private final ContactMongoRepository contactRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ContactServiceImpl(
            ContactMongoRepository contactRepository,
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.contactRepository = contactRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /*
     * Crée un nouveau message de contact pour l'utilisateur authentifié
     * puis génère les notifications destinées aux administrateurs.
     */
    @Override
    public ContactResponseDTO createContact(ContactRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new RuntimeException("Utilisateur connecté introuvable : authentification absente.");
        }

        String memberEmail = authentication.getName();

        User memberUser = userRepository.findByEmailUser(memberEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur connecté introuvable pour l'email : " + memberEmail
                ));

        Integer memberUserId = memberUser.getIdUser();

        if (memberUserId == null) {
            throw new RuntimeException("Utilisateur connecté invalide : identifiant introuvable.");
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

        ContactDocument saved = contactRepository.save(document);

        createAdminNotificationsForNewContact(saved);

        return convertToResponseDTO(saved);
    }

    /*
     * Retourne l'ensemble des messages de contact triés
     * du plus récent au plus ancien.
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

    /*
     * Retourne uniquement les messages associés à un utilisateur.
     */
    @Override
    public List<ContactResponseDTO> getContactsForUser(Integer idUser) {
        return getAllContacts().stream()
                .filter(contact -> Objects.equals(contact.getIdUser(), idUser))
                .collect(Collectors.toList());
    }

    @Override
    public ContactResponseDTO getContactById(String id) {
        ContactDocument doc = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message de contact introuvable : " + id));

        return convertToResponseDTO(doc);
    }

    /*
     * Retourne un message uniquement s'il appartient
     * à l'utilisateur concerné.
     */
    @Override
    public ContactResponseDTO getContactByIdForUser(String id, Integer idUser) {
        if (id == null || id.isBlank() || idUser == null) {
            return null;
        }

        return contactRepository.findById(id)
                .filter(doc -> Objects.equals(doc.getIdUser(), idUser))
                .map(this::convertToResponseDTO)
                .orElse(null);
    }

    /*
     * Enregistre une réponse administrateur puis notifie
     * le membre concerné.
     */
    @Override
    public ContactResponseDTO replyToContact(String id, ContactReplyRequestDTO request) {
        ContactDocument doc = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message de contact introuvable : " + id));

        ContactStatus currentStatus = ContactStatus.fromValue(doc.getStatusContact());

        if (ContactStatus.ANSWERED.equals(currentStatus) || doc.isResponseSentContact()) {
            throw new ContactAlreadyAnsweredException("Ce message a déjà reçu une réponse.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new RuntimeException("Administrateur connecté introuvable : authentification absente.");
        }

        String adminEmail = authentication.getName();

        User adminUser = userRepository.findByEmailUserWithRole(adminEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Administrateur connecté introuvable pour l'email : " + adminEmail
                ));

        Integer adminUserId = adminUser.getIdUser();

        if (adminUserId == null) {
            throw new RuntimeException("Administrateur connecté invalide : identifiant introuvable.");
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

    /*
     * Crée une notification pour chaque administrateur actif
     * lorsqu'un nouveau message est reçu.
     */
    private void createAdminNotificationsForNewContact(ContactDocument contact) {
        List<User> adminUsers = userRepository.findAllWithFilters("", "ADMIN", true);

        for (User admin : adminUsers) {
            NotificationRequestDTO notificationRequest = new NotificationRequestDTO();
            notificationRequest.setIdUser(admin.getIdUser());
            notificationRequest.setTitleNotification("Nouveau message de contact");
            notificationRequest.setMessageNotification(
                    "Un nouveau message de contact a été reçu : " + contact.getSubjectContact()
            );
            notificationRequest.setTargetLinkNotification("/admin/messages?selectedContactId=" + contact.getId());
            notificationRequest.setTypeNotification(NotificationType.CONTACT);
            notificationRequest.setCategoryNotification(NotificationCategory.CONTACT);
            notificationRequest.setPriorityNotification("HIGH");

            notificationService.createSystemNotification(notificationRequest);
        }
    }

    /*
     * Crée une notification pour le membre lorsqu'une réponse
     * est apportée à son message.
     */
    private void createMemberNotificationForReply(ContactDocument contact) {
        if (contact.getIdUser() == null) {
            return;
        }

        NotificationRequestDTO notificationRequest = new NotificationRequestDTO();
        notificationRequest.setIdUser(contact.getIdUser());
        notificationRequest.setTitleNotification("Réponse à votre message de contact");
        notificationRequest.setMessageNotification(
                "Une réponse a été apportée à votre demande : " + contact.getSubjectContact()
        );
        notificationRequest.setTargetLinkNotification("/mes-messages-de-contact?selectedContactId=" + contact.getId());
        notificationRequest.setTypeNotification(NotificationType.CONTACT);
        notificationRequest.setCategoryNotification(NotificationCategory.CONTACT);
        notificationRequest.setPriorityNotification("MEDIUM");

        notificationService.createSystemNotification(notificationRequest);
    }

    /*
     * Construit le DTO exposé aux interfaces à partir
     * du document MongoDB.
     */
    private ContactResponseDTO convertToResponseDTO(ContactDocument d) {
        ContactStatus status = ContactStatus.fromValue(d.getStatusContact());
        boolean answered = ContactStatus.ANSWERED.equals(status);
        String statusBadgeClass = answered ? ANSWERED_BADGE_CLASS : NEW_BADGE_CLASS;
        String senderRoleLabel = resolveSenderRoleLabel(d.getIdUser());
        String answeredByAdminLabel = resolveAnsweredByAdminLabel(d.getAnsweredByUserId());

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

    /*
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

    /*
     * Détermine le nom affiché de l'administrateur
     * ayant répondu au message.
     */
    private String resolveAnsweredByAdminLabel(Integer answeredByUserId) {
        if (answeredByUserId == null) {
            return null;
        }

        return userRepository.findByIdUserWithRole(answeredByUserId)
                .map(this::buildAdminDisplayName)
                .filter(label -> label != null && !label.isBlank())
                .orElse(UNKNOWN_ADMIN_LABEL);
    }

    /*
     * Construit le nom complet affiché d'un administrateur.
     */
    private String buildAdminDisplayName(User user) {
        if (user == null) {
            return null;
        }

        String firstName = user.getFirstNameUser() != null ? user.getFirstNameUser().trim() : "";
        String lastName = user.getLastNameUser() != null ? user.getLastNameUser().trim() : "";
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }

    /*
     * Harmonise les libellés de rôles destinés à l'affichage.
     */
    private String normalizeSenderRoleLabel(String roleLabel) {
        if (roleLabel == null || roleLabel.isBlank()) {
            return UNKNOWN_ROLE_LABEL;
        }

        String normalized = roleLabel.trim();

        if ("ADMIN".equalsIgnoreCase(normalized) || "ADMINISTRATEUR".equalsIgnoreCase(normalized)) {
            return ADMIN_ROLE_LABEL;
        }

        if ("MEMBRE".equalsIgnoreCase(normalized) || "MEMBER".equalsIgnoreCase(normalized)) {
            return MEMBER_ROLE_LABEL;
        }

        if ("INVITE".equalsIgnoreCase(normalized) || "INVITÉ".equalsIgnoreCase(normalized)) {
            return "Invité";
        }

        return normalized;
    }
}