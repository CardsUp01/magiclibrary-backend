package com.magiclibrary.security.auth;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
// Collections pour la gestion des rôles/authorities
import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------
// IMPORTS SPRING SECURITY
// -----------------------------------------------------------------------------
// UserDetailsService et classes Spring Security pour les rôles et authorities
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// Entité User et rôle applicatif
import com.magiclibrary.entities.User;
import com.magiclibrary.enums.ERole;
import com.magiclibrary.exceptions.custom.UnauthorizedException;
import com.magiclibrary.repositories.interfaces.UserRepository;

/**
 * =============================================================================
 * SERVICE : CustomUserDetailsService
 * =============================================================================
 * Service Spring Security chargé de récupérer un utilisateur par email
 * pour l’authentification.
 *
 * Responsabilités :
 *      - charger un utilisateur avec son rôle (JOIN FETCH)
 *      - refuser l’accès si le compte est inactif
 *      - construire les authorities Spring Security : ROLE_ADMIN / ROLE_MEMBRE / ROLE_INVITE
 *
 * Remarques :
 *      - aucune logique métier, uniquement adaptation User -> UserDetails
 *      - compatible avec JWT et filtres Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructeur avec injection du repository utilisateur.
     *
     * @param userRepository repository pour accéder aux utilisateurs et rôles
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // MÉTHODE PRINCIPALE : loadUserByUsername
    // -------------------------------------------------------------------------

    /**
     * Charge un utilisateur par son email pour Spring Security.
     *
     * Étapes :
     *      1. Récupération de l’utilisateur via UserRepository (email + rôle)
     *      2. Vérification du compte actif
     *      3. Vérification du rôle valide
     *      4. Construction des authorities Spring Security
     *      5. Retour d’un objet UserDetails pour le contexte de sécurité
     *
     * @param email email utilisateur
     * @return UserDetails pour Spring Security
     * @throws UsernameNotFoundException si l’utilisateur n’existe pas
     * @throws UnauthorizedException si compte inactif ou rôle invalide
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmailUserWithRole(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouvé pour l’email : " + email
                ));

        if (!Boolean.TRUE.equals(user.getActiveUser())) {
            throw new UnauthorizedException("Compte utilisateur désactivé.");
        }

        if (user.getRole() == null || user.getRole().getLabelRole() == null) {
            throw new UnauthorizedException("Rôle utilisateur invalide.");
        }

        // Source de vérité : enum aligné DB (ADMIN / MEMBRE / INVITE)
        ERole role = ERole.fromString(user.getRole().getLabelRole());

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        return new org.springframework.security.core.userdetails.User(
                user.getEmailUser(),
                user.getPasswordUser(),
                true,
                true,
                true,
                true,
                authorities
        );
    }
}