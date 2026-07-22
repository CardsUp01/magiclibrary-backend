package com.magiclibrary.controllers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.dto.loanline.LoanLineResponseDTO;
import com.magiclibrary.services.LoanLineService;
import com.magiclibrary.services.LoanService;

/**
 * Contrôleur SSR réservé à l'administration des emprunts.
 *
 * <p>Cette classe gère l'affichage paginé des emprunts, la recherche, le tri,
 * l'autocomplétion, la consultation détaillée et la restitution des emprunts
 * depuis l'espace d'administration.</p>
 *
 * <p>Elle expose également au template un indicateur déterminant si le bouton
 * de réinitialisation manuelle de la démonstration doit être affiché. Cet
 * indicateur vaut {@code true} uniquement lorsque :</p>
 *
 * <ul>
 *     <li>le profil Spring {@code demo} est actif ;</li>
 *     <li>{@code magiclibrary.demo.reset.enabled=true} ;</li>
 *     <li>{@code magiclibrary.demo.reset.manual-enabled=true}.</li>
 * </ul>
 *
 * <p>Le contrôleur ne déclenche lui-même aucune reconstruction DEMO. L'action
 * manuelle reste portée par {@code DemoResetController}.</p>
 */
@Controller
public class AdminLoansPageController {

    /**
     * Taille par défaut utilisée pour la pagination de la page SSR
     * d'administration des emprunts.
     */
    private static final int LOANS_PAGE_SIZE = 9;

    private static final String DEMO_PROFILE = "demo";

    private static final String DEMO_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.enabled";

    private static final String DEMO_MANUAL_RESET_ENABLED_PROPERTY =
            "magiclibrary.demo.reset.manual-enabled";

    private final LoanService loanService;
    private final LoanLineService loanLineService;
    private final Environment environment;

    /**
     * Initialise le contrôleur avec les services métier et l'environnement
     * Spring utilisé pour déterminer la visibilité du bouton DEMO.
     *
     * @param loanService service métier des emprunts
     * @param loanLineService service métier des lignes d'emprunt
     * @param environment environnement Spring actif
     */
    public AdminLoansPageController(
            LoanService loanService,
            LoanLineService loanLineService,
            Environment environment
    ) {
        this.loanService = loanService;
        this.loanLineService = loanLineService;
        this.environment = environment;
    }

    /**
     * Affiche la page d'administration des emprunts.
     *
     * <p>La méthode prépare les données nécessaires à l'écran SSR : liste
     * paginée, recherche, tri, sélection éventuelle d'un emprunt, résumé des
     * objets associés, indicateurs de pagination et visibilité de l'action de
     * réinitialisation manuelle DEMO.</p>
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
            @RequestParam(name = "sort", required = false) String sort,
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
        int safeSize = size > 0 ? size : LOANS_PAGE_SIZE;

        String resolvedSort =
                sort == null || sort.trim().isEmpty()
                        ? "recent"
                        : sort.trim();

        String resolvedQuery = q == null ? "" : q.trim();

        Page<LoanResponseDTO> loansPage;

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
        } else if (resolvedQuery.isEmpty()) {
            loansPage = loanService.getAllLoansPagedAndSorted(
                    resolvedSort,
                    safePage,
                    safeSize
            );
        } else {
            loansPage = loanService.searchLoansPagedAndSorted(
                    resolvedQuery,
                    resolvedSort,
                    safePage,
                    safeSize
            );
        }

        List<LoanResponseDTO> loans = loansPage.getContent();

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

        if (currentEmail != null && !currentEmail.isBlank()) {
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

        for (LoanResponseDTO loan : loans) {
            Integer idLoan = loan.getIdLoan();

            List<LoanLineResponseDTO> lines =
                    loanLineService.getLoanLinesByLoanId(idLoan);

            int totalItems = 0;
            String firstTitle = null;

            for (LoanLineResponseDTO line : lines) {
                Integer quantity = line.getQuantityLoanLine();

                totalItems += quantity != null && quantity > 0
                        ? quantity
                        : 0;

                if (firstTitle == null) {
                    String title = line.getTitleItem();

                    if (title != null
                            && !title.trim().isEmpty()) {
                        firstTitle = title.trim();
                    }
                }
            }

            loanItemCounts.put(idLoan, totalItems);

            loanItemSummaries.put(
                    idLoan,
                    buildLoanItemSummary(firstTitle, totalItems)
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
                loansPage.getTotalElements() > safeSize;

        model.addAttribute("loans", loans);
        model.addAttribute("loanItemCounts", loanItemCounts);
        model.addAttribute("loanItemSummaries", loanItemSummaries);
        model.addAttribute("currentUserLoans", currentUserLoans);
        model.addAttribute("q", resolvedQuery);
        model.addAttribute("selectedLoanId", selectedLoanId);
        model.addAttribute("sort", resolvedSort);
        model.addAttribute("pageTitle", "Emprunts");
        model.addAttribute("activePage", "admin-loans");

        model.addAttribute("currentPage", loansPage.getNumber());
        model.addAttribute("pageSize", loansPage.getSize());
        model.addAttribute("totalPages", loansPage.getTotalPages());
        model.addAttribute(
                "totalElements",
                loansPage.getTotalElements()
        );
        model.addAttribute("hasPrevious", loansPage.hasPrevious());
        model.addAttribute("hasNext", loansPage.hasNext());
        model.addAttribute("isFirst", loansPage.isFirst());
        model.addAttribute("isLast", loansPage.isLast());
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
            @RequestParam(name = "q", required = false) String q
    ) {
        List<LoanSuggestResponse> suggestions =
                loanService.suggestLoans(q)
                        .stream()
                        .map(this::toSuggestResponse)
                        .toList();

        return ResponseEntity.ok(suggestions);
    }

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
            @PathVariable("id") Integer idLoan,
            Model model
    ) {
        LoanResponseDTO loan =
                loanService.getLoanById(idLoan);

        List<LoanLineResponseDTO> loanLines =
                loanLineService.getLoanLinesByLoanId(idLoan);

        model.addAttribute("loan", loan);
        model.addAttribute("loanLines", loanLines);
        model.addAttribute("pageTitle", "Fiche emprunt");
        model.addAttribute("activePage", "admin-loans");
        model.addAttribute("loanDetailContext", "admin");

        return "admin/fiche-emprunt";
    }

    /**
     * Marque un emprunt comme restitué depuis l'espace d'administration.
     *
     * @param idLoan identifiant de l'emprunt
     * @return redirection vers la fiche de l'emprunt
     */
    @PostMapping("/admin/emprunts/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public String returnLoan(
            @PathVariable("id") Integer idLoan
    ) {
        loanService.returnLoan(idLoan);

        return "redirect:/admin/emprunts/" + idLoan;
    }

    /**
     * Détermine si le bouton de réinitialisation manuelle DEMO doit être
     * visible.
     *
     * <p>Les trois conditions doivent être vraies simultanément. Le profil
     * {@code prod} utilisé seul ne peut donc jamais afficher cette action.</p>
     *
     * @return {@code true} uniquement sur l'instance DEMO autorisée
     */
    private boolean isDemoManualResetEnabled() {
        boolean demoProfileActive = Arrays.stream(
                environment.getActiveProfiles()
        ).anyMatch(DEMO_PROFILE::equals);

        boolean demoResetEnabled = environment.getProperty(
                DEMO_RESET_ENABLED_PROPERTY,
                Boolean.class,
                Boolean.FALSE
        );

        boolean manualResetEnabled = environment.getProperty(
                DEMO_MANUAL_RESET_ENABLED_PROPERTY,
                Boolean.class,
                Boolean.FALSE
        );

        return demoProfileActive
                && demoResetEnabled
                && manualResetEnabled;
    }

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
                firstTitle != null && !firstTitle.isBlank()
                        ? firstTitle
                        : "Objet sans titre";

        if (totalItems == 1) {
            return safeFirstTitle;
        }

        int additionalItems = totalItems - 1;

        return safeFirstTitle
                + " + "
                + additionalItems
                + " autre"
                + (additionalItems > 1 ? "s" : "");
    }

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

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }
    }
}