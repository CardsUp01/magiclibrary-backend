package com.magiclibrary.services.demo.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.magiclibrary.entities.Item;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;
import com.magiclibrary.entities.Notification;
import com.magiclibrary.entities.Role;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.LoanLineStatus;
import com.magiclibrary.enums.LoanStatus;
import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import com.magiclibrary.init.DemoScenarioCodes;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.repositories.interfaces.LoanLineRepository;
import com.magiclibrary.repositories.interfaces.LoanRepository;
import com.magiclibrary.repositories.interfaces.NotificationRepository;
import com.magiclibrary.repositories.interfaces.RoleRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.demo.DemoRelationalScenarioService;
import com.magiclibrary.services.demo.DemoScenarioDefinition;

/**
 * Reconstruit transactionnellement les scénarios relationnels DEMO stockés
 * dans MariaDB.
 *
 * <p>Cette implémentation remplace la logique historiquement portée par
 * {@code DemoLoanInitializer}. Elle peut être appelée par l'orchestrateur de
 * reset, le démarrage automatique et le scheduler sans dupliquer les règles
 * métier.</p>
 *
 * <p>La reconstruction couvre :</p>
 *
 * <ul>
 *     <li>la restauration canonique des comptes Admin, Lucas et Sarah ;</li>
 *     <li>la suppression sécurisée des comptes temporaires créés en DEMO ;</li>
 *     <li>la suppression ciblée des notifications d'emprunt DEMO ;</li>
 *     <li>la suppression des lignes rattachées aux deux prêts DEMO ;</li>
 *     <li>la suppression des deux prêts portant les marqueurs officiels ;</li>
 *     <li>la restauration sécurisée des objets libérés par ces prêts ;</li>
 *     <li>la création des deux prêts canoniques ;</li>
 *     <li>la création des trois lignes d'emprunt ;</li>
 *     <li>la mise en indisponibilité des trois objets canoniques ;</li>
 *     <li>la création des deux notifications canoniques.</li>
 * </ul>
 *
 * <p>Tous les comptes et objets requis sont résolus et validés avant la
 * première suppression. Les références catalogue doivent correspondre chacune
 * à un objet unique, actif et non supprimé logiquement.</p>
 *
 * <p>Les trois comptes canoniques et les objets du catalogue sont des données
 * socles permanentes. Ils ne sont jamais supprimés. Seuls les comptes portant
 * explicitement le marqueur temporaire
 * {@code RECRUITER_DEMO_CREATED_USERS}, ainsi que leurs dépendances, peuvent
 * être supprimés. Toutes les opérations destructives restent fondées sur les
 * marqueurs {@code demoScenarioCode} définis dans
 * {@code DemoScenarioCodes}.</p>
 *
 * <p>Lorsqu'un ancien prêt DEMO est supprimé, un objet associé n'est remis
 * disponible que s'il ne reste rattaché à aucune autre ligne d'emprunt active.
 * Cette vérification empêche de rendre disponible un objet encore emprunté par
 * un scénario réel.</p>
 */
@Service
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
@Transactional
public class DemoRelationalScenarioServiceImpl
        implements DemoRelationalScenarioService {

    private static final Logger logger =
            LoggerFactory.getLogger(DemoRelationalScenarioServiceImpl.class);

    private static final String ADMIN_EMAIL_PROPERTY =
            "ADMIN_EMAIL";

    private static final String ADMIN_PASSWORD_PROPERTY =
            "ADMIN_PASSWORD";

    private static final String DEFAULT_ADMIN_EMAIL =
            "admin@example.com";

    private static final String LUCAS_LOAN_NOTES =
            "Emprunt de démonstration actif pour Lucas.";

    private static final String SARAH_LOAN_NOTES =
            "Emprunt de démonstration en retard pour Sarah.";

    private static final String DEMO_LOAN_LINE_NOTES =
            "Ligne d'emprunt de démonstration.";

    private static final String LUCAS_NOTIFICATION_TITLE =
            "Emprunt en cours";

    private static final String LUCAS_NOTIFICATION_MESSAGE =
            "Votre emprunt de démonstration est actif. Pensez à restituer "
                    + "les objets avant la date d'échéance.";

    private static final String SARAH_NOTIFICATION_TITLE =
            "Emprunt en retard";

    private static final String SARAH_NOTIFICATION_MESSAGE =
            "Un emprunt de démonstration est en retard. Merci de vérifier "
                    + "la date de retour prévue.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ItemRepository itemRepository;
    private final LoanRepository loanRepository;
    private final LoanLineRepository loanLineRepository;
    private final NotificationRepository notificationRepository;
    private final Environment environment;
    private final PasswordEncoder passwordEncoder;

    /**
     * Initialise le service avec les repositories relationnels requis.
     *
     * @param userRepository repository des utilisateurs
     * @param roleRepository repository des rôles
     * @param itemRepository repository du catalogue
     * @param loanRepository repository des emprunts
     * @param loanLineRepository repository des lignes d'emprunt
     * @param notificationRepository repository des notifications
     * @param environment environnement Spring et propriétés actives
     * @param passwordEncoder encodeur BCrypt partagé par l'application
     */
    public DemoRelationalScenarioServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            ItemRepository itemRepository,
            LoanRepository loanRepository,
            LoanLineRepository loanLineRepository,
            NotificationRepository notificationRepository,
            Environment environment,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.itemRepository = itemRepository;
        this.loanRepository = loanRepository;
        this.loanLineRepository = loanLineRepository;
        this.notificationRepository = notificationRepository;
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebuild() {
        LocalDateTime initializationDate = LocalDateTime.now();

        String adminEmail = resolveAdminEmail();
        String adminPassword = resolveAdminPassword();

        User admin = resolveRequiredDemoUser(
                adminEmail,
                "Admin"
        );

        User lucas = resolveRequiredDemoUser(
                DemoScenarioDefinition.LUCAS_EMAIL,
                "Lucas"
        );

        User sarah = resolveRequiredDemoUser(
                DemoScenarioDefinition.SARAH_EMAIL,
                "Sarah"
        );

        Role adminRole = resolveRequiredRole(
                DemoScenarioDefinition.ADMIN_ROLE_LABEL
        );

        Role memberRole = resolveRequiredRole(
                DemoScenarioDefinition.MEMBER_ROLE_LABEL
        );

        Item lucasBookOne = resolveUniqueRequiredItem(
                DemoScenarioDefinition.LUCAS_BOOK_ONE_SOURCE_REF
        );

        Item lucasBookTwo = resolveUniqueRequiredItem(
                DemoScenarioDefinition.LUCAS_BOOK_TWO_SOURCE_REF
        );

        Item sarahDvd = resolveUniqueRequiredItem(
                DemoScenarioDefinition.SARAH_DVD_SOURCE_REF
        );

        validateDistinctRequiredItems(
                lucasBookOne,
                lucasBookTwo,
                sarahDvd
        );

        validateCanonicalItemsNotBorrowedOutsideDemo(
                lucasBookOne,
                lucasBookTwo,
                sarahDvd
        );

        List<User> temporaryUsers = userRepository.findByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_CREATED_USERS
        );

        List<Item> itemsPreviouslyLinkedToRecreatableLoans =
                collectItemsLinkedToRecreatableLoans(temporaryUsers);

        restoreDemoAccounts(
                admin,
                lucas,
                sarah,
                adminRole,
                memberRole,
                adminEmail,
                adminPassword
        );

        deleteRecreatableDemoData(temporaryUsers);

        restoreReleasedItems(itemsPreviouslyLinkedToRecreatableLoans);

        createCanonicalScenario(
                lucas,
                sarah,
                lucasBookOne,
                lucasBookTwo,
                sarahDvd,
                initializationDate
        );

        verifyPersistedScenario(
                admin,
                lucas,
                sarah,
                adminRole,
                memberRole,
                adminEmail,
                adminPassword
        );

        logger.info(
                "Scénarios relationnels DEMO reconstruits avec succès : "
                        + "3 comptes canoniques restaurés, {} compte(s) "
                        + "temporaire(s) supprimé(s), 2 prêts, 3 lignes et "
                        + "2 notifications.",
                temporaryUsers.size()
        );
    }

    // =========================================================================
    // VALIDATION DES DONNÉES SOCLES
    // =========================================================================

    /**
     * Résout et normalise l'adresse configurée du compte administrateur.
     *
     * @return adresse non vide utilisée comme identité canonique de l'admin
     */
    private String resolveAdminEmail() {
        String adminEmail = environment.getProperty(
                ADMIN_EMAIL_PROPERTY,
                DEFAULT_ADMIN_EMAIL
        );

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL est vide. Impossible de restaurer le compte "
                            + "administrateur DEMO."
            );
        }

        return adminEmail.trim();
    }

    /**
     * Résout le mot de passe canonique du compte administrateur DEMO.
     *
     * <p>La valeur est lue depuis {@code ADMIN_PASSWORD}. Elle n'est jamais
     * journalisée ni conservée en clair dans l'entité : seul son hash BCrypt
     * est persisté.</p>
     *
     * @return mot de passe administrateur non vide
     */
    private String resolveAdminPassword() {
        String adminPassword = environment.getProperty(
                ADMIN_PASSWORD_PROPERTY
        );

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD est vide ou absent. Impossible de "
                            + "restaurer le mot de passe administrateur DEMO."
            );
        }

        return adminPassword;
    }

    /**
     * Résout un rôle canonique requis pour restaurer les comptes socles.
     *
     * @param roleLabel libellé fonctionnel du rôle
     * @return rôle persistant correspondant
     */
    private Role resolveRequiredRole(String roleLabel) {
        return roleRepository.findByLabelRole(roleLabel)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle requis introuvable pour la reconstruction DEMO : "
                                + roleLabel + "."
                ));
    }

    /**
     * Résout un compte socle requis et vérifie son marqueur DEMO.
     *
     * <p>L'email sert uniquement à retrouver le compte socle attendu. Le
     * caractère DEMO est confirmé exclusivement par
     * {@code demoScenarioCode}.</p>
     *
     * @param email email du compte requis
     * @param displayName nom utilisé dans les diagnostics
     * @return compte DEMO validé
     */
    private User resolveRequiredDemoUser(
            String email,
            String displayName
    ) {
        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Compte requis introuvable pour la reconstruction "
                                + "relationnelle DEMO : "
                                + displayName + " (" + email + ")."
                ));

        if (!DemoScenarioCodes.RECRUITER_DEMO_USERS.equals(
                user.getDemoScenarioCode()
        )) {
            throw new IllegalStateException(
                    "Le compte " + displayName
                            + " existe, mais ne porte pas le marqueur "
                            + "officiel des comptes DEMO."
            );
        }

        return user;
    }

    /**
     * Résout un objet catalogue par sa référence source et vérifie que la
     * correspondance est unique.
     *
     * <p>La recherche est effectuée sur l'ensemble des objets afin de ne pas
     * masquer une duplication de {@code source_ref} par l'utilisation d'une
     * méthode {@code findFirst}.</p>
     *
     * @param sourceRef référence source attendue
     * @return objet catalogue unique et actif
     */
    private Item resolveUniqueRequiredItem(String sourceRef) {
        List<Item> matches = itemRepository.findAll().stream()
                .filter(item -> item.getDeletedDateItem() == null)
                .filter(item -> item.getTagsItem() != null)
                .filter(item -> item.getTagsItem().contains(sourceRef))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "Objet catalogue requis introuvable pour la "
                            + "reconstruction DEMO : " + sourceRef + "."
            );
        }

        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Référence catalogue ambiguë pour la reconstruction "
                            + "DEMO : " + sourceRef + " correspond à "
                            + matches.size() + " objets."
            );
        }

        Item item = matches.getFirst();

        if (item.getIdItem() == null) {
            throw new IllegalStateException(
                    "L'objet catalogue associé à " + sourceRef
                            + " ne possède aucun identifiant persistant."
            );
        }

        return item;
    }

    /**
     * Vérifie que les trois références canoniques ne désignent pas
     * accidentellement le même objet.
     *
     * @param items objets canoniques résolus
     */
    private void validateDistinctRequiredItems(Item... items) {
        long distinctItemCount = List.of(items).stream()
                .map(Item::getIdItem)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        if (distinctItemCount
                != DemoScenarioDefinition.EXPECTED_BORROWED_ITEM_COUNT) {
            throw new IllegalStateException(
                    "Les trois références catalogue DEMO ne correspondent "
                            + "pas à trois objets distincts."
            );
        }
    }

    /**
     * Vérifie qu'aucun des objets canoniques n'est déjà engagé dans un emprunt
     * actif extérieur aux deux scénarios DEMO reconstruits.
     *
     * <p>Une ligne appartenant à l'un des deux prêts DEMO officiels est
     * autorisée, car elle sera supprimée puis recréée pendant la même
     * transaction. Toute autre ligne active appartenant à un prêt non restitué
     * et non supprimé logiquement bloque la reconstruction afin d'éviter un
     * double emprunt.</p>
     *
     * @param items objets canoniques destinés aux prêts DEMO
     */
    private void validateCanonicalItemsNotBorrowedOutsideDemo(
            Item... items
    ) {
        Map<Integer, Item> canonicalItemsById = new LinkedHashMap<>();

        for (Item item : items) {
            canonicalItemsById.put(item.getIdItem(), item);
        }

        for (LoanLine loanLine : loanLineRepository.findAll()) {
            if (!LoanLineStatus.ACTIVE.equals(
                    loanLine.getStatusLoanLine()
            )) {
                continue;
            }

            Item linkedItem = loanLine.getItem();

            if (linkedItem == null
                    || linkedItem.getIdItem() == null
                    || !canonicalItemsById.containsKey(
                    linkedItem.getIdItem()
            )) {
                continue;
            }

            Loan parentLoan = loanLine.getLoan();

            if (parentLoan == null
                    || Boolean.TRUE.equals(parentLoan.getReturnedLoan())
                    || parentLoan.getDeletedDateLoan() != null
                    || isOfficialDemoLoan(parentLoan)) {
                continue;
            }

            throw new IllegalStateException(
                    "Reconstruction DEMO impossible : l'objet catalogue "
                            + linkedItem.getIdItem()
                            + " est déjà rattaché à un autre emprunt actif."
            );
        }
    }

    /**
     * Indique si un prêt appartient à l'un des deux scénarios DEMO officiels
     * reconstruits par ce service.
     *
     * @param loan prêt à contrôler
     * @return {@code true} lorsque le marqueur correspond à Lucas ou Sarah
     */
    private boolean isOfficialDemoLoan(Loan loan) {
        String scenarioCode = loan.getDemoScenarioCode();

        return DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN.equals(
                scenarioCode
        ) || DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN.equals(
                scenarioCode
        );
    }

    // =========================================================================
    // RESTAURATION DES COMPTES SOCLES
    // =========================================================================

    /**
     * Restaure intégralement l'identité fonctionnelle et les statuts des trois
     * comptes canoniques. Le mot de passe de l'administrateur est réencodé
     * depuis la configuration DEMO ; les mots de passe de Lucas et Sarah ainsi
     * que les dates d'inscription restent inchangés.
     *
     * @param admin compte administrateur
     * @param lucas compte Lucas
     * @param sarah compte Sarah
     * @param adminRole rôle ADMIN persistant
     * @param memberRole rôle MEMBRE persistant
     * @param adminEmail adresse canonique configurée de l'administrateur
     * @param adminPassword mot de passe canonique de l'administrateur
     */
    private void restoreDemoAccounts(
            User admin,
            User lucas,
            User sarah,
            Role adminRole,
            Role memberRole,
            String adminEmail,
            String adminPassword
    ) {
        restoreCanonicalAccount(
                admin,
                adminRole,
                DemoScenarioDefinition.ADMIN_CIVILITY,
                DemoScenarioDefinition.ADMIN_FIRST_NAME,
                DemoScenarioDefinition.ADMIN_LAST_NAME,
                adminEmail
        );

        admin.setPasswordUser(
                passwordEncoder.encode(adminPassword)
        );

        restoreCanonicalAccount(
                lucas,
                memberRole,
                DemoScenarioDefinition.LUCAS_CIVILITY,
                DemoScenarioDefinition.LUCAS_FIRST_NAME,
                DemoScenarioDefinition.LUCAS_LAST_NAME,
                DemoScenarioDefinition.LUCAS_EMAIL
        );

        restoreCanonicalAccount(
                sarah,
                memberRole,
                DemoScenarioDefinition.SARAH_CIVILITY,
                DemoScenarioDefinition.SARAH_FIRST_NAME,
                DemoScenarioDefinition.SARAH_LAST_NAME,
                DemoScenarioDefinition.SARAH_EMAIL
        );

        userRepository.saveAll(List.of(admin, lucas, sarah));
        userRepository.flush();
    }

    /**
     * Restaure un compte socle dans son état canonique tout en préservant son
     * mot de passe haché et sa date d'inscription initiale.
     *
     * <p>Pour l'administrateur, le mot de passe est remplacé séparément dans
     * {@link #restoreDemoAccounts(User, User, User, Role, Role, String, String)}
     * après cette restauration générique.</p>
     *
     * @param user compte à restaurer
     * @param role rôle canonique
     * @param civility civilité canonique
     * @param firstName prénom canonique
     * @param lastName nom canonique
     * @param email adresse canonique
     */
    private void restoreCanonicalAccount(
            User user,
            Role role,
            String civility,
            String firstName,
            String lastName,
            String email
    ) {
        user.setRole(role);
        user.setCivilityUser(civility);
        user.setFirstNameUser(firstName);
        user.setLastNameUser(lastName);
        user.setEmailUser(email);

        user.setActiveUser(Boolean.TRUE);
        user.setSubscriptionUser(Boolean.TRUE);
        user.setEmailVerifiedUser(Boolean.TRUE);
        user.setDepositUser(Boolean.TRUE);

        user.setEmailVerificationTokenUser(null);
        user.setPhoneUser(null);
        user.setAddressUser(null);
        user.setFfapMemberUser(null);
        user.setFfapNumberUser(null);
        user.setAssociationJoinDateUser(null);
        user.setResetTokenUser(null);
        user.setResetTokenExpireUser(null);
        user.setLastLoginUser(null);
        user.setUpdatedAtUser(null);
        user.setAvatarUser(null);
        user.setBioUser(null);
        user.setNotesUser(null);

        user.setDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_USERS
        );
    }

    // =========================================================================
    // COLLECTE ET SUPPRESSION DES DONNÉES RECRÉABLES
    // =========================================================================

    /**
     * Collecte les objets actuellement rattachés aux prêts DEMO officiels ainsi
     * qu'aux prêts appartenant aux comptes temporaires avant toute suppression.
     *
     * @param temporaryUsers comptes temporaires explicitement marqués
     * @return liste dédupliquée des objets potentiellement libérés
     */
    private List<Item> collectItemsLinkedToRecreatableLoans(
            List<User> temporaryUsers
    ) {
        Map<Integer, Item> itemsById = new LinkedHashMap<>();

        collectItemsLinkedToScenario(
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN,
                itemsById
        );

        collectItemsLinkedToScenario(
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN,
                itemsById
        );

        for (User temporaryUser : temporaryUsers) {
            for (Loan loan : loanRepository.findByUser(temporaryUser)) {
                if (loan.getIdLoan() == null) {
                    continue;
                }

                for (LoanLine loanLine
                        : loanLineRepository.findByLoan_IdLoan(
                        loan.getIdLoan()
                )) {
                    Item item = loanLine.getItem();

                    if (item != null && item.getIdItem() != null) {
                        itemsById.putIfAbsent(item.getIdItem(), item);
                    }
                }
            }
        }

        return List.copyOf(itemsById.values());
    }

    /**
     * Ajoute les objets rattachés à un scénario de prêt DEMO dans la collection
     * de travail.
     *
     * @param scenarioCode marqueur officiel du prêt
     * @param itemsById collection dédupliquée des objets
     */
    private void collectItemsLinkedToScenario(
            String scenarioCode,
            Map<Integer, Item> itemsById
    ) {
        List<LoanLine> loanLines =
                loanLineRepository.findByLoan_DemoScenarioCode(
                        scenarioCode
                );

        for (LoanLine loanLine : loanLines) {
            Item item = loanLine.getItem();

            if (item != null && item.getIdItem() != null) {
                itemsById.putIfAbsent(item.getIdItem(), item);
            }
        }
    }

    /**
     * Supprime uniquement les données relationnelles explicitement marquées
     * comme recréables.
     *
     * <p>L'ordre respecte strictement les dépendances relationnelles :</p>
     *
     * <ol>
     *     <li>notifications des comptes temporaires ;</li>
     *     <li>lignes de leurs prêts ;</li>
     *     <li>leurs prêts ;</li>
     *     <li>comptes temporaires explicitement marqués ;</li>
     *     <li>notifications, lignes et prêts des scénarios canoniques.</li>
     * </ol>
     *
     * @param temporaryUsers comptes temporaires à nettoyer
     */
    private void deleteRecreatableDemoData(List<User> temporaryUsers) {
        deleteTemporaryUsersAndDependencies(temporaryUsers);

        notificationRepository.deleteByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_LOAN_NOTIFICATIONS
        );
        notificationRepository.flush();

        loanLineRepository.deleteByLoan_DemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
        );

        loanLineRepository.deleteByLoan_DemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN
        );
        loanLineRepository.flush();

        loanRepository.deleteByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
        );

        loanRepository.deleteByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN
        );
        loanRepository.flush();
    }

    /**
     * Supprime les comptes temporaires et toutes leurs dépendances connues.
     *
     * <p>La sélection des comptes repose exclusivement sur le marqueur
     * {@code RECRUITER_DEMO_CREATED_USERS}. Aucun compte sans marqueur n'est
     * déduit comme temporaire et aucun compte socle n'est supprimé.</p>
     *
     * @param temporaryUsers comptes temporaires explicitement sélectionnés
     */
    private void deleteTemporaryUsersAndDependencies(
            List<User> temporaryUsers
    ) {
        if (temporaryUsers.isEmpty()) {
            return;
        }

        List<Notification> notificationsToDelete = new ArrayList<>();
        List<LoanLine> loanLinesToDelete = new ArrayList<>();
        List<Loan> loansToDelete = new ArrayList<>();

        for (User temporaryUser : temporaryUsers) {
            notificationsToDelete.addAll(
                    notificationRepository
                            .findByUserOrderByDateNotificationDesc(temporaryUser)
            );

            List<Loan> userLoans = loanRepository.findByUser(temporaryUser);
            loansToDelete.addAll(userLoans);

            for (Loan loan : userLoans) {
                if (loan.getIdLoan() != null) {
                    loanLinesToDelete.addAll(
                            loanLineRepository.findByLoan_IdLoan(
                                    loan.getIdLoan()
                            )
                    );
                }
            }
        }

        if (!notificationsToDelete.isEmpty()) {
            notificationRepository.deleteAll(notificationsToDelete);
            notificationRepository.flush();
        }

        if (!loanLinesToDelete.isEmpty()) {
            loanLineRepository.deleteAll(loanLinesToDelete);
            loanLineRepository.flush();
        }

        if (!loansToDelete.isEmpty()) {
            loanRepository.deleteAll(loansToDelete);
            loanRepository.flush();
        }

        userRepository.deleteAll(temporaryUsers);
        userRepository.flush();
    }

    // =========================================================================
    // RESTAURATION SÉCURISÉE DES OBJETS LIBÉRÉS
    // =========================================================================

    /**
     * Remet disponibles les objets libérés par les anciens prêts DEMO seulement
     * lorsqu'aucune autre ligne d'emprunt active ne les utilise encore.
     *
     * @param previouslyLinkedItems objets liés aux anciens prêts DEMO
     */
    private void restoreReleasedItems(
            List<Item> previouslyLinkedItems
    ) {
        if (previouslyLinkedItems.isEmpty()) {
            return;
        }

        List<LoanLine> remainingLoanLines =
                loanLineRepository.findAll();

        List<Item> itemsToRestore = new ArrayList<>();

        for (Item item : previouslyLinkedItems) {
            if (!isStillBorrowed(item, remainingLoanLines)) {
                markItemAvailable(item);
                itemsToRestore.add(item);
            }
        }

        if (!itemsToRestore.isEmpty()) {
            itemRepository.saveAll(itemsToRestore);
        }
    }

    /**
     * Détermine si un objet reste associé à une autre ligne active appartenant
     * à un prêt non restitué et non supprimé logiquement.
     *
     * @param item objet à contrôler
     * @param remainingLoanLines lignes restantes après nettoyage DEMO
     * @return {@code true} si l'objet est encore réellement emprunté
     */
    private boolean isStillBorrowed(
            Item item,
            List<LoanLine> remainingLoanLines
    ) {
        for (LoanLine loanLine : remainingLoanLines) {
            if (!LoanLineStatus.ACTIVE.equals(
                    loanLine.getStatusLoanLine()
            )) {
                continue;
            }

            Item linkedItem = loanLine.getItem();

            if (linkedItem == null
                    || linkedItem.getIdItem() == null
                    || !linkedItem.getIdItem().equals(item.getIdItem())) {
                continue;
            }

            Loan parentLoan = loanLine.getLoan();

            if (parentLoan != null
                    && !Boolean.TRUE.equals(parentLoan.getReturnedLoan())
                    && parentLoan.getDeletedDateLoan() == null) {
                return true;
            }
        }

        return false;
    }

    // =========================================================================
    // RECONSTRUCTION DU SCÉNARIO CANONIQUE
    // =========================================================================

    /**
     * Crée les deux prêts, leurs trois lignes, les états catalogue attendus et
     * les deux notifications.
     *
     * @param lucas compte Lucas
     * @param sarah compte Sarah
     * @param lucasBookOne premier livre de Lucas
     * @param lucasBookTwo second livre de Lucas
     * @param sarahDvd DVD de Sarah
     * @param initializationDate instant commun de reconstruction
     */
    private void createCanonicalScenario(
            User lucas,
            User sarah,
            Item lucasBookOne,
            Item lucasBookTwo,
            Item sarahDvd,
            LocalDateTime initializationDate
    ) {
        Loan lucasLoan = createLoan(
                lucas,
                initializationDate.minusDays(5),
                initializationDate.toLocalDate().plusDays(16),
                LoanStatus.ONGOING,
                Boolean.FALSE,
                Boolean.FALSE,
                LUCAS_LOAN_NOTES,
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
        );

        Loan sarahLoan = createLoan(
                sarah,
                initializationDate.minusDays(20),
                initializationDate.toLocalDate().minusDays(3),
                LoanStatus.LATE,
                Boolean.FALSE,
                Boolean.TRUE,
                SARAH_LOAN_NOTES,
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN
        );

        loanRepository.saveAll(List.of(lucasLoan, sarahLoan));

        LoanLine lucasBookOneLine = createLoanLine(
                lucasLoan,
                lucasBookOne,
                initializationDate
        );

        LoanLine lucasBookTwoLine = createLoanLine(
                lucasLoan,
                lucasBookTwo,
                initializationDate
        );

        LoanLine sarahDvdLine = createLoanLine(
                sarahLoan,
                sarahDvd,
                initializationDate
        );

        loanLineRepository.saveAll(
                List.of(
                        lucasBookOneLine,
                        lucasBookTwoLine,
                        sarahDvdLine
                )
        );

        markItemUnavailable(lucasBookOne, initializationDate);
        markItemUnavailable(lucasBookTwo, initializationDate);
        markItemUnavailable(sarahDvd, initializationDate);

        itemRepository.saveAll(
                List.of(
                        lucasBookOne,
                        lucasBookTwo,
                        sarahDvd
                )
        );

        Notification lucasNotification = createNotification(
                lucas,
                LUCAS_NOTIFICATION_TITLE,
                LUCAS_NOTIFICATION_MESSAGE,
                NotificationType.REMINDER,
                NotificationCategory.RAPPEL,
                DemoScenarioDefinition.LUCAS_NOTIFICATION_PRIORITY
        );

        Notification sarahNotification = createNotification(
                sarah,
                SARAH_NOTIFICATION_TITLE,
                SARAH_NOTIFICATION_MESSAGE,
                NotificationType.OVERDUE,
                NotificationCategory.RAPPEL,
                DemoScenarioDefinition.SARAH_NOTIFICATION_PRIORITY
        );

        notificationRepository.saveAll(
                List.of(
                        lucasNotification,
                        sarahNotification
                )
        );
    }

    /**
     * Construit un prêt DEMO complet.
     *
     * @param user propriétaire du prêt
     * @param startDate date et heure de début
     * @param dueDate date d'échéance
     * @param status statut métier
     * @param returned indicateur de restitution
     * @param overdue indicateur de retard
     * @param notes notes lisibles
     * @param scenarioCode marqueur officiel du scénario
     * @return prêt prêt à être persisté
     */
    private Loan createLoan(
            User user,
            LocalDateTime startDate,
            LocalDate dueDate,
            LoanStatus status,
            Boolean returned,
            Boolean overdue,
            String notes,
            String scenarioCode
    ) {
        Loan loan = new Loan(
                user,
                startDate,
                dueDate,
                returned,
                overdue,
                Boolean.FALSE,
                0,
                status,
                DemoScenarioDefinition.LOAN_ORIGIN_SYSTEM
        );

        loan.setReturnDateLoan(null);
        loan.setDeletedDateLoan(null);
        loan.setNotesLoan(notes);
        loan.setDemoScenarioCode(scenarioCode);

        return loan;
    }

    /**
     * Construit une ligne d'emprunt active.
     *
     * @param loan prêt parent
     * @param item objet emprunté
     * @param initializationDate instant commun de reconstruction
     * @return ligne prête à être persistée
     */
    private LoanLine createLoanLine(
            Loan loan,
            Item item,
            LocalDateTime initializationDate
    ) {
        LoanLine loanLine = new LoanLine(
                loan,
                item,
                1,
                LoanLineStatus.ACTIVE,
                initializationDate
        );

        loanLine.setNotesLoanLine(DEMO_LOAN_LINE_NOTES);

        return loanLine;
    }

    /**
     * Construit une notification d'emprunt DEMO.
     *
     * @param user destinataire
     * @param title titre
     * @param message message
     * @param type type technique
     * @param category catégorie fonctionnelle
     * @param priority priorité
     * @return notification prête à être persistée
     */
    private Notification createNotification(
            User user,
            String title,
            String message,
            NotificationType type,
            NotificationCategory category,
            String priority
    ) {
        Notification notification = new Notification(
                user,
                title,
                message,
                DemoScenarioDefinition.LOAN_NOTIFICATION_ACTION_URL,
                type,
                category,
                priority
        );

        notification.setDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_LOAN_NOTIFICATIONS
        );

        return notification;
    }

    /**
     * Rend un objet indisponible pour le scénario canonique.
     *
     * @param item objet à modifier
     * @param initializationDate instant commun de reconstruction
     */
    private void markItemUnavailable(
            Item item,
            LocalDateTime initializationDate
    ) {
        item.setAvailableItem(Boolean.FALSE);
        item.setStatusItem(ItemStatus.UNAVAILABLE);
        item.setUpdatedAtItem(initializationDate);
    }

    /**
     * Rend un objet disponible après libération sécurisée.
     *
     * @param item objet à modifier
     */
    private void markItemAvailable(Item item) {
        item.setAvailableItem(Boolean.TRUE);
        item.setStatusItem(ItemStatus.AVAILABLE);
        item.setUpdatedAtItem(LocalDateTime.now());
    }

    // =========================================================================
    // VÉRIFICATION APRÈS PERSISTANCE
    // =========================================================================

    /**
     * Vérifie les comptes canoniques et les cardinalités essentielles après
     * reconstruction.
     *
     * @param admin compte administrateur restauré
     * @param lucas compte Lucas restauré
     * @param sarah compte Sarah restauré
     * @param adminRole rôle ADMIN attendu
     * @param memberRole rôle MEMBRE attendu
     * @param adminEmail adresse canonique de l'administrateur
     * @param adminPassword mot de passe administrateur attendu en clair
     */
    private void verifyPersistedScenario(
            User admin,
            User lucas,
            User sarah,
            Role adminRole,
            Role memberRole,
            String adminEmail,
            String adminPassword
    ) {
        verifyCanonicalUser(
                admin,
                adminRole,
                DemoScenarioDefinition.ADMIN_CIVILITY,
                DemoScenarioDefinition.ADMIN_FIRST_NAME,
                DemoScenarioDefinition.ADMIN_LAST_NAME,
                adminEmail,
                "Admin"
        );

        if (!passwordEncoder.matches(
                adminPassword,
                admin.getPasswordUser()
        )) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète : le mot de passe "
                            + "administrateur n'a pas été restauré."
            );
        }

        verifyCanonicalUser(
                lucas,
                memberRole,
                DemoScenarioDefinition.LUCAS_CIVILITY,
                DemoScenarioDefinition.LUCAS_FIRST_NAME,
                DemoScenarioDefinition.LUCAS_LAST_NAME,
                DemoScenarioDefinition.LUCAS_EMAIL,
                "Lucas"
        );

        verifyCanonicalUser(
                sarah,
                memberRole,
                DemoScenarioDefinition.SARAH_CIVILITY,
                DemoScenarioDefinition.SARAH_FIRST_NAME,
                DemoScenarioDefinition.SARAH_LAST_NAME,
                DemoScenarioDefinition.SARAH_EMAIL,
                "Sarah"
        );

        long remainingTemporaryUserCount =
                userRepository.countByDemoScenarioCode(
                        DemoScenarioCodes.RECRUITER_DEMO_CREATED_USERS
                );

        if (remainingTemporaryUserCount != 0) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète : "
                            + remainingTemporaryUserCount
                            + " compte(s) temporaire(s) subsiste(nt)."
            );
        }

        verifyScenarioCount(
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN,
                1
        );

        verifyScenarioCount(
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN,
                1
        );

        int lucasLineCount = loanLineRepository
                .findByLoan_DemoScenarioCode(
                        DemoScenarioCodes
                                .RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
                )
                .size();

        int sarahLineCount = loanLineRepository
                .findByLoan_DemoScenarioCode(
                        DemoScenarioCodes
                                .RECRUITER_DEMO_SARAH_OVERDUE_LOAN
                )
                .size();

        if (lucasLineCount
                != DemoScenarioDefinition.LUCAS_EXPECTED_LOAN_LINE_COUNT) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète : "
                            + lucasLineCount
                            + " ligne(s) trouvée(s) pour Lucas."
            );
        }

        if (sarahLineCount
                != DemoScenarioDefinition.SARAH_EXPECTED_LOAN_LINE_COUNT) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète : "
                            + sarahLineCount
                            + " ligne(s) trouvée(s) pour Sarah."
            );
        }

        long notificationCount =
                notificationRepository.countByDemoScenarioCode(
                        DemoScenarioCodes
                                .RECRUITER_DEMO_LOAN_NOTIFICATIONS
                );

        if (notificationCount
                != DemoScenarioDefinition
                .EXPECTED_LOAN_NOTIFICATION_COUNT) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète : "
                            + notificationCount
                            + " notification(s) trouvée(s) au lieu de "
                            + DemoScenarioDefinition
                            .EXPECTED_LOAN_NOTIFICATION_COUNT
                            + "."
            );
        }
    }

    /**
     * Vérifie l'identité, le rôle et les statuts d'un compte canonique.
     *
     * @param user compte contrôlé
     * @param expectedRole rôle attendu
     * @param expectedCivility civilité attendue
     * @param expectedFirstName prénom attendu
     * @param expectedLastName nom attendu
     * @param expectedEmail email attendu
     * @param displayName nom utilisé dans les diagnostics
     */
    private void verifyCanonicalUser(
            User user,
            Role expectedRole,
            String expectedCivility,
            String expectedFirstName,
            String expectedLastName,
            String expectedEmail,
            String displayName
    ) {
        boolean valid = Objects.equals(user.getRole(), expectedRole)
                && Objects.equals(user.getCivilityUser(), expectedCivility)
                && Objects.equals(user.getFirstNameUser(), expectedFirstName)
                && Objects.equals(user.getLastNameUser(), expectedLastName)
                && Objects.equals(user.getEmailUser(), expectedEmail)
                && Boolean.TRUE.equals(user.getActiveUser())
                && Boolean.TRUE.equals(user.getSubscriptionUser())
                && Boolean.TRUE.equals(user.getEmailVerifiedUser())
                && Boolean.TRUE.equals(user.getDepositUser())
                && DemoScenarioCodes.RECRUITER_DEMO_USERS.equals(
                user.getDemoScenarioCode()
        );

        if (!valid) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète : le compte canonique "
                            + displayName + " n'a pas été restauré."
            );
        }
    }

    /**
     * Vérifie le nombre de prêts associé à un marqueur officiel.
     *
     * @param scenarioCode marqueur du scénario
     * @param expectedCount nombre attendu
     */
    private void verifyScenarioCount(
            String scenarioCode,
            long expectedCount
    ) {
        long actualCount =
                loanRepository.countByDemoScenarioCode(scenarioCode);

        if (actualCount != expectedCount) {
            throw new IllegalStateException(
                    "Reconstruction DEMO incomplète pour le scénario "
                            + scenarioCode + " : "
                            + actualCount + " prêt(s) trouvé(s) au lieu de "
                            + expectedCount + "."
            );
        }
    }
}