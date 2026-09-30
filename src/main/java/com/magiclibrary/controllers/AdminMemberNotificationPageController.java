package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
// Sécurité, MVC, validation des formulaires et gestion des redirections
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// DTO
import com.magiclibrary.dto.notification.NotificationRequestDTO;
import com.magiclibrary.dto.user.UserResponseDTO;

// Entités
import com.magiclibrary.entities.User;

// Enums notifications
import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;

// Exceptions métier
import com.magiclibrary.exceptions.custom.UserNotFoundException;

// Repositories
import com.magiclibrary.repositories.interfaces.UserRepository;

// Services
import com.magiclibrary.services.NotificationService;
import com.magiclibrary.services.UserService;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * =============================================================================
 * CONTROLEUR SSR : AdminMemberNotificationPageController
 * =============================================================================
 *
 * Contrôleur réservé à l'envoi manuel d'une notification par un administrateur
 * vers un utilisateur autorisé de MagicLibrary.
 *
 * <p>
 * Le contrôleur reste rattaché au module d'administration des membres car
 * l'action est accessible depuis les écrans {@code /admin/membres}, lesquels
 * centralisent actuellement la consultation des comptes utilisateurs.
 * </p>
 *
 * <p>
 * Cette fonctionnalité complète l'infrastructure de notifications existante
 * sans introduire de système de messagerie supplémentaire.
 * </p>
 *
 * <p>
 * Parcours fonctionnel :
 * </p>
 *
 * <pre>
 * ADMIN
 *   -> fiche ou liste des utilisateurs
 *   -> Envoyer une notification
 *   -> saisie du titre et du message
 *   -> création de la notification
 *   -> consultation par le destinataire dans son espace Notifications
 * </pre>
 *
 * <p>
 * Destinataires autorisés :
 * </p>
 *
 * <ul>
 *     <li>MEMBRE ;</li>
 *     <li>ADMIN différent de l'administrateur actuellement connecté.</li>
 * </ul>
 *
 * <p>
 * Destinataires refusés :
 * </p>
 *
 * <ul>
 *     <li>INVITE ;</li>
 *     <li>tout rôle non explicitement autorisé ;</li>
 *     <li>l'administrateur connecté lui-même.</li>
 * </ul>
 *
 * <p>
 * Règles de sécurité :
 * </p>
 *
 * <ul>
 *     <li>les routes sont réservées au rôle ADMIN ;</li>
 *     <li>le destinataire est déterminé exclusivement par l'identifiant
 *         présent dans l'URL ;</li>
 *     <li>le compte cible est systématiquement rechargé depuis la base ;</li>
 *     <li>le rôle du destinataire est contrôlé côté serveur ;</li>
 *     <li>un administrateur ne peut pas s'envoyer une notification à lui-même ;</li>
 *     <li>le type et la catégorie de notification sont imposés côté serveur ;</li>
 *     <li>aucun identifiant utilisateur fourni par le formulaire n'est utilisé ;</li>
 *     <li>l'administrateur authentifié est résolu depuis Spring Security ;</li>
 *     <li>la création métier est déléguée à NotificationService.</li>
 * </ul>
 *
 * <p>
 * La notification créée manuellement utilise :
 * </p>
 *
 * <ul>
 *     <li>type : {@link NotificationType#SYSTEM} ;</li>
 *     <li>catégorie : {@link NotificationCategory#SYSTEM} ;</li>
 *     <li>lien cible : aucun ;</li>
 *     <li>priorité : aucune valeur spécifique pour cette première version.</li>
 * </ul>
 *
 * <p>
 * La date de création, le statut lu/non lu et l'éventuel marqueur DEMO
 * restent gérés par NotificationServiceImpl.
 * </p>
 */
@Controller
public class AdminMemberNotificationPageController {

    // -------------------------------------------------------------------------
    // DÉPENDANCES
    // -------------------------------------------------------------------------

    /**
     * Service utilisé pour consulter les informations
     * de l'utilisateur destinataire.
     */
    private final UserService userService;

    /**
     * Service métier responsable de la création effective de la notification.
     */
    private final NotificationService notificationService;

    /**
     * Repository utilisé uniquement pour résoudre l'administrateur authentifié
     * à partir de son adresse email Spring Security.
     */
    private final UserRepository userRepository;

    /**
     * Injection des dépendances par constructeur.
     *
     * @param userService service de gestion des utilisateurs
     * @param notificationService service de gestion des notifications
     * @param userRepository repository des utilisateurs
     */
    public AdminMemberNotificationPageController(
            UserService userService,
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // GET : AFFICHAGE DU FORMULAIRE D'ENVOI
    // =========================================================================

    /**
     * Affiche le formulaire permettant à un administrateur d'envoyer
     * une notification à un utilisateur autorisé.
     *
     * <p>
     * Le destinataire est chargé depuis la base à partir de l'identifiant
     * contenu dans l'URL. Le formulaire ne permet donc jamais de modifier
     * librement le destinataire.
     * </p>
     *
     * <p>
     * Avant d'afficher le formulaire, le contrôleur vérifie :
     * </p>
     *
     * <ul>
     *     <li>que l'utilisateur cible existe ;</li>
     *     <li>qu'il possède le rôle MEMBRE ou ADMIN ;</li>
     *     <li>qu'il ne correspond pas à l'administrateur connecté.</li>
     * </ul>
     *
     * @param id identifiant de l'utilisateur destinataire
     * @param authentication contexte Spring Security courant
     * @param model modèle Thymeleaf
     * @param redirectAttributes attributs utilisés lors d'une redirection
     * @return template d'envoi ou redirection adaptée
     */
    @GetMapping("/admin/membres/{id}/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public String showNotificationForm(
            @PathVariable("id") Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UserResponseDTO targetUser = userService.getUserById(id);
            User requester = resolveCurrentUser(authentication);

            if (!isAllowedTarget(targetUser)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "L’envoi manuel d’une notification est autorisé uniquement vers un compte membre ou administrateur."
                );

                return "redirect:/admin/membres/" + id;
            }

            if (isSelfNotification(targetUser, requester)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Vous ne pouvez pas vous envoyer une notification à vous-même."
                );

                return "redirect:/admin/membres/" + id;
            }

            /*
             * Le formulaire exposé à l'interface contient volontairement
             * uniquement les deux informations que l'administrateur
             * est autorisé à saisir :
             *
             * - le titre ;
             * - le message.
             *
             * Les informations techniques de la notification sont ajoutées
             * exclusivement côté serveur au moment de la soumission.
             */
            if (!model.containsAttribute("notificationForm")) {
                model.addAttribute(
                        "notificationForm",
                        new AdminUserNotificationForm()
                );
            }

            preparePageModel(model, targetUser);

            return "admin/envoyer-notification";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/membres";
        }
    }

    // =========================================================================
    // POST : CRÉATION DE LA NOTIFICATION
    // =========================================================================

    /**
     * Traite la soumission du formulaire d'envoi d'une notification.
     *
     * <p>
     * La méthode applique plusieurs niveaux de protection :
     * </p>
     *
     * <ul>
     *     <li>validation Bean Validation du titre et du message ;</li>
     *     <li>rechargement de l'utilisateur cible depuis la base ;</li>
     *     <li>vérification du rôle MEMBRE ou ADMIN ;</li>
     *     <li>interdiction de l'auto-notification ;</li>
     *     <li>résolution de l'administrateur connecté ;</li>
     *     <li>construction serveur du NotificationRequestDTO ;</li>
     *     <li>création via NotificationService.</li>
     * </ul>
     *
     * <p>
     * L'identifiant destinataire, le type et la catégorie ne proviennent
     * jamais du formulaire HTML.
     * </p>
     *
     * @param id identifiant de l'utilisateur destinataire
     * @param notificationForm formulaire contenant titre et message
     * @param bindingResult résultat de la validation du formulaire
     * @param authentication contexte Spring Security courant
     * @param model modèle Thymeleaf
     * @param redirectAttributes attributs de redirection
     * @return formulaire en cas d'erreur ou fiche utilisateur après succès
     */
    @PostMapping("/admin/membres/{id}/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public String sendNotification(
            @PathVariable("id") Integer id,
            @Valid
            @ModelAttribute("notificationForm")
            AdminUserNotificationForm notificationForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        UserResponseDTO targetUser;

        try {
            /*
             * Le destinataire est systématiquement relu depuis la base.
             *
             * On ne se fie jamais à une donnée transmise depuis le navigateur
             * pour déterminer l'utilisateur réellement ciblé.
             */
            targetUser = userService.getUserById(id);

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/membres";
        }

        /*
         * L'administrateur connecté est également résolu depuis la base.
         *
         * Son identifiant permet :
         * - d'interdire l'auto-notification ;
         * - de transmettre au service métier l'identité du requérant.
         */
        User requester = resolveCurrentUser(authentication);

        if (!isAllowedTarget(targetUser)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "L’envoi manuel d’une notification est autorisé uniquement vers un compte membre ou administrateur."
            );

            return "redirect:/admin/membres/" + id;
        }

        if (isSelfNotification(targetUser, requester)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Vous ne pouvez pas vous envoyer une notification à vous-même."
            );

            return "redirect:/admin/membres/" + id;
        }

        /*
         * En cas d'erreur de validation, le formulaire est simplement
         * réaffiché avec les données déjà saisies.
         */
        if (bindingResult.hasErrors()) {
            preparePageModel(model, targetUser);

            model.addAttribute(
                    "errorMessage",
                    "Merci de corriger les champs du formulaire."
            );

            return "admin/envoyer-notification";
        }

        /*
         * Construction du DTO métier.
         *
         * Seuls le titre et le message proviennent du formulaire.
         * Toutes les données sensibles ou techniques sont imposées
         * explicitement par le backend.
         */
        NotificationRequestDTO requestDTO =
                new NotificationRequestDTO();

        requestDTO.setIdUser(
                targetUser.getIdUser()
        );

        requestDTO.setTitleNotification(
                notificationForm.getTitleNotification()
        );

        requestDTO.setMessageNotification(
                notificationForm.getMessageNotification()
        );

        /*
         * La notification est purement informative.
         * Aucun lien cible n'est nécessaire pour cette première version.
         */
        requestDTO.setTargetLinkNotification(null);

        /*
         * Pour la V1, une notification administrative manuelle
         * réutilise les valeurs génériques SYSTEM existantes.
         *
         * Aucun nouvel enum ni aucune migration SQL ne sont nécessaires.
         */
        requestDTO.setTypeNotification(
                NotificationType.SYSTEM
        );

        requestDTO.setCategoryNotification(
                NotificationCategory.SYSTEM
        );

        /*
         * Aucune priorité particulière n'est imposée.
         * Le champ reste donc null.
         */
        requestDTO.setPriorityNotification(null);

        /*
         * La création réelle est déléguée au service métier existant.
         *
         * NotificationServiceImpl se charge notamment :
         * - du contrôle du rôle ADMIN du requérant ;
         * - de la résolution du destinataire ;
         * - de la date de création ;
         * - du statut non lu ;
         * - du marqueur DEMO éventuel ;
         * - de la persistance JPA.
         */
        notificationService.createNotification(
                requestDTO,
                requester.getIdUser()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "La notification a été envoyée à l’utilisateur avec succès."
        );

        return "redirect:/admin/membres/" + id;
    }

    // =========================================================================
    // OUTILS INTERNES
    // =========================================================================

    /**
     * Prépare les attributs communs nécessaires à l'affichage
     * du formulaire Thymeleaf.
     *
     * @param model modèle MVC
     * @param targetUser utilisateur destinataire
     */
    private void preparePageModel(
            Model model,
            UserResponseDTO targetUser
    ) {
        model.addAttribute(
                "user",
                targetUser
        );

        model.addAttribute(
                "pageTitle",
                "Envoyer une notification"
        );

        model.addAttribute(
                "activePage",
                "admin-members"
        );
    }

    /**
     * Vérifie que l'utilisateur cible possède un rôle autorisé
     * pour recevoir une notification administrative manuelle.
     *
     * <p>
     * Les rôles actuellement acceptés sont :
     * </p>
     *
     * <ul>
     *     <li>MEMBRE ;</li>
     *     <li>ADMIN.</li>
     * </ul>
     *
     * <p>
     * Le rôle INVITE reste volontairement exclu de cette première version,
     * tant que son parcours de consultation des notifications n'a pas été
     * explicitement validé.
     * </p>
     *
     * @param user utilisateur à contrôler
     * @return true uniquement pour un compte MEMBRE ou ADMIN
     */
    private boolean isAllowedTarget(
            UserResponseDTO user
    ) {
        if (user == null || user.getRoleLabel() == null) {
            return false;
        }

        String roleLabel = user.getRoleLabel().trim();

        return roleLabel.equalsIgnoreCase("MEMBRE")
                || roleLabel.equalsIgnoreCase("ADMIN");
    }

    /**
     * Vérifie si l'administrateur tente de s'envoyer
     * une notification à lui-même.
     *
     * <p>
     * La comparaison repose exclusivement sur les identifiants techniques
     * des utilisateurs et non sur leur email ou leur nom.
     * </p>
     *
     * @param targetUser utilisateur destinataire
     * @param requester administrateur authentifié
     * @return true lorsque la cible et le requérant sont le même utilisateur
     */
    private boolean isSelfNotification(
            UserResponseDTO targetUser,
            User requester
    ) {
        if (targetUser == null
                || targetUser.getIdUser() == null
                || requester == null
                || requester.getIdUser() == null) {
            return false;
        }

        return targetUser.getIdUser()
                .equals(requester.getIdUser());
    }

    /**
     * Résout l'administrateur authentifié à partir du contexte Spring Security.
     *
     * <p>
     * L'adresse email présente dans Authentication correspond au nom
     * d'utilisateur utilisé par MagicLibrary.
     * </p>
     *
     * @param authentication contexte d'authentification courant
     * @return utilisateur authentifié
     * @throws UserNotFoundException si aucun utilisateur correspondant
     *                               n'est disponible
     */
    private User resolveCurrentUser(
            Authentication authentication
    ) {
        String email =
                authentication != null
                        ? authentication.getName()
                        : null;

        if (email == null || email.isBlank()) {
            throw new UserNotFoundException(
                    "Utilisateur authentifié introuvable."
            );
        }

        return userRepository
                .findByEmailUser(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Utilisateur authentifié introuvable."
                        )
                );
    }

    // =========================================================================
    // FORMULAIRE INTERNE : TITRE + MESSAGE UNIQUEMENT
    // =========================================================================

    /**
     * Modèle de formulaire interne utilisé uniquement par cette page SSR.
     *
     * <p>
     * Il est volontairement limité au titre et au message afin de ne jamais
     * exposer au navigateur les propriétés techniques de NotificationRequestDTO
     * telles que :
     * </p>
     *
     * <ul>
     *     <li>idUser ;</li>
     *     <li>typeNotification ;</li>
     *     <li>categoryNotification ;</li>
     *     <li>targetLinkNotification ;</li>
     *     <li>priorityNotification.</li>
     * </ul>
     *
     * <p>
     * Les setters normalisent les espaces externes avant la validation,
     * ce qui garantit notamment que la longueur minimale est contrôlée
     * sur le contenu réellement enregistré.
     * </p>
     */
    public static final class AdminUserNotificationForm {

        /**
         * Titre visible par le destinataire.
         */
        @NotBlank(
                message = "Le titre de la notification est obligatoire."
        )
        @Size(
                min = 2,
                max = 150,
                message = "Le titre doit contenir entre 2 et 150 caractères."
        )
        private String titleNotification;

        /**
         * Message principal de la notification.
         */
        @NotBlank(
                message = "Le message de la notification est obligatoire."
        )
        @Size(
                min = 2,
                max = 10_000,
                message = "Le message doit contenir entre 2 et 10 000 caractères."
        )
        private String messageNotification;

        public AdminUserNotificationForm() {
        }

        public String getTitleNotification() {
            return titleNotification;
        }

        public void setTitleNotification(
                String titleNotification
        ) {
            this.titleNotification =
                    titleNotification != null
                            ? titleNotification.trim()
                            : null;
        }

        public String getMessageNotification() {
            return messageNotification;
        }

        public void setMessageNotification(
                String messageNotification
        ) {
            this.messageNotification =
                    messageNotification != null
                            ? messageNotification.trim()
                            : null;
        }
    }
}