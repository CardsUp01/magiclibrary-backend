package com.magiclibrary.services.demo.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.magiclibrary.entities.Item;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ContactStatus;
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.LoanLineStatus;
import com.magiclibrary.enums.LoanStatus;
import com.magiclibrary.init.DemoScenarioCodes;
import com.magiclibrary.mongo.documents.ContactDocument;
import com.magiclibrary.mongo.repositories.ContactMongoRepository;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.repositories.interfaces.LoanLineRepository;
import com.magiclibrary.repositories.interfaces.LoanRepository;
import com.magiclibrary.repositories.interfaces.NotificationRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.demo.DemoScenarioDefinition;
import com.magiclibrary.services.demo.DemoScenarioHealthReport;
import com.magiclibrary.services.demo.DemoScenarioHealthService;

/**
 * Contrôle l'état canonique complet des scénarios de démonstration.
 *
 * <p>Cette implémentation est strictement diagnostique. Elle consulte les
 * données MariaDB et MongoDB sans réaliser aucune suppression, correction ou
 * reconstruction.</p>
 *
 * <p>Le contrôle couvre cinq périmètres :</p>
 *
 * <ul>
 *     <li>les trois comptes socles Admin, Lucas et Sarah, ainsi que
 *         l'absence de comptes temporaires DEMO résiduels ;</li>
 *     <li>les deux prêts DEMO et leurs lignes ;</li>
 *     <li>les trois objets du catalogue associés aux prêts ;</li>
 *     <li>les notifications relationnelles ;</li>
 *     <li>les huit messages du module CONTACT.</li>
 * </ul>
 *
 * <p>Chaque anomalie détectée est ajoutée au rapport afin de permettre au
 * scheduler, aux journaux applicatifs et aux tests automatisés d'identifier
 * précisément la cause d'une dégradation.</p>
 *
 * <p>Le chargement du service exige deux protections cumulatives :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} doit être actif ;</li>
 *     <li>la propriété {@code magiclibrary.demo.reset.enabled} doit valoir
 *     explicitement {@code true}.</li>
 * </ul>
 */
@Service
@Profile("demo")
@ConditionalOnProperty(
        prefix = "magiclibrary.demo.reset",
        name = "enabled",
        havingValue = "true"
)
@Transactional(readOnly = true)
public class DemoScenarioHealthServiceImpl implements DemoScenarioHealthService {

    private static final String DEFAULT_ADMIN_EMAIL =
            "admin@example.com";

    private static final String ADMIN_EMAIL_PROPERTY =
            "ADMIN_EMAIL";

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final LoanRepository loanRepository;
    private final LoanLineRepository loanLineRepository;
    private final NotificationRepository notificationRepository;
    private final ContactMongoRepository contactMongoRepository;
    private final Environment environment;

    /**
     * Initialise le service avec les repositories nécessaires au diagnostic.
     *
     * @param userRepository repository des comptes relationnels
     * @param itemRepository repository du catalogue
     * @param loanRepository repository des emprunts
     * @param loanLineRepository repository des lignes d'emprunt
     * @param notificationRepository repository des notifications
     * @param contactMongoRepository repository MongoDB du module CONTACT
     * @param environment environnement Spring utilisé pour résoudre ADMIN_EMAIL
     */
    public DemoScenarioHealthServiceImpl(
            UserRepository userRepository,
            ItemRepository itemRepository,
            LoanRepository loanRepository,
            LoanLineRepository loanLineRepository,
            NotificationRepository notificationRepository,
            ContactMongoRepository contactMongoRepository,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.loanRepository = loanRepository;
        this.loanLineRepository = loanLineRepository;
        this.notificationRepository = notificationRepository;
        this.contactMongoRepository = contactMongoRepository;
        this.environment = environment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DemoScenarioHealthReport checkHealth() {
        List<String> anomalies = new ArrayList<>();

        boolean usersHealthy = checkUsers(anomalies);
        boolean loansHealthy = checkLoansAndItems(anomalies);
        boolean notificationsHealthy = checkNotifications(anomalies);
        boolean contactsHealthy = checkContacts(anomalies);

        boolean healthy = usersHealthy
                && loansHealthy
                && notificationsHealthy
                && contactsHealthy;

        return new DemoScenarioHealthReport(
                healthy,
                usersHealthy,
                loansHealthy,
                notificationsHealthy,
                contactsHealthy,
                anomalies,
                Instant.now()
        );
    }

    // =========================================================================
    // COMPTES SOCLES
    // =========================================================================

    /**
     * Vérifie l'état canonique complet des trois comptes socles du scénario
     * recruteur et l'absence de comptes temporaires DEMO résiduels.
     *
     * <p>Le contrôle porte sur l'identité, la civilité, le rôle, les quatre
     * indicateurs fonctionnels et le marqueur technique officiel. Le mot de
     * passe, les dates et les données facultatives de profil ne sont pas
     * contrôlés, car la reconstruction les conserve volontairement.</p>
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si les comptes DEMO sont intégralement conformes
     */
    private boolean checkUsers(List<String> anomalies) {
        String adminEmail = resolveAdminEmail();

        boolean adminHealthy = checkCanonicalUser(
                adminEmail,
                DemoScenarioDefinition.ADMIN_DISPLAY_NAME,
                DemoScenarioDefinition.ADMIN_CIVILITY,
                DemoScenarioDefinition.ADMIN_FIRST_NAME,
                DemoScenarioDefinition.ADMIN_LAST_NAME,
                DemoScenarioDefinition.ADMIN_ROLE_LABEL,
                anomalies
        );

        boolean lucasHealthy = checkCanonicalUser(
                DemoScenarioDefinition.LUCAS_EMAIL,
                DemoScenarioDefinition.LUCAS_DISPLAY_NAME,
                DemoScenarioDefinition.LUCAS_CIVILITY,
                DemoScenarioDefinition.LUCAS_FIRST_NAME,
                DemoScenarioDefinition.LUCAS_LAST_NAME,
                DemoScenarioDefinition.MEMBER_ROLE_LABEL,
                anomalies
        );

        boolean sarahHealthy = checkCanonicalUser(
                DemoScenarioDefinition.SARAH_EMAIL,
                DemoScenarioDefinition.SARAH_DISPLAY_NAME,
                DemoScenarioDefinition.SARAH_CIVILITY,
                DemoScenarioDefinition.SARAH_FIRST_NAME,
                DemoScenarioDefinition.SARAH_LAST_NAME,
                DemoScenarioDefinition.MEMBER_ROLE_LABEL,
                anomalies
        );

        long canonicalUserCount = userRepository.countByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_USERS
        );

        boolean canonicalCountHealthy =
                canonicalUserCount
                        == DemoScenarioDefinition.EXPECTED_CANONICAL_USER_COUNT;

        if (!canonicalCountHealthy) {
            anomalies.add(
                    "Le scénario recruteur doit contenir exactement "
                            + DemoScenarioDefinition.EXPECTED_CANONICAL_USER_COUNT
                            + " comptes socles DEMO, "
                            + canonicalUserCount + " trouvé(s)."
            );
        }

        long temporaryUserCount = userRepository.countByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_CREATED_USERS
        );

        boolean temporaryUsersHealthy = temporaryUserCount == 0L;

        if (!temporaryUsersHealthy) {
            anomalies.add(
                    "Des comptes temporaires DEMO subsistent après "
                            + "réinitialisation : "
                            + temporaryUserCount + " compte(s) trouvé(s)."
            );
        }

        return adminHealthy
                && lucasHealthy
                && sarahHealthy
                && canonicalCountHealthy
                && temporaryUsersHealthy;
    }

    /**
     * Résout l'adresse administrateur configurée pour l'instance courante.
     *
     * <p>La valeur par défaut reste strictement identique à celle utilisée par
     * {@code UserInitializer}. Une valeur vide rend le diagnostic non fiable et
     * provoque donc une erreur explicite.</p>
     *
     * @return email administrateur normalisé et non vide
     */
    private String resolveAdminEmail() {
        String adminEmail = environment.getProperty(
                ADMIN_EMAIL_PROPERTY,
                DEFAULT_ADMIN_EMAIL
        );

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL est vide. Impossible de contrôler le compte "
                            + "administrateur DEMO."
            );
        }

        return adminEmail.trim();
    }

    /**
     * Vérifie l'intégralité des propriétés canoniques d'un compte socle DEMO.
     *
     * @param email email stable permettant de retrouver le compte
     * @param displayName nom utilisé dans les diagnostics
     * @param expectedCivility civilité canonique attendue
     * @param expectedFirstName prénom canonique attendu
     * @param expectedLastName nom canonique attendu
     * @param expectedRoleLabel libellé du rôle canonique attendu
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si le compte est intégralement conforme
     */
    private boolean checkCanonicalUser(
            String email,
            String displayName,
            String expectedCivility,
            String expectedFirstName,
            String expectedLastName,
            String expectedRoleLabel,
            List<String> anomalies
    ) {
        Optional<User> userOptional = userRepository.findByEmailUser(email);

        if (userOptional.isEmpty()) {
            anomalies.add(
                    "Compte DEMO absent : " + displayName + " (" + email + ")."
            );
            return false;
        }

        User user = userOptional.get();
        boolean healthy = true;

        if (!DemoScenarioCodes.RECRUITER_DEMO_USERS.equals(
                user.getDemoScenarioCode()
        )) {
            anomalies.add(
                    "Compte DEMO non marqué correctement : "
                            + displayName + " (" + email + ")."
            );
            healthy = false;
        }

        if (!Objects.equals(expectedCivility, user.getCivilityUser())) {
            anomalies.add(
                    "La civilité du compte DEMO " + displayName
                            + " ne correspond pas à l'état canonique."
            );
            healthy = false;
        }

        if (!Objects.equals(expectedFirstName, user.getFirstNameUser())) {
            anomalies.add(
                    "Le prénom du compte DEMO " + displayName
                            + " ne correspond pas à l'état canonique."
            );
            healthy = false;
        }

        if (!Objects.equals(expectedLastName, user.getLastNameUser())) {
            anomalies.add(
                    "Le nom du compte DEMO " + displayName
                            + " ne correspond pas à l'état canonique."
            );
            healthy = false;
        }

        if (user.getRole() == null
                || !Objects.equals(
                expectedRoleLabel,
                user.getRole().getLabelRole()
        )) {
            anomalies.add(
                    "Le rôle du compte DEMO " + displayName
                            + " ne correspond pas à l'état canonique."
            );
            healthy = false;
        }

        if (!Boolean.TRUE.equals(user.getActiveUser())) {
            anomalies.add(
                    "Le compte DEMO de " + displayName + " n'est pas actif."
            );
            healthy = false;
        }

        if (!Boolean.TRUE.equals(user.getSubscriptionUser())) {
            anomalies.add(
                    "Le compte DEMO de " + displayName
                            + " ne possède pas une cotisation active."
            );
            healthy = false;
        }

        if (!Boolean.TRUE.equals(user.getEmailVerifiedUser())) {
            anomalies.add(
                    "L'adresse email du compte DEMO de " + displayName
                            + " n'est pas vérifiée."
            );
            healthy = false;
        }

        if (!Boolean.TRUE.equals(user.getDepositUser())) {
            anomalies.add(
                    "La caution du compte DEMO de " + displayName
                            + " n'est pas validée."
            );
            healthy = false;
        }

        return healthy;
    }

    // =========================================================================
    // EMPRUNTS, LIGNES ET OBJETS
    // =========================================================================

    /**
     * Vérifie les deux prêts canoniques ainsi que les objets catalogue associés.
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si les prêts, lignes et objets sont conformes
     */
    private boolean checkLoansAndItems(List<String> anomalies) {
        boolean lucasLoanHealthy = checkLucasLoan(anomalies);
        boolean sarahLoanHealthy = checkSarahLoan(anomalies);
        boolean itemsHealthy = checkRequiredItems(anomalies);

        return lucasLoanHealthy && sarahLoanHealthy && itemsHealthy;
    }

    /**
     * Vérifie le prêt actif de Lucas.
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si le scénario d'emprunt Lucas est conforme
     */
    private boolean checkLucasLoan(List<String> anomalies) {
        List<Loan> loans = loanRepository.findByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
        );

        if (loans.size() != 1) {
            anomalies.add(
                    "Le scénario Lucas doit contenir exactement un prêt, "
                            + loans.size() + " trouvé(s)."
            );
            return false;
        }

        Loan loan = loans.getFirst();
        boolean healthy = true;

        if (loan.getUser() == null
                || !DemoScenarioDefinition.LUCAS_EMAIL.equals(
                loan.getUser().getEmailUser()
        )) {
            anomalies.add(
                    "Le prêt DEMO de Lucas n'est pas rattaché au compte attendu."
            );
            healthy = false;
        }

        if (!LoanStatus.ONGOING.equals(loan.getStatusLoan())) {
            anomalies.add(
                    "Le prêt DEMO de Lucas n'est pas au statut ONGOING."
            );
            healthy = false;
        }

        if (!Boolean.FALSE.equals(loan.getReturnedLoan())) {
            anomalies.add(
                    "Le prêt DEMO de Lucas est marqué comme restitué."
            );
            healthy = false;
        }

        if (!Boolean.FALSE.equals(loan.getOverdueLoan())) {
            anomalies.add(
                    "Le prêt DEMO de Lucas est incorrectement marqué en retard."
            );
            healthy = false;
        }

        if (loan.getReturnDateLoan() != null) {
            anomalies.add(
                    "Le prêt DEMO de Lucas possède une date de restitution."
            );
            healthy = false;
        }

        if (loan.getDeletedDateLoan() != null) {
            anomalies.add(
                    "Le prêt DEMO de Lucas est supprimé logiquement."
            );
            healthy = false;
        }

        if (!DemoScenarioDefinition.LOAN_ORIGIN_SYSTEM.equals(
                loan.getOriginLoan()
        )) {
            anomalies.add(
                    "Le prêt DEMO de Lucas ne possède pas l'origine SYSTEM."
            );
            healthy = false;
        }

        boolean linesHealthy = checkLoanLines(
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN,
                DemoScenarioDefinition.LUCAS_EXPECTED_LOAN_LINE_COUNT,
                Set.of(
                        DemoScenarioDefinition.LUCAS_BOOK_ONE_SOURCE_REF,
                        DemoScenarioDefinition.LUCAS_BOOK_TWO_SOURCE_REF
                ),
                "Lucas",
                anomalies
        );

        return healthy && linesHealthy;
    }

    /**
     * Vérifie le prêt en retard de Sarah.
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si le scénario d'emprunt Sarah est conforme
     */
    private boolean checkSarahLoan(List<String> anomalies) {
        List<Loan> loans = loanRepository.findByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN
        );

        if (loans.size() != 1) {
            anomalies.add(
                    "Le scénario Sarah doit contenir exactement un prêt, "
                            + loans.size() + " trouvé(s)."
            );
            return false;
        }

        Loan loan = loans.getFirst();
        boolean healthy = true;

        if (loan.getUser() == null
                || !DemoScenarioDefinition.SARAH_EMAIL.equals(
                loan.getUser().getEmailUser()
        )) {
            anomalies.add(
                    "Le prêt DEMO de Sarah n'est pas rattaché au compte attendu."
            );
            healthy = false;
        }

        if (!LoanStatus.LATE.equals(loan.getStatusLoan())) {
            anomalies.add(
                    "Le prêt DEMO de Sarah n'est pas au statut LATE."
            );
            healthy = false;
        }

        if (!Boolean.FALSE.equals(loan.getReturnedLoan())) {
            anomalies.add(
                    "Le prêt DEMO de Sarah est marqué comme restitué."
            );
            healthy = false;
        }

        if (!Boolean.TRUE.equals(loan.getOverdueLoan())) {
            anomalies.add(
                    "Le prêt DEMO de Sarah n'est pas marqué en retard."
            );
            healthy = false;
        }

        if (loan.getReturnDateLoan() != null) {
            anomalies.add(
                    "Le prêt DEMO de Sarah possède une date de restitution."
            );
            healthy = false;
        }

        if (loan.getDeletedDateLoan() != null) {
            anomalies.add(
                    "Le prêt DEMO de Sarah est supprimé logiquement."
            );
            healthy = false;
        }

        if (!DemoScenarioDefinition.LOAN_ORIGIN_SYSTEM.equals(
                loan.getOriginLoan()
        )) {
            anomalies.add(
                    "Le prêt DEMO de Sarah ne possède pas l'origine SYSTEM."
            );
            healthy = false;
        }

        boolean linesHealthy = checkLoanLines(
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN,
                DemoScenarioDefinition.SARAH_EXPECTED_LOAN_LINE_COUNT,
                Set.of(DemoScenarioDefinition.SARAH_DVD_SOURCE_REF),
                "Sarah",
                anomalies
        );

        return healthy && linesHealthy;
    }

    /**
     * Vérifie le nombre, le statut et les objets associés aux lignes d'un prêt
     * DEMO.
     *
     * @param scenarioCode marqueur technique du prêt DEMO
     * @param expectedLineCount nombre de lignes attendu
     * @param expectedSourceRefs références catalogue attendues
     * @param scenarioName nom utilisé dans les diagnostics
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si les lignes du prêt sont conformes
     */
    private boolean checkLoanLines(
            String scenarioCode,
            int expectedLineCount,
            Set<String> expectedSourceRefs,
            String scenarioName,
            List<String> anomalies
    ) {
        List<LoanLine> loanLines =
                loanLineRepository.findByLoan_DemoScenarioCode(scenarioCode);

        boolean healthy = true;

        if (loanLines.size() != expectedLineCount) {
            anomalies.add(
                    "Le prêt DEMO de " + scenarioName + " doit contenir "
                            + expectedLineCount + " ligne(s), "
                            + loanLines.size() + " trouvée(s)."
            );
            healthy = false;
        }

        for (LoanLine loanLine : loanLines) {
            if (!LoanLineStatus.ACTIVE.equals(
                    loanLine.getStatusLoanLine()
            )) {
                anomalies.add(
                        "Une ligne du prêt DEMO de " + scenarioName
                                + " n'est pas au statut ACTIVE."
                );
                healthy = false;
            }

            if (loanLine.getItem() == null) {
                anomalies.add(
                        "Une ligne du prêt DEMO de " + scenarioName
                                + " ne référence aucun objet."
                );
                healthy = false;
            }
        }

        Set<Integer> actualItemIds = new HashSet<>();

        for (LoanLine loanLine : loanLines) {
            if (loanLine.getItem() != null
                    && loanLine.getItem().getIdItem() != null) {
                actualItemIds.add(loanLine.getItem().getIdItem());
            }
        }

        for (String sourceRef : expectedSourceRefs) {
            Optional<Item> itemOptional = resolveUniqueRequiredItem(
                    sourceRef,
                    "le scénario " + scenarioName,
                    anomalies
            );

            if (itemOptional.isEmpty()) {
                healthy = false;
                continue;
            }

            Item item = itemOptional.get();

            if (!actualItemIds.contains(item.getIdItem())) {
                anomalies.add(
                        "L'objet " + sourceRef
                                + " n'est pas rattaché au prêt DEMO de "
                                + scenarioName + "."
                );
                healthy = false;
            }
        }

        return healthy;
    }

    /**
     * Vérifie que les trois objets canoniques sont présents et indisponibles.
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si les trois objets sont conformes
     */
    private boolean checkRequiredItems(List<String> anomalies) {
        boolean healthy = true;

        for (String sourceRef
                : DemoScenarioDefinition.REQUIRED_ITEM_SOURCE_REFS) {

            Optional<Item> itemOptional = resolveUniqueRequiredItem(
                    sourceRef,
                    "le contrôle global des objets DEMO",
                    anomalies
            );

            if (itemOptional.isEmpty()) {
                healthy = false;
                continue;
            }

            Item item = itemOptional.get();

            if (!ItemStatus.UNAVAILABLE.equals(item.getStatusItem())) {
                anomalies.add(
                        "L'objet " + sourceRef
                                + " n'est pas au statut UNAVAILABLE."
                );
                healthy = false;
            }

            if (!Boolean.FALSE.equals(item.getAvailableItem())) {
                anomalies.add(
                        "L'objet " + sourceRef
                                + " est incorrectement marqué disponible."
                );
                healthy = false;
            }
        }

        return healthy;
    }

    /**
     * Résout une référence catalogue active et vérifie qu'elle correspond à un
     * seul objet.
     *
     * <p>Le diagnostic ne doit pas masquer une duplication de référence par
     * l'utilisation d'une recherche {@code findFirst}. Une référence absente
     * ou ambiguë rend donc le scénario non conforme.</p>
     *
     * @param sourceRef référence catalogue attendue
     * @param context contexte fonctionnel ajouté au diagnostic
     * @param anomalies collection recevant les anomalies détectées
     * @return objet unique lorsqu'il existe, sinon {@link Optional#empty()}
     */
    private Optional<Item> resolveUniqueRequiredItem(
            String sourceRef,
            String context,
            List<String> anomalies
    ) {
        List<Item> matches = itemRepository.findAll().stream()
                .filter(item -> item.getDeletedDateItem() == null)
                .filter(item -> item.getTagsItem() != null)
                .filter(item -> item.getTagsItem().contains(sourceRef))
                .toList();

        if (matches.isEmpty()) {
            anomalies.add(
                    "Objet catalogue requis introuvable pour "
                            + context + " : " + sourceRef + "."
            );
            return Optional.empty();
        }

        if (matches.size() > 1) {
            anomalies.add(
                    "Référence catalogue ambiguë pour "
                            + context + " : " + sourceRef
                            + " correspond à " + matches.size() + " objets."
            );
            return Optional.empty();
        }

        Item item = matches.getFirst();

        if (item.getIdItem() == null) {
            anomalies.add(
                    "L'objet catalogue associé à " + sourceRef
                            + " ne possède aucun identifiant persistant."
            );
            return Optional.empty();
        }

        return Optional.of(item);
    }

    // =========================================================================
    // NOTIFICATIONS
    // =========================================================================

    /**
     * Vérifie le nombre total de notifications et leur répartition entre Lucas
     * et Sarah.
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si les notifications DEMO sont conformes
     */
    private boolean checkNotifications(List<String> anomalies) {
        boolean healthy = true;

        long totalCount = notificationRepository.countByDemoScenarioCode(
                DemoScenarioCodes.RECRUITER_DEMO_LOAN_NOTIFICATIONS
        );

        if (totalCount
                != DemoScenarioDefinition.EXPECTED_LOAN_NOTIFICATION_COUNT) {
            anomalies.add(
                    "Le scénario doit contenir exactement "
                            + DemoScenarioDefinition
                            .EXPECTED_LOAN_NOTIFICATION_COUNT
                            + " notifications DEMO, "
                            + totalCount + " trouvée(s)."
            );
            healthy = false;
        }

        Optional<User> lucasOptional = userRepository.findByEmailUser(
                DemoScenarioDefinition.LUCAS_EMAIL
        );

        Optional<User> sarahOptional = userRepository.findByEmailUser(
                DemoScenarioDefinition.SARAH_EMAIL
        );

        if (lucasOptional.isPresent()) {
            int lucasNotificationCount =
                    notificationRepository.findByUserAndDemoScenarioCode(
                            lucasOptional.get(),
                            DemoScenarioCodes
                                    .RECRUITER_DEMO_LOAN_NOTIFICATIONS
                    ).size();

            if (lucasNotificationCount != 1) {
                anomalies.add(
                        "Lucas doit posséder exactement une notification DEMO, "
                                + lucasNotificationCount + " trouvée(s)."
                );
                healthy = false;
            }
        } else {
            anomalies.add(
                    "Impossible de contrôler les notifications de Lucas : "
                            + "compte introuvable."
            );
            healthy = false;
        }

        if (sarahOptional.isPresent()) {
            int sarahNotificationCount =
                    notificationRepository.findByUserAndDemoScenarioCode(
                            sarahOptional.get(),
                            DemoScenarioCodes
                                    .RECRUITER_DEMO_LOAN_NOTIFICATIONS
                    ).size();

            if (sarahNotificationCount != 1) {
                anomalies.add(
                        "Sarah doit posséder exactement une notification DEMO, "
                                + sarahNotificationCount + " trouvée(s)."
                );
                healthy = false;
            }
        } else {
            anomalies.add(
                    "Impossible de contrôler les notifications de Sarah : "
                            + "compte introuvable."
            );
            healthy = false;
        }

        return healthy;
    }

    // =========================================================================
    // CONTACTS MONGODB
    // =========================================================================

    /**
     * Vérifie le nombre, les statuts, l'origine, les sujets et la cohérence des
     * réponses des documents Contact DEMO.
     *
     * @param anomalies collection recevant les anomalies détectées
     * @return {@code true} si les huit documents Contact sont conformes
     */
    private boolean checkContacts(List<String> anomalies) {
        List<ContactDocument> contacts =
                contactMongoRepository.findByDemoScenarioCode(
                        DemoScenarioCodes.RECRUITER_DEMO_CONTACT_MESSAGES
                );

        boolean healthy = true;

        if (contacts.size()
                != DemoScenarioDefinition.EXPECTED_CONTACT_COUNT) {
            anomalies.add(
                    "Le scénario CONTACT doit contenir exactement "
                            + DemoScenarioDefinition.EXPECTED_CONTACT_COUNT
                            + " documents, "
                            + contacts.size() + " trouvé(s)."
            );
            healthy = false;
        }

        long newCount = contacts.stream()
                .filter(contact -> ContactStatus.NEW.name().equals(
                        contact.getStatusContact()
                ))
                .count();

        long answeredCount = contacts.stream()
                .filter(contact -> ContactStatus.ANSWERED.name().equals(
                        contact.getStatusContact()
                ))
                .count();

        if (newCount
                != DemoScenarioDefinition.EXPECTED_NEW_CONTACT_COUNT) {
            anomalies.add(
                    "Le scénario CONTACT doit contenir "
                            + DemoScenarioDefinition.EXPECTED_NEW_CONTACT_COUNT
                            + " messages NEW, "
                            + newCount + " trouvé(s)."
            );
            healthy = false;
        }

        if (answeredCount
                != DemoScenarioDefinition.EXPECTED_ANSWERED_CONTACT_COUNT) {
            anomalies.add(
                    "Le scénario CONTACT doit contenir "
                            + DemoScenarioDefinition
                            .EXPECTED_ANSWERED_CONTACT_COUNT
                            + " messages ANSWERED, "
                            + answeredCount + " trouvé(s)."
            );
            healthy = false;
        }

        boolean invalidOriginExists = contacts.stream()
                .anyMatch(contact ->
                        !DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM.equals(
                                contact.getOriginContact()
                        )
                );

        if (invalidOriginExists) {
            anomalies.add(
                    "Au moins un document CONTACT DEMO ne possède pas "
                            + "l'origine "
                            + DemoScenarioDefinition.CONTACT_ORIGIN_WEB_FORM
                            + "."
            );
            healthy = false;
        }

        Set<String> actualSubjects = new HashSet<>();

        for (ContactDocument contact : contacts) {
            if (contact.getSubjectContact() != null) {
                actualSubjects.add(contact.getSubjectContact());
            }
        }

        if (!Objects.equals(
                actualSubjects,
                DemoScenarioDefinition.EXPECTED_CONTACT_SUBJECTS
        )) {
            anomalies.add(
                    "Les sujets des documents CONTACT DEMO ne correspondent "
                            + "pas aux huit scénarios canoniques."
            );
            healthy = false;
        }

        for (ContactDocument contact : contacts) {
            boolean answered = ContactStatus.ANSWERED.name().equals(
                    contact.getStatusContact()
            );

            if (answered) {
                if (!contact.isResponseSentContact()
                        || contact.getResponseContentContact() == null
                        || contact.getResponseContentContact().isBlank()
                        || contact.getAnsweredByUserId() == null
                        || contact.getUpdatedAtContact() == null) {
                    anomalies.add(
                            "Un document CONTACT ANSWERED est incomplet : "
                                    + contact.getSubjectContact() + "."
                    );
                    healthy = false;
                }
            } else {
                if (contact.isResponseSentContact()
                        || contact.getResponseContentContact() != null
                        || contact.getAnsweredByUserId() != null
                        || contact.getUpdatedAtContact() != null) {
                    anomalies.add(
                            "Un document CONTACT NEW contient des données "
                                    + "de réponse : "
                                    + contact.getSubjectContact() + "."
                    );
                    healthy = false;
                }
            }
        }

        return healthy;
    }
}