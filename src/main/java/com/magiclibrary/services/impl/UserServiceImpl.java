package com.magiclibrary.services.impl;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.magiclibrary.dto.user.UserCreateDTO;
import com.magiclibrary.dto.user.UserResponseDTO;
import com.magiclibrary.dto.user.UserUpdateDTO;
import com.magiclibrary.entities.Role;
import com.magiclibrary.entities.User;
import com.magiclibrary.mappers.UserMapper;
import com.magiclibrary.repositories.interfaces.RoleRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.services.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String SORT_ROLE_THEN_LAST_NAME = "roleThenLastName";
    private static final String SORT_LAST_NAME_THEN_FIRST_NAME = "lastNameThenFirstName";
    private static final String SORT_FIRST_NAME_THEN_LAST_NAME = "firstNameThenLastName";
    private static final String SORT_STATUS_THEN_LAST_NAME = "statusThenLastName";
    private static final String SORT_EMAIL_ASC = "emailAsc";
    private static final String SORT_NEWEST = "newest";

    private static final int DEFAULT_PAGE_SIZE = 9;
    private static final int SUGGEST_LIMIT = 8;
    private static final int MAX_QUERY_LENGTH = 80;
    private static final int MIN_TOKEN_LENGTH = 2;

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "du", "des", "d",
            "la", "le", "les", "l",
            "un", "une",
            "et", "ou",
            "a", "au", "aux",
            "the", "of", "and", "or"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {
        normalizeCreateDTO(userCreateDTO);

        if (userRepository.existsByEmailUser(userCreateDTO.getEmail())) {
            throw new IllegalStateException("Un utilisateur avec cet email existe déjà.");
        }

        User user = userMapper.toEntity(userCreateDTO);

        user.setPasswordUser(passwordEncoder.encode(user.getPasswordUser()));

        Role membreRole = roleRepository.findByLabelRole("MEMBRE")
                .orElseThrow(() -> new IllegalStateException("Le rôle MEMBRE est introuvable."));
        user.setRole(membreRole);

        user.setActiveUser(true);
        user.setSubscriptionUser(false);
        user.setDepositUser(false);
        user.setEmailVerifiedUser(false);

        user.setSignupDateUser(LocalDateTime.now());
        user.setUpdatedAtUser(null);

        User saved = userRepository.save(user);
        return userMapper.toResponseDTO(saved);
    }

    @Override
    public UserResponseDTO getAuthenticatedUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));

        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));

        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateAuthenticatedUser(Integer userId, UserUpdateDTO userUpdateDTO) {
        normalizeUpdateDTO(userUpdateDTO);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));

        userMapper.updateEntityFromDTO(user, userUpdateDTO);
        user.setUpdatedAtUser(LocalDateTime.now());

        User saved = userRepository.save(user);
        return userMapper.toResponseDTO(saved);
    }

    @Override
    public UserResponseDTO updateUserByAdmin(Integer userId, UserUpdateDTO userUpdateDTO) {
        normalizeUpdateDTO(userUpdateDTO);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));

        userMapper.updateEntityFromDTO(user, userUpdateDTO);
        user.setUpdatedAtUser(LocalDateTime.now());

        User saved = userRepository.save(user);
        return userMapper.toResponseDTO(saved);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    @Override
    public List<UserResponseDTO> getFilteredUsers(String search, String role, String status, String sort) {
        String normalizedSearch = normalize(search);
        String normalizedRole = normalize(role);
        Boolean normalizedStatus = parseStatus(status);
        String normalizedSort = normalizeSort(sort);

        return userRepository.findAllWithFilters(normalizedSearch, normalizedRole, normalizedStatus)
                .stream()
                .map(userMapper::toResponseDTO)
                .sorted(buildComparator(normalizedSort))
                .toList();
    }

    @Override
    public Page<UserResponseDTO> getFilteredUsersPaged(String search, String role, String status, String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : DEFAULT_PAGE_SIZE;

        List<UserResponseDTO> users = getFilteredUsers(search, role, status, sort);
        int total = users.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        List<UserResponseDTO> content = users.subList(fromIndex, toIndex);

        return new PageImpl<>(
                content,
                PageRequest.of(safePage, safeSize),
                total
        );
    }

    @Override
    public List<UserResponseDTO> suggestUsers(String query) {
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

        List<UserResponseDTO> users = userRepository.findAll().stream()
                .map(userMapper::toResponseDTO)
                .sorted(buildComparator(SORT_NEWEST))
                .toList();

        List<UserResponseDTO> suggestions = new ArrayList<>(SUGGEST_LIMIT);

        for (UserResponseDTO user : users) {
            if (user == null || user.getIdUser() == null) {
                continue;
            }

            String haystack = buildSuggestHaystack(user);

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

            suggestions.add(user);

            if (suggestions.size() >= SUGGEST_LIMIT) {
                break;
            }
        }

        return suggestions;
    }

    private void normalizeCreateDTO(UserCreateDTO userCreateDTO) {
        if (userCreateDTO == null) {
            return;
        }

        userCreateDTO.setCivility(normalizeCivility(userCreateDTO.getCivility()));
        userCreateDTO.setFirstName(normalizeFirstName(userCreateDTO.getFirstName()));
        userCreateDTO.setLastName(normalizeLastName(userCreateDTO.getLastName()));
        userCreateDTO.setEmail(normalizeEmail(userCreateDTO.getEmail()));
    }

    private void normalizeUpdateDTO(UserUpdateDTO userUpdateDTO) {
        if (userUpdateDTO == null) {
            return;
        }

        userUpdateDTO.setFirstName(normalizeFirstName(userUpdateDTO.getFirstName()));
        userUpdateDTO.setLastName(normalizeLastName(userUpdateDTO.getLastName()));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSpaces(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private String stripAccents(String value) {
        if (value == null) {
            return null;
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private String normalizeCivility(String civility) {
        String normalized = normalizeSpaces(civility);

        if (normalized == null) {
            return null;
        }

        String simplified = stripAccents(normalized).toLowerCase(Locale.ROOT);

        return switch (simplified) {
            case "m" -> "M";
            case "mme" -> "Mme";
            default -> normalized;
        };
    }

    private String normalizeFirstName(String firstName) {
        String normalized = normalizeSpaces(firstName);

        if (normalized == null) {
            return null;
        }

        String[] parts = normalized.split(" ");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (part.isEmpty()) {
                continue;
            }

            String lower = part.toLowerCase(Locale.ROOT);
            String formatted = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(formatted);
        }

        return builder.toString();
    }

    private String normalizeLastName(String lastName) {
        String normalized = normalizeSpaces(lastName);

        if (normalized == null) {
            return null;
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeSpaces(email);

        if (normalized == null) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private Boolean parseStatus(String status) {
        String normalizedStatus = normalize(status);

        if (normalizedStatus == null) {
            return null;
        }

        return switch (normalizedStatus.toUpperCase(Locale.ROOT)) {
            case "ACTIF", "ACTIVE", "TRUE" -> Boolean.TRUE;
            case "INACTIF", "INACTIVE", "FALSE" -> Boolean.FALSE;
            default -> null;
        };
    }

    private String normalizeSort(String sort) {
        String normalizedSort = normalize(sort);

        if (normalizedSort == null) {
            return SORT_ROLE_THEN_LAST_NAME;
        }

        return switch (normalizedSort) {
            case SORT_ROLE_THEN_LAST_NAME,
                 SORT_LAST_NAME_THEN_FIRST_NAME,
                 SORT_FIRST_NAME_THEN_LAST_NAME,
                 SORT_STATUS_THEN_LAST_NAME,
                 SORT_EMAIL_ASC,
                 SORT_NEWEST -> normalizedSort;
            default -> SORT_ROLE_THEN_LAST_NAME;
        };
    }

    private Comparator<UserResponseDTO> buildComparator(String sort) {
        Comparator<UserResponseDTO> byId = Comparator.comparing(
                UserResponseDTO::getIdUser,
                Comparator.nullsLast(Integer::compareTo)
        );

        Comparator<UserResponseDTO> byLastName = Comparator.comparing(
                user -> normalizeForSort(user.getLastName()),
                Comparator.nullsLast(String::compareToIgnoreCase)
        );

        Comparator<UserResponseDTO> byFirstName = Comparator.comparing(
                user -> normalizeForSort(user.getFirstName()),
                Comparator.nullsLast(String::compareToIgnoreCase)
        );

        Comparator<UserResponseDTO> byEmail = Comparator.comparing(
                user -> normalizeForSort(user.getEmail()),
                Comparator.nullsLast(String::compareToIgnoreCase)
        );

        Comparator<UserResponseDTO> byRole = Comparator.comparing(
                user -> normalizeForSort(user.getRoleLabel()),
                Comparator.nullsLast(String::compareToIgnoreCase)
        );

        Comparator<UserResponseDTO> byStatus = Comparator.comparing(
                user -> user.getActiveUser() == null ? null : (user.getActiveUser() ? 0 : 1),
                Comparator.nullsLast(Integer::compareTo)
        );

        Comparator<UserResponseDTO> bySignupDateDesc = Comparator.comparing(
                UserResponseDTO::getSignupDateUser,
                Comparator.nullsLast(LocalDateTime::compareTo)
        ).reversed();

        return switch (sort) {
            case SORT_LAST_NAME_THEN_FIRST_NAME -> byLastName.thenComparing(byFirstName).thenComparing(byId);
            case SORT_FIRST_NAME_THEN_LAST_NAME -> byFirstName.thenComparing(byLastName).thenComparing(byId);
            case SORT_EMAIL_ASC -> byEmail.thenComparing(byLastName).thenComparing(byFirstName).thenComparing(byId);
            case SORT_STATUS_THEN_LAST_NAME -> byStatus.thenComparing(byLastName).thenComparing(byFirstName).thenComparing(byId);
            case SORT_NEWEST -> bySignupDateDesc.thenComparing(byLastName).thenComparing(byFirstName).thenComparing(byId);
            case SORT_ROLE_THEN_LAST_NAME -> byRole.thenComparing(byLastName).thenComparing(byFirstName).thenComparing(byId);
            default -> byRole.thenComparing(byLastName).thenComparing(byFirstName).thenComparing(byId);
        };
    }

    private String normalizeForSort(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String buildSuggestHaystack(UserResponseDTO user) {
        String id = user.getIdUser() == null ? "" : normalizeNumericToken(String.valueOf(user.getIdUser()));
        String firstName = normalizeText(user.getFirstName());
        String lastName = normalizeText(user.getLastName());
        String email = normalizeText(user.getEmail());
        String role = normalizeText(user.getRoleLabel());
        String status = Boolean.TRUE.equals(user.getActiveUser())
                ? "actif active actifs actives active true"
                : "inactif inactive inactifs inactives inactive false";

        return (
                id + " " +
                        firstName + " " +
                        lastName + " " +
                        email + " " +
                        role + " " +
                        status
        ).trim();
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ROOT).trim();

        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");

        normalized = normalized
                .replace('\'', ' ')
                .replace('’', ' ')
                .replace('`', ' ')
                .replace('´', ' ')
                .replace('-', ' ')
                .replace('_', ' ')
                .replace(',', ' ')
                .replace(';', ' ')
                .replace(':', ' ')
                .replace('/', ' ')
                .replace('\\', ' ')
                .replace('(', ' ')
                .replace(')', ' ')
                .replace('{', ' ')
                .replace('}', ' ')
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('.', ' ')
                .replace('!', ' ')
                .replace('?', ' ');

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