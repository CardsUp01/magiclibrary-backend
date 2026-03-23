package com.magiclibrary.controllers.advice;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.magiclibrary.entities.User;
import com.magiclibrary.repositories.interfaces.UserRepository;

@ControllerAdvice
public class GlobalCurrentUserModelAdvice {

    private final UserRepository userRepository;

    public GlobalCurrentUserModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public Map<String, Object> populateCurrentUser(Authentication authentication) {
        Map<String, Object> currentUser = new LinkedHashMap<>();

        currentUser.put("authenticated", false);
        currentUser.put("civility", null);
        currentUser.put("firstName", null);
        currentUser.put("lastName", null);
        currentUser.put("fullName", null);
        currentUser.put("displayName", null);
        currentUser.put("role", null);
        currentUser.put("avatar", null);
        currentUser.put("initials", "ML");
        currentUser.put("email", null);

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return currentUser;
        }

        String email = normalize(authentication.getName());
        currentUser.put("email", email);

        if (email == null) {
            return currentUser;
        }

        Optional<User> optionalUser = userRepository.findByEmailUserWithRole(email);
        if (optionalUser.isEmpty()) {
            currentUser.put("authenticated", true);
            currentUser.put("displayName", email);
            currentUser.put("initials", buildInitials(null, null, email));
            return currentUser;
        }

        User user = optionalUser.get();

        String civility = normalizeCivility(user.getCivilityUser());
        String firstName = normalizeName(user.getFirstNameUser());
        String lastName = normalizeName(user.getLastNameUser());
        String fullName = buildFullName(firstName, lastName);
        String displayName = fullName != null ? fullName : email;
        String role = translateRole(user.getRole() != null ? user.getRole().getLabelRole() : null);
        String avatar = normalize(user.getAvatarUser());
        String initials = buildInitials(firstName, lastName, email);

        currentUser.put("authenticated", true);
        currentUser.put("civility", civility);
        currentUser.put("firstName", firstName);
        currentUser.put("lastName", lastName);
        currentUser.put("fullName", fullName);
        currentUser.put("displayName", displayName);
        currentUser.put("role", role);
        currentUser.put("avatar", avatar);
        currentUser.put("initials", initials);
        currentUser.put("email", email);

        return currentUser;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String normalizeName(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }

        if (isAllUpperCase(normalizedValue)) {
            return toTitleCase(normalizedValue);
        }

        return normalizedValue;
    }

    private String normalizeCivility(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }

        String upperCasedValue = normalizedValue.toUpperCase(Locale.ROOT);

        return switch (upperCasedValue) {
            case "M", "M.", "MONSIEUR" -> "M.";
            case "MME", "MADAME" -> "Mme";
            default -> normalizedValue;
        };
    }

    private boolean isAllUpperCase(String value) {
        boolean hasLetter = false;

        for (char character : value.toCharArray()) {
            if (Character.isLetter(character)) {
                hasLetter = true;
                if (Character.isLowerCase(character)) {
                    return false;
                }
            }
        }

        return hasLetter;
    }

    private String toTitleCase(String value) {
        String lowerCasedValue = value.toLowerCase(Locale.FRENCH);
        StringBuilder result = new StringBuilder(lowerCasedValue.length());
        boolean capitalizeNext = true;

        for (char character : lowerCasedValue.toCharArray()) {
            if (capitalizeNext && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else {
                result.append(character);
            }

            if (character == ' ' || character == '-' || character == '\'') {
                capitalizeNext = true;
            }
        }

        return result.toString();
    }

    private String buildFullName(String firstName, String lastName) {
        String safeFirstName = firstName == null ? "" : firstName;
        String safeLastName = lastName == null ? "" : lastName;

        String fullName = (safeFirstName + " " + safeLastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    private String buildInitials(String firstName, String lastName, String email) {
        StringBuilder initials = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            initials.append(Character.toUpperCase(firstName.charAt(0)));
        }

        if (lastName != null && !lastName.isBlank()) {
            initials.append(Character.toUpperCase(lastName.charAt(0)));
        }

        if (initials.length() == 0 && email != null && !email.isBlank()) {
            initials.append(Character.toUpperCase(email.charAt(0)));
        }

        if (initials.length() == 1 && email != null && !email.isBlank()) {
            int atIndex = email.indexOf('@');
            String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
            if (localPart.length() > 1) {
                initials.append(Character.toUpperCase(localPart.charAt(1)));
            }
        }

        return initials.length() == 0 ? "ML" : initials.toString();
    }

    private String translateRole(String role) {
        String normalizedRole = normalize(role);
        if (normalizedRole == null) {
            return "Compte";
        }

        return switch (normalizedRole.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> "Administrateur";
            case "MEMBRE" -> "Membre";
            case "INVITE" -> "Invité";
            default -> normalizedRole;
        };
    }
}