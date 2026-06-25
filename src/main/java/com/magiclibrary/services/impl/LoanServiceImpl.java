package com.magiclibrary.services.impl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.magiclibrary.dto.loan.LoanRequestDTO;
import com.magiclibrary.dto.loan.LoanResponseDTO;
import com.magiclibrary.entities.Loan;
import com.magiclibrary.entities.LoanLine;
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ItemStatus;
import com.magiclibrary.enums.LoanLineStatus;
import com.magiclibrary.enums.LoanOrigin;
import com.magiclibrary.enums.LoanStatus;
import com.magiclibrary.exceptions.custom.LoanAlreadyReturnedException;
import com.magiclibrary.exceptions.custom.LoanNotFoundException;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.mappers.LoanMapper;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.repositories.interfaces.LoanLineRepository;
import com.magiclibrary.repositories.interfaces.LoanRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.LoanService;

/**
 * Implémentation principale du service de gestion des emprunts.
 *
 * Cette classe centralise les opérations métier liées aux emprunts :
 * création, restitution, consultation, recherche, suggestions, tri et pagination.
 *
 * Elle applique également les règles de filtrage des emprunts actifs
 * ainsi que les mécanismes de recherche utilisés par l'interface SSR.
 */
@Service
@Transactional
public class LoanServiceImpl implements LoanService {

    /*
     * Valeurs de tri autorisées par les écrans SSR.
     * Ces constantes garantissent la cohérence entre l'interface,
     * le service et les mécanismes de tri dynamiques.
     */
    private static final String SORT_RECENT = "recent";
    private static final String SORT_OLDEST = "oldest";
    private static final String SORT_DUE_SOON = "dueSoon";
    private static final String SORT_STATUS = "status";
    private static final String SORT_MEMBER = "member";
    private static final String SORT_ORIGIN = "origin";

    /*
     * Paramètres du moteur de recherche et de suggestion.
     *
     * SUGGEST_LIMIT :
     * nombre maximal de résultats retournés par l'autocomplétion.
     *
     * MAX_QUERY_LENGTH :
     * longueur maximale acceptée pour une recherche utilisateur.
     *
     * MIN_TOKEN_LENGTH :
     * longueur minimale d'un terme significatif après normalisation.
     */
    private static final int SUGGEST_LIMIT = 8;
    private static final int MAX_QUERY_LENGTH = 80;
    private static final int MIN_TOKEN_LENGTH = 2;

    /*
     * Liste des mots fonctionnels ignorés lors de la recherche.
     *
     * Leur suppression améliore la pertinence des résultats et évite
     * que des termes très fréquents influencent le moteur de recherche.
     */
    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "de", "du", "des", "d",
            "la", "le", "les", "l",
            "un", "une",
            "et", "ou",
            "a", "au", "aux",
            "the", "of", "and", "or"
    );

    private final LoanRepository loanRepository;
    private final LoanLineRepository loanLineRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanLineRepository loanLineRepository,
            ItemRepository itemRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.loanLineRepository = loanLineRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public LoanResponseDTO createLoan(LoanRequestDTO request) {
        if (request == null || request.getIdUser() == null) {
            throw new IllegalArgumentException(
                    "L'identifiant utilisateur est obligatoire pour créer un emprunt."
            );
        }

        User user = userRepository.findById(request.getIdUser())
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable avec l'id : " + request.getIdUser()
                ));

        if (!Boolean.TRUE.equals(user.getActiveUser())) {
            throw new UserNotFoundException(
                    "Le membre n’existe pas ou n’est pas actif."
            );
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setStartDateLoan(LocalDateTime.now());
        loan.setDueDateLoan(LocalDate.now().plusDays(30));
        loan.setReturnedLoan(Boolean.FALSE);
        loan.setReturnDateLoan(null);
        loan.setOverdueLoan(Boolean.FALSE);
        loan.setExtendedLoan(Boolean.FALSE);
        loan.setExtensionCountLoan(0);
        loan.setStatusLoan(LoanStatus.ONGOING);
        loan.setOriginLoan(LoanOrigin.ADMIN.getCode());
        loan.setDeletedDateLoan(null);
        loan.setNotesLoan(null);

        Loan saved = loanRepository.save(loan);

        return LoanMapper.toResponseDTO(saved);
    }

    @Override
    public LoanResponseDTO returnLoan(Integer idLoan)
            throws LoanNotFoundException, LoanAlreadyReturnedException {

        Loan loan = loanRepository.findById(idLoan)
                .orElseThrow(() -> new LoanNotFoundException(
                        "Aucun emprunt trouvé avec l'id : " + idLoan
                ));

        if (Boolean.TRUE.equals(loan.getReturnedLoan())) {
            throw new LoanAlreadyReturnedException(
                    "L'emprunt est déjà restitué."
            );
        }

        List<LoanLine> loanLines = loanLineRepository.findByLoan_IdLoan(idLoan);

        for (LoanLine loanLine : loanLines) {
            loanLine.setStatusLoanLine(LoanLineStatus.RETURNED);

            if (loanLine.getItem() != null) {
                loanLine.getItem().setAvailableItem(Boolean.TRUE);
                loanLine.getItem().setStatusItem(ItemStatus.AVAILABLE);
                itemRepository.save(loanLine.getItem());
            }
        }

        loanLineRepository.saveAll(loanLines);

        loan.setReturnedLoan(Boolean.TRUE);
        loan.setReturnDateLoan(LocalDateTime.now());
        loan.setStatusLoan(LoanStatus.RETURNED);

        Loan updated = loanRepository.save(loan);

        return LoanMapper.toResponseDTO(updated);
    }

    @Override
    public LoanResponseDTO getLoanById(Integer idLoan)
            throws LoanNotFoundException {

        Loan loan = loanRepository.findById(idLoan)
                .orElseThrow(() -> new LoanNotFoundException(
                        "Aucun emprunt trouvé avec l'id : " + idLoan
                ));

        return LoanMapper.toResponseDTO(loan);
    }

    @Override
    public List<LoanResponseDTO> getAllLoans() {
        List<Loan> loans = getActiveLoansSorted(Sort.by(
                Sort.Order.desc("startDateLoan"),
                Sort.Order.desc("idLoan")
        ));
        return LoanMapper.toResponseDTOList(loans);
    }

    @Override
    public List<LoanResponseDTO> getAllLoansSorted(String sort) {
        Sort resolvedSort = buildLoanSort(sort);
        List<Loan> loans = getActiveLoansSorted(resolvedSort);
        return LoanMapper.toResponseDTOList(loans);
    }

    @Override
    public Page<LoanResponseDTO> getAllLoansPagedAndSorted(String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        List<Loan> loans = new ArrayList<>(getActiveLoansSorted(buildLoanSort(sort)));
        List<LoanResponseDTO> content = paginateAndMap(loans, safePage, safeSize);

        return new PageImpl<>(
                content,
                PageRequest.of(safePage, safeSize),
                loans.size()
        );
    }

    @Override
    public Page<LoanResponseDTO> searchLoansPagedAndSorted(String query, String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        String normalizedQuery = normalizeQuery(query);
        List<Loan> loans = new ArrayList<>(getActiveLoansSorted(buildLoanSort(sort)));

        if (normalizedQuery.isEmpty()) {
            List<LoanResponseDTO> content = paginateAndMap(loans, safePage, safeSize);
            return new PageImpl<>(
                    content,
                    PageRequest.of(safePage, safeSize),
                    loans.size()
            );
        }

        if (!thresholdReached(normalizedQuery) || containsForbiddenChars(normalizedQuery)) {
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(safePage, safeSize),
                    0
            );
        }

        List<String> tokens = tokenize(normalizedQuery);
        if (tokens.isEmpty()) {
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(safePage, safeSize),
                    0
            );
        }

        List<Loan> filteredLoans = loans.stream()
                .filter(loan -> matchesLoanTokens(loan, tokens))
                .toList();

        List<LoanResponseDTO> content = paginateAndMap(filteredLoans, safePage, safeSize);

        return new PageImpl<>(
                content,
                PageRequest.of(safePage, safeSize),
                filteredLoans.size()
        );
    }

    @Override
    public List<LoanResponseDTO> getLoansForUser(String email) {
        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email
                ));

        List<Loan> loans = getActiveLoansForUser(user);

        return LoanMapper.toResponseDTOList(loans);
    }

    @Override
    public Page<LoanResponseDTO> getLoansForUserPagedAndSorted(String email, String sort, int page, int size) {
        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email
                ));

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 9;

        List<Loan> loans = new ArrayList<>(getActiveLoansForUser(user));
        loans.sort(buildLoanComparator(sort));

        int total = loans.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        List<LoanResponseDTO> content = LoanMapper.toResponseDTOList(
                loans.subList(fromIndex, toIndex)
        );

        return new PageImpl<>(
                content,
                PageRequest.of(safePage, safeSize),
                total
        );
    }

    @Override
    public LoanResponseDTO getLoanByIdForUser(Integer idLoan, String email, boolean isAdmin) {
        Loan loan = loanRepository.findById(idLoan)
                .orElseThrow(() -> new LoanNotFoundException(
                        "Aucun emprunt trouvé avec l'id : " + idLoan
                ));

        if (!isAdmin) {
            String ownerEmail = loan.getUser().getEmailUser();
            if (!ownerEmail.equals(email)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Accès interdit à cet emprunt."
                );
            }
        }

        return LoanMapper.toResponseDTO(loan);
    }

    @Override
    public List<LoanResponseDTO> suggestLoans(String query) {
        String normalizedQuery = normalizeQuery(query);

        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        if (!thresholdReached(normalizedQuery)) {
            return List.of();
        }

        if (containsForbiddenChars(normalizedQuery)) {
            return List.of();
        }

        List<String> tokens = tokenize(normalizedQuery);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<Loan> loans = getActiveLoansSorted(Sort.by(
                Sort.Order.desc("startDateLoan"),
                Sort.Order.desc("idLoan")
        ));

        return suggestFromLoans(loans, tokens);
    }

    @Override
    public List<LoanResponseDTO> suggestLoansForUser(String email, String query) {
        User user = userRepository.findByEmailUser(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email
                ));

        String normalizedQuery = normalizeQuery(query);

        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        if (!thresholdReached(normalizedQuery)) {
            return List.of();
        }

        if (containsForbiddenChars(normalizedQuery)) {
            return List.of();
        }

        List<String> tokens = tokenize(normalizedQuery);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<Loan> loans = new ArrayList<>(getActiveLoansForUser(user));
        loans.sort(buildLoanComparator(SORT_RECENT));

        return suggestFromLoans(loans, tokens);
    }

    /*
     * Construit les suggestions affichées dans l'autocomplétion.
     *
     * Tous les termes recherchés doivent être présents dans les données
     * normalisées de l'emprunt pour qu'il soit retenu.
     */
    private List<LoanResponseDTO> suggestFromLoans(List<Loan> loans, List<String> tokens) {
        Map<Integer, LoanResponseDTO> out = new LinkedHashMap<>(SUGGEST_LIMIT * 2);

        for (Loan loan : loans) {
            if (loan == null || loan.getIdLoan() == null) {
                continue;
            }

            if (loan.getDeletedDateLoan() != null) {
                continue;
            }

            LoanResponseDTO dto = LoanMapper.toResponseDTO(loan);
            if (dto == null) {
                continue;
            }

            String haystack = buildSuggestHaystack(dto);

            boolean matchesAll = true;
            for (String token : tokens) {
                String normalizedToken = normalizeTokenForMatch(token);
                if (normalizedToken.isEmpty()) {
                    continue;
                }
                if (!haystack.contains(normalizedToken)) {
                    matchesAll = false;
                    break;
                }
            }

            if (!matchesAll) {
                continue;
            }

            out.putIfAbsent(dto.getIdLoan(), dto);

            if (out.size() >= SUGGEST_LIMIT) {
                break;
            }
        }

        return new ArrayList<>(out.values());
    }

    private boolean matchesLoanTokens(Loan loan, List<String> tokens) {
        if (loan == null || loan.getDeletedDateLoan() != null) {
            return false;
        }

        LoanResponseDTO dto = LoanMapper.toResponseDTO(loan);
        if (dto == null) {
            return false;
        }

        String haystack = buildSuggestHaystack(dto);

        for (String token : tokens) {
            String normalizedToken = normalizeTokenForMatch(token);
            if (normalizedToken.isEmpty()) {
                continue;
            }

            if (!haystack.contains(normalizedToken)) {
                return false;
            }
        }

        return true;
    }

    private List<Loan> getActiveLoansSorted(Sort sort) {
        return loanRepository.findAll(sort).stream()
                .filter(this::isActiveLoan)
                .toList();
    }

    private List<Loan> getActiveLoansForUser(User user) {
        return loanRepository.findByUser(user).stream()
                .filter(this::isActiveLoan)
                .toList();
    }

    private boolean isActiveLoan(Loan loan) {
        return loan != null && loan.getDeletedDateLoan() == null;
    }

    private List<LoanResponseDTO> paginateAndMap(List<Loan> loans, int page, int size) {
        int total = loans.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        return LoanMapper.toResponseDTOList(
                loans.subList(fromIndex, toIndex)
        );
    }

    /*
     * Convertit la valeur de tri reçue depuis l'interface
     * en objet Sort exploitable par Spring Data JPA.
     */
    private Sort buildLoanSort(String sort) {
        String normalizedSort = normalizeSort(sort);

        return switch (normalizedSort) {
            case SORT_OLDEST -> Sort.by(
                    Sort.Order.asc("startDateLoan"),
                    Sort.Order.asc("idLoan")
            );
            case SORT_DUE_SOON -> Sort.by(
                    Sort.Order.asc("dueDateLoan"),
                    Sort.Order.desc("startDateLoan"),
                    Sort.Order.desc("idLoan")
            );
            case SORT_STATUS -> Sort.by(
                    Sort.Order.asc("statusLoan"),
                    Sort.Order.desc("startDateLoan"),
                    Sort.Order.desc("idLoan")
            );
            case SORT_MEMBER -> Sort.by(
                    Sort.Order.asc("user.idUser"),
                    Sort.Order.desc("startDateLoan"),
                    Sort.Order.desc("idLoan")
            );
            case SORT_ORIGIN -> Sort.by(
                    Sort.Order.asc("originLoan"),
                    Sort.Order.desc("startDateLoan"),
                    Sort.Order.desc("idLoan")
            );
            case SORT_RECENT -> Sort.by(
                    Sort.Order.desc("startDateLoan"),
                    Sort.Order.desc("idLoan")
            );
            default -> Sort.by(
                    Sort.Order.desc("startDateLoan"),
                    Sort.Order.desc("idLoan")
            );
        };
    }

    /*
     * Reproduit côté mémoire les mêmes règles de tri que celles utilisées
     * par Spring Data lorsque les données sont déjà chargées.
     */
    private Comparator<Loan> buildLoanComparator(String sort) {
        String normalizedSort = normalizeSort(sort);

        Comparator<Loan> byIdDesc = Comparator.comparing(
                Loan::getIdLoan,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        Comparator<Loan> byIdAsc = Comparator.comparing(
                Loan::getIdLoan,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        Comparator<Loan> byStartDateDesc = Comparator.comparing(
                Loan::getStartDateLoan,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        Comparator<Loan> byStartDateAsc = Comparator.comparing(
                Loan::getStartDateLoan,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        Comparator<Loan> byDueDateAsc = Comparator.comparing(
                Loan::getDueDateLoan,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        Comparator<Loan> byStatusAsc = Comparator.comparing(
                Loan::getStatusLoan,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        Comparator<Loan> byOriginAsc = Comparator.comparing(
                Loan::getOriginLoan,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );

        Comparator<Loan> byMemberIdAsc = Comparator.comparing(
                loan -> loan != null && loan.getUser() != null ? loan.getUser().getIdUser() : null,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        return switch (normalizedSort) {
            case SORT_OLDEST -> byStartDateAsc.thenComparing(byIdAsc);
            case SORT_DUE_SOON -> byDueDateAsc.thenComparing(byStartDateDesc).thenComparing(byIdDesc);
            case SORT_STATUS -> byStatusAsc.thenComparing(byStartDateDesc).thenComparing(byIdDesc);
            case SORT_MEMBER -> byMemberIdAsc.thenComparing(byStartDateDesc).thenComparing(byIdDesc);
            case SORT_ORIGIN -> byOriginAsc.thenComparing(byStartDateDesc).thenComparing(byIdDesc);
            case SORT_RECENT -> byStartDateDesc.thenComparing(byIdDesc);
            default -> byStartDateDesc.thenComparing(byIdDesc);
        };
    }

    private String normalizeSort(String sort) {
        if (sort == null) {
            return SORT_RECENT;
        }

        String normalized = sort.trim();

        if (normalized.isEmpty()) {
            return SORT_RECENT;
        }

        return switch (normalized) {
            case SORT_RECENT, SORT_OLDEST, SORT_DUE_SOON, SORT_STATUS, SORT_MEMBER, SORT_ORIGIN -> normalized;
            default -> SORT_RECENT;
        };
    }

    private static String buildSuggestHaystack(LoanResponseDTO dto) {
        String idLoan = dto.getIdLoan() == null ? "" : normalizeNumericToken(String.valueOf(dto.getIdLoan()));
        String idUser = dto.getIdUser() == null ? "" : normalizeNumericToken(String.valueOf(dto.getIdUser()));
        String firstName = normalizeText(dto.getFirstNameUser());
        String lastName = normalizeText(dto.getLastNameUser());
        String statusCode = dto.getStatusLoan() == null ? "" : normalizeText(dto.getStatusLoan().name());
        String statusLabel = normalizeText(dto.getStatusLoanLabel());
        String originCode = normalizeText(dto.getOriginLoan());
        String originLabel = normalizeText(dto.getOriginLoanLabel());

        return (
                idLoan + " " +
                        idUser + " " +
                        firstName + " " +
                        lastName + " " +
                        statusCode + " " +
                        statusLabel + " " +
                        originCode + " " +
                        originLabel
        ).trim();
    }

    /*
     * Normalise un texte pour les opérations de recherche :
     * suppression des accents, passage en minuscules,
     * uniformisation des séparateurs et espaces.
     */
    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ROOT).trim();

        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");

        normalized = normalized.replace(',', ' ');
        normalized = normalized.replace(';', ' ');
        normalized = normalized.replace(':', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    private static List<String> tokenize(String raw) {
        if (raw == null) {
            return List.of();
        }

        String cleaned = normalizeText(raw);
        if (cleaned.isEmpty()) {
            return List.of();
        }

        String[] parts = cleaned.split(" ");
        List<String> tokens = new ArrayList<>(parts.length);

        for (String part : parts) {
            if (part == null) {
                continue;
            }

            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }

            if (STOP_WORDS.contains(token)) {
                continue;
            }

            if (isNumeric(token)) {
                tokens.add(normalizeNumericToken(token));
                continue;
            }

            if (token.length() < MIN_TOKEN_LENGTH) {
                continue;
            }

            tokens.add(token);
        }

        return tokens;
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.length() > MAX_QUERY_LENGTH) {
            return trimmed.substring(0, MAX_QUERY_LENGTH).trim();
        }

        return trimmed;
    }

    private static boolean thresholdReached(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }

        return isNumeric(query) ? query.length() >= 1 : query.length() >= 2;
    }

    private static boolean containsForbiddenChars(String query) {
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '/' || c == '\\') {
                return true;
            }
        }

        return query.contains("//");
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    private static String normalizeNumericToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        if (!isNumeric(trimmed)) {
            return trimmed;
        }

        String normalized = trimmed.replaceFirst("^0+", "");
        return normalized.isEmpty() ? "0" : normalized;
    }

    private static String normalizeTokenForMatch(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }

        String trimmed = token.trim();
        if (isNumeric(trimmed)) {
            return normalizeNumericToken(trimmed);
        }

        return normalizeText(trimmed);
    }
}