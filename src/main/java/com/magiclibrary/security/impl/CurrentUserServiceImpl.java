package com.magiclibrary.security.impl;

// -----------------------------------------------------------------------------
// IMPORTS SPRING SECURITY
// -----------------------------------------------------------------------------
// Gestion du contexte d’authentification et extraction du principal
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
// Entité User et repository associé
import com.magiclibrary.entities.User;
import com.magiclibrary.repositories.interfaces.UserRepository;
// Interface service
import com.magiclibrary.security.service.CurrentUserService;

/**
 * =============================================================================
 * SERVICE IMPL : CURRENT USER
 * =============================================================================
 * Implémentation centralisée pour récupérer l’utilisateur actuellement
 * authentifié dans Spring Security.
 *
 * Rôle :
 *      - accéder au SecurityContext de Spring Security
 *      - extraire l’objet Authentication
 *      - vérifier qu’il s’agit d’un UserDetails
 *      - convertir l’email username → entité User depuis la base
 *
 * Avantages :
 *      - évite la répétition de SecurityContextHolder dans tout le projet
 *      - améliore lisibilité et cohérence des contrôleurs
 *      - compatible avec les futures évolutions (statistiques, audit, notifications)
 *
 * Remarque :
 *      - aucune logique métier
 *      - uniquement lecture d’informations de sécurité
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    // -------------------------------------------------------------------------
    // DÉPENDANCE : UserRepository
    // -------------------------------------------------------------------------
    private final UserRepository userRepository;

    /**
     * Constructeur avec injection explicite de UserRepository.
     *
     * @param userRepository repository pour accéder aux utilisateurs
     */
    public CurrentUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // MÉTHODES PRINCIPALES : UTILISATEUR ACTUEL
    // -------------------------------------------------------------------------

    /**
     * Retourne l’entité User correspondant à l’utilisateur actuellement authentifié.
     *
     * Processus :
     *      1. Récupération de l’Authentication depuis SecurityContextHolder
     *      2. Vérification que le principal est un UserDetails
     *      3. Extraction de l’email depuis UserDetails
     *      4. Chargement de l’entité User depuis la base
     *
     * @return User ou null si aucun utilisateur connecté
     */
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmailUser(email).orElse(null);
    }

    /**
     * Indique si un utilisateur est actuellement authentifié.
     *
     * @return true si un utilisateur est connecté, false sinon
     */
    @Override
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null &&
                authentication.getPrincipal() instanceof UserDetails;
    }
}