package com.magiclibrary.init;

// -----------------------------------------------------------------------------
// IMPORTS JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// -----------------------------------------------------------------------------
// IMPORTS LOGGING
// -----------------------------------------------------------------------------
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionTemplate;

// -----------------------------------------------------------------------------
// IMPORTS MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.entities.Item;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;
import com.magiclibrary.entities.Notification;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.LoanLineStatus;
import com.magiclibrary.enums.LoanStatus;
import com.magiclibrary.enums.NotificationCategory;
import com.magiclibrary.enums.NotificationType;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.repositories.interfaces.LoanLineRepository;
import com.magiclibrary.repositories.interfaces.LoanRepository;
import com.magiclibrary.repositories.interfaces.NotificationRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;

/**
 * =============================================================================
 * INITIALISATION AUTOMATIQUE - EMPRUNTS DE DÉMONSTRATION
 * =============================================================================
 *
 * Objectif :
 *      Reconstruire automatiquement les emprunts, lignes d'emprunt et
 *      notifications de démonstration lorsque la base contient déjà les
 *      utilisateurs membres de démonstration et le catalogue importé.
 *
 * Principes :
 *      - aucun identifiant technique n'est utilisé ;
 *      - les utilisateurs sont retrouvés par email ;
 *      - les objets sont retrouvés par source_ref stocké dans tags_item ;
 *      - les scénarios sont identifiés par demoScenarioCode ;
 *      - les notes restent du contenu lisible et ne servent plus de marqueur ;
 *      - les données scénarisées sont supprimées puis recréées proprement ;
 *      - aucune donnée réelle existante n'est supprimée ;
 *      - la classe fonctionne après RoleInitializer, UserInitializer et
 *        MemberInitializer.
 *
 * Sécurité d'exécution :
 *      - si les comptes membres ou les objets attendus sont absents, l'initializer
 *        ne bloque pas le démarrage de l'application ;
 *      - dans ce cas, un log explicite est produit et la création sera retentée
 *        au prochain démarrage ;
 *      - les suppressions ciblent exclusivement les constantes officielles
 *        DemoScenarioCodes.
 *
 * =============================================================================
 */
@Configuration
public class DemoLoanInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DemoLoanInitializer.class);

    private static final String ORIGIN_SYSTEM = "SYSTEM";

    private static final String LUCAS_EMAIL = "lucas.demo@magiclibrary.fr";
    private static final String SARAH_EMAIL = "sarah.demo@magiclibrary.fr";

    private static final String REF_LUCAS_BOOK_ONE = "source_ref:L00001";
    private static final String REF_LUCAS_BOOK_TWO = "source_ref:L00002";
    private static final String REF_SARAH_DVD_ONE = "source_ref:D00001";

    @Bean
    @Order(4)
    public CommandLineRunner initDemoLoans(
            UserRepository userRepository,
            ItemRepository itemRepository,
            LoanRepository loanRepository,
            LoanLineRepository loanLineRepository,
            NotificationRepository notificationRepository,
            TransactionTemplate transactionTemplate
    ) {
        return args -> transactionTemplate.executeWithoutResult(status -> initializeDemoLoans(
                userRepository,
                itemRepository,
                loanRepository,
                loanLineRepository,
                notificationRepository
        ));
    }

    private void initializeDemoLoans(
            UserRepository userRepository,
            ItemRepository itemRepository,
            LoanRepository loanRepository,
            LoanLineRepository loanLineRepository,
            NotificationRepository notificationRepository
    ) {

        Optional<User> lucasOptional = userRepository.findByEmailUser(LUCAS_EMAIL);
        Optional<User> sarahOptional = userRepository.findByEmailUser(SARAH_EMAIL);

        if (lucasOptional.isEmpty() || sarahOptional.isEmpty()) {
            logger.warn("Emprunts de démonstration non créés : comptes membres de démonstration absents.");
            return;
        }

        Optional<Item> lucasBookOneOptional = findDemoItem(itemRepository, REF_LUCAS_BOOK_ONE);
        Optional<Item> lucasBookTwoOptional = findDemoItem(itemRepository, REF_LUCAS_BOOK_TWO);
        Optional<Item> sarahDvdOneOptional = findDemoItem(itemRepository, REF_SARAH_DVD_ONE);

        if (lucasBookOneOptional.isEmpty() || lucasBookTwoOptional.isEmpty() || sarahDvdOneOptional.isEmpty()) {
            logger.warn("Emprunts de démonstration non créés : objets de démonstration introuvables dans le catalogue.");
            return;
        }

        rebuildDemoLoans(
                itemRepository,
                loanRepository,
                loanLineRepository,
                notificationRepository
        );

        User lucas = lucasOptional.get();
        User sarah = sarahOptional.get();

        Item lucasBookOne = lucasBookOneOptional.get();
        Item lucasBookTwo = lucasBookTwoOptional.get();
        Item sarahDvdOne = sarahDvdOneOptional.get();

        Loan lucasLoan = createLoan(
                lucas,
                LocalDateTime.now().minusDays(5),
                LocalDate.now().plusDays(16),
                LoanStatus.ONGOING,
                false,
                false,
                "Emprunt de démonstration actif pour Lucas.",
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
        );

        loanRepository.save(lucasLoan);

        loanLineRepository.save(createLoanLine(lucasLoan, lucasBookOne));
        loanLineRepository.save(createLoanLine(lucasLoan, lucasBookTwo));

        markItemUnavailable(lucasBookOne);
        markItemUnavailable(lucasBookTwo);

        itemRepository.saveAll(List.of(lucasBookOne, lucasBookTwo));

        notificationRepository.save(createNotification(
                lucas,
                "Emprunt en cours",
                "Votre emprunt de démonstration est actif. Pensez à restituer les objets avant la date d'échéance.",
                NotificationType.REMINDER,
                NotificationCategory.RAPPEL,
                "MEDIUM"
        ));

        Loan sarahLoan = createLoan(
                sarah,
                LocalDateTime.now().minusDays(20),
                LocalDate.now().minusDays(3),
                LoanStatus.LATE,
                false,
                true,
                "Emprunt de démonstration en retard pour Sarah.",
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN
        );

        loanRepository.save(sarahLoan);

        loanLineRepository.save(createLoanLine(sarahLoan, sarahDvdOne));

        markItemUnavailable(sarahDvdOne);
        itemRepository.save(sarahDvdOne);

        notificationRepository.save(createNotification(
                sarah,
                "Emprunt en retard",
                "Un emprunt de démonstration est en retard. Merci de vérifier la date de retour prévue.",
                NotificationType.OVERDUE,
                NotificationCategory.RAPPEL,
                "HIGH"
        ));

        logger.info("Emprunts, lignes d'emprunt et notifications de démonstration reconstruits avec succès.");
    }

    // -------------------------------------------------------------------------
    // RECONSTRUCTION CONTRÔLÉE DES DONNÉES DE DÉMONSTRATION
    // -------------------------------------------------------------------------
    /**
     * Nettoie les données scénarisées avant reconstruction.
     *
     * Ordre volontaire :
     *      1. supprimer les notifications DEMO ;
     *      2. retrouver les lignes rattachées aux prêts DEMO ;
     *      3. remettre les items concernés disponibles ;
     *      4. supprimer les lignes d'emprunt DEMO ;
     *      5. supprimer les prêts DEMO.
     *
     * Les comptes utilisateurs et les objets catalogue ne sont jamais supprimés.
     */
    private void rebuildDemoLoans(
            ItemRepository itemRepository,
            LoanRepository loanRepository,
            LoanLineRepository loanLineRepository,
            NotificationRepository notificationRepository
    ) {
        notificationRepository.deleteByDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_LOAN_NOTIFICATIONS);

        restoreItemsLinkedToDemoLoans(
                itemRepository,
                loanLineRepository,
                DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN
        );

        restoreItemsLinkedToDemoLoans(
                itemRepository,
                loanLineRepository,
                DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN
        );

        loanLineRepository.deleteByLoan_DemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN);
        loanLineRepository.deleteByLoan_DemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN);

        loanRepository.deleteByDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_LUCAS_ACTIVE_LOAN);
        loanRepository.deleteByDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_SARAH_OVERDUE_LOAN);
    }

    /**
     * Remet disponibles les objets actuellement rattachés à un prêt de
     * démonstration avant suppression des lignes d'emprunt.
     */
    private void restoreItemsLinkedToDemoLoans(
            ItemRepository itemRepository,
            LoanLineRepository loanLineRepository,
            String demoScenarioCode
    ) {
        List<LoanLine> demoLoanLines = loanLineRepository.findByLoan_DemoScenarioCode(demoScenarioCode);
        List<Item> itemsToRestore = new ArrayList<>();

        for (LoanLine loanLine : demoLoanLines) {
            Item item = loanLine.getItem();

            if (item != null) {
                markItemAvailable(item);
                itemsToRestore.add(item);
            }
        }

        if (!itemsToRestore.isEmpty()) {
            itemRepository.saveAll(itemsToRestore);
        }
    }

    private Optional<Item> findDemoItem(ItemRepository itemRepository, String sourceRef) {
        return itemRepository.findFirstByDeletedDateItemIsNullAndTagsItemContaining(sourceRef);
    }

    private Loan createLoan(
            User user,
            LocalDateTime startDate,
            LocalDate dueDate,
            LoanStatus status,
            Boolean returned,
            Boolean overdue,
            String notes,
            String demoScenarioCode
    ) {
        Loan loan = new Loan(
                user,
                startDate,
                dueDate,
                returned,
                overdue,
                false,
                0,
                status,
                ORIGIN_SYSTEM
        );

        loan.setNotesLoan(notes);
        loan.setDemoScenarioCode(demoScenarioCode);
        return loan;
    }

    private LoanLine createLoanLine(Loan loan, Item item) {
        LoanLine loanLine = new LoanLine(
                loan,
                item,
                1,
                LoanLineStatus.ACTIVE,
                LocalDateTime.now()
        );

        loanLine.setNotesLoanLine("Ligne d'emprunt de démonstration.");
        return loanLine;
    }

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
                "/mes-emprunts",
                type,
                category,
                priority
        );

        notification.setDemoScenarioCode(DemoScenarioCodes.RECRUITER_DEMO_LOAN_NOTIFICATIONS);
        return notification;
    }

    private void markItemUnavailable(Item item) {
        item.setAvailableItem(false);
        item.setStatusItem(ItemStatus.UNAVAILABLE);
        item.setUpdatedAtItem(LocalDateTime.now());
    }

    private void markItemAvailable(Item item) {
        item.setAvailableItem(true);
        item.setStatusItem(ItemStatus.AVAILABLE);
        item.setUpdatedAtItem(LocalDateTime.now());
    }
}