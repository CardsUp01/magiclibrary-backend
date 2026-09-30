package com.magiclibrary.controllers;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.Valid;

// -----------------------------------------------------------------------------
// IMPORTS SPRING
// -----------------------------------------------------------------------------
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.dto.loan.AdminLoanRequestDTO;
import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.exceptions.custom.ItemUnavailableException;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.services.ItemService;
import com.magiclibrary.services.LoanLineService;
import com.magiclibrary.services.LoanService;
import com.magiclibrary.services.UserService;

/**
 * =============================================================================
 * CONTROLLER SSR : ADMINISTRATION DES EMPRUNTS
 * =============================================================================
 *
 * Contrôleur réservé aux administrateurs pour la gestion des emprunts depuis
 * l'interface SSR de MagicLibrary.
 *
 * Cette classe centralise notamment :
 *
 * - l'affichage paginé des emprunts ;
 * - la recherche et le tri ;
 * - les suggestions d'autocomplétion ;
 * - la consultation détaillée d'un emprunt ;
 * - la restitution d'un emprunt ;
 * - la création administrative complète d'un nouvel emprunt.
 *
 * Le workflow de création administrative permet désormais de sélectionner :
 *
 * - un emprunteur actif possédant le rôle MEMBRE ou ADMIN ;
 * - un ou plusieurs objets disponibles ;
 * - la date réelle de début ;
 * - la date d'échéance ;
 * - une note facultative.
 *
 * La validation métier définitive reste volontairement confiée à LoanService.
 * Le contrôleur filtre les données affichées pour améliorer l'expérience
 * utilisateur, mais il ne constitue jamais l'unique barrière de sécurité.
 *
 * Le contrôleur expose également au template un indicateur déterminant si le
 * bouton de réinitialisation manuelle de la démonstration doit être affiché.
 * Cet indicateur vaut true uniquement lorsque :
 *
 * - le profil Spring "demo" est actif ;
 * - magiclibrary.demo.reset.enabled=true ;
 * - magiclibrary.demo.reset.manual-enabled=true.
 *
 * Le contrôleur ne déclenche lui-même aucune reconstruction DEMO.
 * L'action manuelle reste portée par DemoResetController.
 * =============================================================================
 */
@Controller
public class AdminLoansPageController {

    // -------------------------------------------------------------------------
    // CONSTANTES DE PAGINATION
    // -------------------------------------------------------------------------

    /**
     * Taille par défaut utilisée pour la pagination de la page SSR
     * d'administration des emprunts.
     */
    private static final int LOANS_PAGE_SIZE = 9;

    // -------------------------------------------------------------------------
    // CONSTANTES DU WORKFLOW DE CRÉATION
    // -------------------------------------------------------------------------

    /**
     * Rôles autorisés à être proposés comme emprunteurs.
     *
     * Un administrateur peut lui-même être emprunteur dans MagicLibrary.
     * Le rôle INVITE reste exclu.
     */
    private static final String BORROWER_ROLE_MEMBER = "MEMBRE";
    private static final String BORROWER_ROLE_ADMIN = "ADMIN";

    /**
     * Statut technique correspondant à un objet empruntable.
     */
    private static final String AVAILABLE_ITEM_STATUS = "AVAILABLE";

    /**
     * Durée par défaut proposée lors de la création d'un emprunt.
     *
     * Cette valeur correspond au comportement historique du MVP :
     * échéance proposée à trente jours.
     */
    private static final long DEFAULT_LOAN_DURATION_DAYS = 30L;

    // -------------------------------------------------------------------------
    // CONSTANTES DEMO
    // -------------------------------------------------------------------------

    private static final String DEMO_PROFILE = "demo";

    private static final String DEMO_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.enabled";

    private static final String DEMO_MANUAL_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.manual-enabled";

    // -------------------------------------------------------------------------
    // SERVICES
    // -------------------------------------------------------------------------

    private final LoanService loanService;
    private final LoanLineService loanLineService;
    private final UserService userService;
    private final ItemService itemService;
    private final Environment environment;

    /**
     * Initialise le contrôleur avec les services métier nécessaires.
     *
     * @param loanService service métier des emprunts
     * @param loanLineService service métier des lignes d'emprunt
     * @param userService service métier des utilisateurs
     * @param itemService service métier du catalogue
     * @param environment environnement Spring actif
     */
    public AdminLoansPageController(
            LoanService loanService,
            LoanLineService loanLineService,
            UserService userService,
            ItemService itemService,
            Environment environment
    ) {
        this.loanService = loanService;
        this.loanLineService = loanLineService;
        this.userService = userService;
        this.itemService = itemService;
        this.environment = environment;
    }

    // =========================================================================
    // LISTE DES EMPRUNTS
    // =========================================================================

    /**
     * Affiche la page d'administration des emprunts.
     *
     * La méthode prépare les données nécessaires à l'écran SSR :
     * liste paginée, recherche, tri, sélection éventuelle d'un emprunt,
     * résumé des objets associés, indicateurs de pagination et visibilité
     * de l'action de réinitialisation manuelle DEMO.
     *
     * @param q recherche textuelle facultative
     * @param selectedLoanId identifiant d'un emprunt sélectionné
     * @param sort mode de tri
     * @param page index de page demandé
     * @param size taille de page demandée
     * @param authentication authentification courante
     * @param model modèle Thymeleaf
     * @return template SSR d'administration des emprunts
     */
    @GetMapping("/admin/emprunts")
    @PreAuthorize("hasRole('ADMIN')")
    public String showLoansPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(
                    name = "selectedLoanId",
                    required = false
            ) Integer selectedLoanId,
            @RequestParam(
                    name = "sort",
                    required = false
            ) String sort,
            @RequestParam(
                    name = "page",
                    required = false,
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    name = "size",
                    required = false,
                    defaultValue = "9"
            ) int size,
            Authentication authentication,
            Model model
    ) {

        int safePage = Math.max(page, 0);
        int safeSize = size > 0
                ? size
                : LOANS_PAGE_SIZE;

        String resolvedSort =
                sort == null || sort.trim().isEmpty()
                        ? "recent"
                        : sort.trim();

        String resolvedQuery =
                q == null
                        ? ""
                        : q.trim();

        Page<LoanResponseDTO> loansPage;

        /*
         * Lorsqu'un emprunt précis est sélectionné depuis l'autocomplétion,
         * la page affiche uniquement cet emprunt.
         */
        if (selectedLoanId != null) {

            LoanResponseDTO selectedLoan =
                    loanService.getLoanById(selectedLoanId);

            List<LoanResponseDTO> selectedLoans =
                    List.of(selectedLoan);

            loansPage = new PageImpl<>(
                    selectedLoans,
                    PageRequest.of(0, safeSize),
                    selectedLoans.size()
            );

            /*
             * Sans recherche, récupération paginée standard.
             */
        } else if (resolvedQuery.isEmpty()) {

            loansPage =
                    loanService.getAllLoansPagedAndSorted(
                            resolvedSort,
                            safePage,
                            safeSize
                    );

            /*
             * Recherche textuelle active.
             */
        } else {

            loansPage =
                    loanService.searchLoansPagedAndSorted(
                            resolvedQuery,
                            resolvedSort,
                            safePage,
                            safeSize
                    );
        }

        List<LoanResponseDTO> loans =
                loansPage.getContent();

        Map<Integer, Integer> loanItemCounts =
                new LinkedHashMap<>();

        Map<Integer, String> loanItemSummaries =
                new LinkedHashMap<>();

        Map<Integer, Boolean> currentUserLoans =
                new LinkedHashMap<>();

        String currentEmail =
                authentication != null
                        ? authentication.getName()
                        : null;

        Integer currentUserId = null;

        /*
         * Cette logique existante permet notamment d'identifier les emprunts
         * appartenant à l'utilisateur actuellement connecté.
         */
        if (currentEmail != null
                && !currentEmail.isBlank()) {

            List<LoanResponseDTO> currentUserAllLoans =
                    loanService.getLoansForUser(currentEmail);

            if (!currentUserAllLoans.isEmpty()) {

                LoanResponseDTO firstLoan =
                        currentUserAllLoans.get(0);

                currentUserId =
                        firstLoan != null
                                ? firstLoan.getIdUser()
                                : null;
            }
        }

        /*
         * Préparation du nombre d'objets et du résumé textuel associé
         * à chaque emprunt affiché.
         */
        for (LoanResponseDTO loan : loans) {

            Integer idLoan =
                    loan.getIdLoan();

            List<LoanLineResponseDTO> lines =
                    loanLineService.getLoanLinesByLoanId(
                            idLoan
                    );

            int totalItems = 0;
            String firstTitle = null;

            for (LoanLineResponseDTO line : lines) {

                Integer quantity =
                        line.getQuantityLoanLine();

                totalItems +=
                        quantity != null
                                && quantity > 0
                                ? quantity
                                : 0;

                if (firstTitle == null) {

                    String title =
                            line.getTitleItem();

                    if (title != null
                            && !title.trim().isEmpty()) {

                        firstTitle =
                                title.trim();
                    }
                }
            }

            loanItemCounts.put(
                    idLoan,
                    totalItems
            );

            loanItemSummaries.put(
                    idLoan,
                    buildLoanItemSummary(
                            firstTitle,
                            totalItems
                    )
            );

            currentUserLoans.put(
                    idLoan,
                    currentUserId != null
                            && Objects.equals(
                            currentUserId,
                            loan.getIdUser()
                    )
            );
        }

        boolean paginationEnabled =
                loansPage.getTotalElements()
                        > safeSize;

        // ---------------------------------------------------------------------
        // MODÈLE THYMELEAF
        // ---------------------------------------------------------------------

        model.addAttribute(
                "loans",
                loans
        );

        model.addAttribute(
                "loanItemCounts",
                loanItemCounts
        );

        model.addAttribute(
                "loanItemSummaries",
                loanItemSummaries
        );

        model.addAttribute(
                "currentUserLoans",
                currentUserLoans
        );

        model.addAttribute(
                "q",
                resolvedQuery
        );

        model.addAttribute(
                "selectedLoanId",
                selectedLoanId
        );

        model.addAttribute(
                "sort",
                resolvedSort
        );

        model.addAttribute(
                "pageTitle",
                "Emprunts"
        );

        model.addAttribute(
                "activePage",
                "admin-loans"
        );

        model.addAttribute(
                "currentPage",
                loansPage.getNumber()
        );

        model.addAttribute(
                "pageSize",
                loansPage.getSize()
        );

        model.addAttribute(
                "totalPages",
                loansPage.getTotalPages()
        );

        model.addAttribute(
                "totalElements",
                loansPage.getTotalElements()
        );

        model.addAttribute(
                "hasPrevious",
                loansPage.hasPrevious()
        );

        model.addAttribute(
                "hasNext",
                loansPage.hasNext()
        );

        model.addAttribute(
                "isFirst",
                loansPage.isFirst()
        );

        model.addAttribute(
                "isLast",
                loansPage.isLast()
        );

        model.addAttribute(
                "paginationEnabled",
                paginationEnabled
        );

        model.addAttribute(
                "demoManualResetEnabled",
                isDemoManualResetEnabled()
        );

        return "admin/emprunts";
    }

    // =========================================================================
    // CRÉATION ADMINISTRATIVE D'UN EMPRUNT
    // =========================================================================

    /**
     * Affiche le formulaire de création administrative d'un emprunt.
     *
     * Les valeurs proposées par défaut sont :
     *
     * - date de début : date et heure actuelles ;
     * - date d'échéance : trente jours après la date actuelle.
     *
     * Ces valeurs restent modifiables par l'administrateur afin de permettre
     * notamment l'enregistrement rétroactif d'un emprunt déjà commencé.
     *
     * @param model modèle Thymeleaf
     * @return template de création d'un emprunt
     */
    @GetMapping("/admin/emprunts/ajouter")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateLoanPage(
            Model model
    ) {

        AdminLoanRequestDTO request =
                new AdminLoanRequestDTO();

        /*
         * Suppression des secondes et nanosecondes afin de produire une valeur
         * parfaitement compatible avec un champ HTML datetime-local affiché
         * avec une précision à la minute.
         */
        LocalDateTime defaultStartDate =
                LocalDateTime.now()
                        .withSecond(0)
                        .withNano(0);

        request.setStartDateLoan(
                defaultStartDate
        );

        request.setDueDateLoan(
                defaultStartDate
                        .toLocalDate()
                        .plusDays(DEFAULT_LOAN_DURATION_DAYS)
        );

        model.addAttribute(
                "adminLoanRequest",
                request
        );

        prepareCreateLoanModel(model);

        return "admin/ajout-emprunt";
    }

    /**
     * Traite la création complète d'un emprunt depuis l'espace ADMIN.
     *
     * Le contrôleur exécute d'abord la validation Bean Validation du DTO.
     *
     * En cas de succès, LoanService réalise ensuite les contrôles métier
     * définitifs et crée transactionnellement :
     *
     * - le Loan ;
     * - les LoanLine ;
     * - la mise à jour de disponibilité des Item.
     *
     * En cas d'erreur de formulaire ou d'erreur métier attendue, le formulaire
     * est réaffiché avec les données saisies et un message explicite.
     *
     * @param request données saisies dans le formulaire
     * @param bindingResult résultat de la validation Spring
     * @param model modèle Thymeleaf
     * @return formulaire en cas d'erreur ou redirection vers l'emprunt créé
     */
    @PostMapping("/admin/emprunts/ajouter")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAdminLoan(
            @Valid
            @ModelAttribute("adminLoanRequest")
            AdminLoanRequestDTO request,
            BindingResult bindingResult,
            Model model
    ) {

        /*
         * Les erreurs Bean Validation ou de conversion Spring sont traitées
         * avant tout appel au service métier.
         */
        if (bindingResult.hasErrors()) {

            prepareCreateLoanModel(model);

            return "admin/ajout-emprunt";
        }

        try {

            LoanResponseDTO createdLoan =
                    loanService.createAdminLoan(
                            request
                    );

            /*
             * La création terminée, l'administrateur est envoyé directement
             * vers la fiche du nouvel emprunt afin de contrôler immédiatement
             * l'emprunteur, les objets et les dates enregistrées.
             */
            return "redirect:/admin/emprunts/"
                    + createdLoan.getIdLoan();

        } catch (
                IllegalArgumentException
                | UserNotFoundException
                | ItemNotFoundException
                | ItemUnavailableException exception
        ) {

            /*
             * Les erreurs métier prévisibles sont associées au formulaire
             * comme erreur globale.
             *
             * La transaction portée par LoanService garantit qu'aucune création
             * partielle n'est conservée lorsqu'une exception est levée.
             */
            bindingResult.reject(
                    "adminLoan.creation",
                    exception.getMessage()
            );

            prepareCreateLoanModel(model);

            return "admin/ajout-emprunt";
        }
    }

    /**
     * Prépare toutes les données nécessaires au formulaire administratif
     * de création d'un emprunt.
     *
     * La méthode filtre volontairement :
     *
     * EMPRUNTEURS :
     * - utilisateur non null ;
     * - compte actif ;
     * - rôle MEMBRE ou ADMIN.
     *
     * Le rôle INVITE et les comptes inactifs sont donc exclus.
     *
     * OBJETS :
     * - objet non null ;
     * - non archivé ;
     * - availableItem = true ;
     * - statusItem = AVAILABLE.
     *
     * IMPORTANT :
     * ces filtres améliorent uniquement l'interface.
     * Toutes les règles sont revérifiées dans LoanService avant enregistrement.
     *
     * @param model modèle Thymeleaf à enrichir
     */
    private void prepareCreateLoanModel(
            Model model
    ) {

        // ---------------------------------------------------------------------
        // EMPRUNTEURS AUTORISÉS
        // ---------------------------------------------------------------------

        List<UserResponseDTO> members =
                userService.getAllUsers()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(user ->
                                Boolean.TRUE.equals(
                                        user.getActiveUser()
                                )
                        )
                        .filter(user -> {

                            if (user.getRoleLabel() == null) {
                                return false;
                            }

                            String roleLabel =
                                    user.getRoleLabel().trim();

                            return BORROWER_ROLE_MEMBER.equalsIgnoreCase(
                                    roleLabel
                            ) || BORROWER_ROLE_ADMIN.equalsIgnoreCase(
                                    roleLabel
                            );
                        })
                        .sorted(
                                Comparator
                                        .comparing(
                                                UserResponseDTO::getLastName,
                                                Comparator.nullsLast(
                                                        String.CASE_INSENSITIVE_ORDER
                                                )
                                        )
                                        .thenComparing(
                                                UserResponseDTO::getFirstName,
                                                Comparator.nullsLast(
                                                        String.CASE_INSENSITIVE_ORDER
                                                )
                                        )
                                        .thenComparing(
                                                UserResponseDTO::getIdUser,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                        )
                        .toList();

        // ---------------------------------------------------------------------
        // OBJETS DISPONIBLES
        // ---------------------------------------------------------------------

        List<ItemResponseDTO> availableItems =
                itemService.getAllItems()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(item ->
                                item.getDeletedDateItem() == null
                        )
                        .filter(item ->
                                Boolean.TRUE.equals(
                                        item.getAvailableItem()
                                )
                        )
                        .filter(item ->
                                item.getStatusItem() != null
                                        && AVAILABLE_ITEM_STATUS.equalsIgnoreCase(
                                        item.getStatusItem().trim()
                                )
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                ItemResponseDTO::getTitleItem,
                                                Comparator.nullsLast(
                                                        String.CASE_INSENSITIVE_ORDER
                                                )
                                        )
                                        .thenComparing(
                                                ItemResponseDTO::getIdItem,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                        )
                        .toList();

        // ---------------------------------------------------------------------
        // ATTRIBUTS DU FORMULAIRE
        // ---------------------------------------------------------------------

        /*
         * Le nom "members" est conservé pour ne pas casser le contrat actuel
         * avec le template Thymeleaf. La liste contient désormais les comptes
         * emprunteurs autorisés : MEMBRE et ADMIN actifs.
         */
        model.addAttribute(
                "members",
                members
        );

        model.addAttribute(
                "availableItems",
                availableItems
        );

        model.addAttribute(
                "membersCount",
                members.size()
        );

        model.addAttribute(
                "availableItemsCount",
                availableItems.size()
        );

        model.addAttribute(
                "pageTitle",
                "Créer un emprunt"
        );

        model.addAttribute(
                "activePage",
                "admin-loans"
        );
    }

    // =========================================================================
    // AUTOCOMPLÉTION DES EMPRUNTS
    // =========================================================================

    /**
     * Fournit les suggestions d'emprunts pour l'autocomplétion de la page
     * d'administration.
     *
     * @param q recherche saisie
     * @return suggestions au format JSON
     */
    @GetMapping(
            value = "/admin/emprunts/suggest",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanSuggestResponse>> suggestLoans(
            @RequestParam(
                    name = "q",
                    required = false
            ) String q
    ) {

        List<LoanSuggestResponse> suggestions =
                loanService.suggestLoans(q)
                        .stream()
                        .map(this::toSuggestResponse)
                        .toList();

        return ResponseEntity.ok(
                suggestions
        );
    }

    // =========================================================================
    // FICHE DÉTAILLÉE
    // =========================================================================

    /**
     * Affiche la fiche détaillée d'un emprunt pour l'administrateur.
     *
     * @param idLoan identifiant de l'emprunt
     * @param model modèle Thymeleaf
     * @return template de détail de l'emprunt
     */
    @GetMapping("/admin/emprunts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showLoanDetailPage(
            @PathVariable("id")
            Integer idLoan,
            Model model
    ) {

        LoanResponseDTO loan =
                loanService.getLoanById(
                        idLoan
                );

        List<LoanLineResponseDTO> loanLines =
                loanLineService.getLoanLinesByLoanId(
                        idLoan
                );

        model.addAttribute(
                "loan",
                loan
        );

        model.addAttribute(
                "loanLines",
                loanLines
        );

        model.addAttribute(
                "pageTitle",
                "Fiche emprunt"
        );

        model.addAttribute(
                "activePage",
                "admin-loans"
        );

        model.addAttribute(
                "loanDetailContext",
                "admin"
        );

        return "admin/fiche-emprunt";
    }

    // =========================================================================
    // RESTITUTION
    // =========================================================================

    /**
     * Marque un emprunt comme restitué depuis l'espace d'administration.
     *
     * @param idLoan identifiant de l'emprunt
     * @return redirection vers la fiche de l'emprunt
     */
    @PostMapping("/admin/emprunts/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public String returnLoan(
            @PathVariable("id")
            Integer idLoan
    ) {

        loanService.returnLoan(
                idLoan
        );

        return "redirect:/admin/emprunts/"
                + idLoan;
    }

    // =========================================================================
    // DEMO
    // =========================================================================

    /**
     * Détermine si le bouton de réinitialisation manuelle DEMO doit être
     * visible.
     *
     * Les trois conditions doivent être vraies simultanément.
     * Le profil prod utilisé seul ne peut donc jamais afficher cette action.
     *
     * @return true uniquement sur l'instance DEMO autorisée
     */
    private boolean isDemoManualResetEnabled() {

        boolean demoProfileActive =
                Arrays.stream(
                        environment.getActiveProfiles()
                ).anyMatch(
                        DEMO_PROFILE::equals
                );

        boolean demoResetEnabled =
                environment.getProperty(
                        DEMO_RESET_ENABLED_PROPERTY,
                        Boolean.class,
                        Boolean.FALSE
                );

        boolean manualResetEnabled =
                environment.getProperty(
                        DEMO_MANUAL_RESET_ENABLED_PROPERTY,
                        Boolean.class,
                        Boolean.FALSE
                );

        return demoProfileActive
                && demoResetEnabled
                && manualResetEnabled;
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES
    // =========================================================================

    /**
     * Convertit un emprunt en suggestion légère.
     *
     * @param loan emprunt source
     * @return suggestion JSON
     */
    private LoanSuggestResponse toSuggestResponse(
            LoanResponseDTO loan
    ) {

        return new LoanSuggestResponse(
                loan.getIdLoan(),
                loan.getIdUser(),
                loan.getFirstNameUser(),
                loan.getLastNameUser(),
                loan.getStatusLoanLabel(),
                loan.getOriginLoanLabel()
        );
    }

    /**
     * Construit le résumé affiché pour les objets associés à un emprunt.
     *
     * @param firstTitle premier titre disponible
     * @param totalItems nombre total d'objets
     * @return résumé lisible
     */
    private String buildLoanItemSummary(
            String firstTitle,
            int totalItems
    ) {

        if (totalItems <= 0) {
            return "Aucun objet associé";
        }

        String safeFirstTitle =
                firstTitle != null
                        && !firstTitle.isBlank()
                        ? firstTitle
                        : "Objet sans titre";

        if (totalItems == 1) {
            return safeFirstTitle;
        }

        int additionalItems =
                totalItems - 1;

        return safeFirstTitle
                + " + "
                + additionalItems
                + " autre"
                + (additionalItems > 1 ? "s" : "");
    }

    // =========================================================================
    // DTO INTERNE : SUGGESTION
    // =========================================================================

    /**
     * DTO interne utilisé uniquement pour exposer les suggestions d'emprunts
     * au format JSON.
     */
    public static final class LoanSuggestResponse {

        private Integer idLoan;
        private Integer idUser;
        private String firstName;
        private String lastName;
        private String status;
        private String origin;

        public LoanSuggestResponse() {
        }

        public LoanSuggestResponse(
                Integer idLoan,
                Integer idUser,
                String firstName,
                String lastName,
                String status,
                String origin
        ) {
            this.idLoan = idLoan;
            this.idUser = idUser;
            this.firstName = firstName;
            this.lastName = lastName;
            this.status = status;
            this.origin = origin;
        }

        public Integer getIdLoan() {
            return idLoan;
        }

        public void setIdLoan(Integer idLoan) {
            this.idLoan = idLoan;
        }

        public Integer getIdUser() {
            return idUser;
        }

        public void setIdUser(Integer idUser) {
            this.idUser = idUser;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(
                String firstName
        ) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(
                String lastName
        ) {
            this.lastName = lastName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(
                String status
        ) {
            this.status = status;
        }

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(
                String origin
        ) {
            this.origin = origin;
        }
    }
}